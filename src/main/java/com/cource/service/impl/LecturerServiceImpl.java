package com.cource.service.impl;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.course.CourseOfferingRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.CourseService;
import com.cource.service.LecturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LecturerServiceImpl implements LecturerService {

    private final CourseLecturerRepository courseLecturerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final AcademicTermRepository academicTermRepository;
    private final CourseService courseService;

    @Override
    public List<CourseResponseDTO> getCoursesByLecturerId(long lecturerId) {
        return courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> {
                    CourseOffering offering = cl.getOffering();
                    Course course = offering.getCourse();
                    return CourseResponseDTO.builder()
                            .id(offering.getId())
                            .courseCode(course.getCourseCode())
                            .title(course.getTitle())
                            .description(course.getDescription())
                            .credits(course.getCredits())
                            .enrollmentCode(offering.getEnrollmentCode())
                            .active(offering.getTerm().isActive())
                            .capacity(offering.getCapacity())
                            .enrolled((int) enrollmentRepository.countByOfferingId(offering.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseOffering> getOfferingsByLecturerId(long lecturerId) {
        return courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(CourseLecturer::getOffering)
                .collect(Collectors.toList());
    }

    @Override
    public CourseOffering getOfferingById(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        return courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
    }

    @Override
    public CourseOffering createCourseOffering(long lecturerId, CourseOfferingRequestDTO dto) {
        var course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        var term = academicTermRepository.findById(dto.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Term not found"));

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(dto.getCapacity());
        offering.setActive(dto.getActive() != null ? dto.getActive() : true);
        offering.setEnrollmentCode(courseService.generateEnrollmentCode(course.getCourseCode()));

        offering = courseOfferingRepository.save(offering);

        CourseLecturer cl = new CourseLecturer();
        cl.setOffering(offering);
        User l = new User(); l.setId(lecturerId);
        cl.setLecturer(l);
        cl.setPrimary(true);
        courseLecturerRepository.save(cl);

        return offering;
    }

    @Override
    public CourseOffering updateCourseOffering(long lecturerId, long offeringId, CourseOfferingRequestDTO dto) {
        verifyOwnership(offeringId, lecturerId);
        var offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        if (dto.getCapacity() != null) offering.setCapacity(dto.getCapacity());
        if (dto.getActive() != null) offering.setActive(dto.getActive());
        return courseOfferingRepository.save(offering);
    }

    @Override
    public void deleteCourseOffering(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        courseOfferingRepository.deleteById(offeringId);
    }

    @Override
    public String regenerateOfferingCode(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        String newCode = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        offering.setEnrollmentCode(newCode);
        offering.setEnrollmentCodeExpiresAt(LocalDateTime.now().plusDays(7));
        courseOfferingRepository.save(offering);
        return newCode;
    }

    @Override
    public CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        regenerateOfferingCode(offeringId);
        return courseOfferingRepository.findById(offeringId).orElseThrow();
    }

    @Override
    public List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        return classScheduleRepository.findByOfferingIdAndLecturerId(offeringId, lecturerId);
    }

    @Override
    public List<Student> getEnrolledStudents(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        return enrollmentRepository.findByOfferingId(offeringId).stream()
                .filter(e -> e.getStatus() == null || "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
    }

    @Override
    public void recordAttendance(AttendanceRequestDTO dto, long studentId, String status) {
        if (dto.getLecturerId() == null) throw new IllegalArgumentException("Lecturer ID required");
        recordAttendance(dto, studentId, status, dto.getLecturerId());
    }

    @Override
    public void recordAttendance(AttendanceRequestDTO request, long studentId, String status, long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        verifyOwnership(schedule.getOffering().getId(), lecturerId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(
                        studentId, schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (attendanceRepository.existsByStudentIdAndScheduleId(studentId, schedule.getId(), enrollment.getId(), request.getAttendanceDate())) {
            throw new ConflictException("Attendance already recorded");
        }

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(status);

        User lecturer = new User();
        lecturer.setId(lecturerId);
        attendance.setRecordedBy(lecturer);

        if (request.getNotes() != null) attendance.setNotes(request.getNotes());
        attendanceRepository.save(attendance);
    }

    @Override
    public Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        if (lecturerId != null) {
            verifyOwnership(attendance.getSchedule().getOffering().getId(), lecturerId);
        }
        if (dto.getStatus() != null) attendance.setStatus(dto.getStatus());
        if (dto.getAttendanceDate() != null) attendance.setAttendanceDate(dto.getAttendanceDate());
        if (dto.getNotes() != null) attendance.setNotes(dto.getNotes());
        return attendanceRepository.save(attendance);
    }

    @Override
    public void deleteAttendance(long attendanceId, Long lecturerId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        if (lecturerId != null) verifyOwnership(attendance.getSchedule().getOffering().getId(), lecturerId);
        attendanceRepository.delete(attendance);
    }

    @Override
    public List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (lecturerId != null) verifyOwnership(schedule.getOffering().getId(), lecturerId);
        return attendanceRepository.findByScheduleId(scheduleId);
    }

    @Override
    public List<Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId) {
        List<Attendance> records = getAttendanceRecords(scheduleId, lecturerId);
        return records.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("studentName", a.getEnrollment().getStudent().getFullName());
            map.put("studentId", a.getEnrollment().getStudent().getStudentId());
            map.put("date", a.getAttendanceDate());
            map.put("status", a.getStatus());
            map.put("notes", a.getNotes());
            return map;
        }).collect(Collectors.toList());
    }


    @Override
    public LecturerDashboardDTO getDashboardStats(Long lecturerId) {
        long activeCourses = courseLecturerRepository.findByLecturerId(lecturerId).size();
        return LecturerDashboardDTO.builder().activeCourses(activeCourses).build();
    }

    @Override
    public Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Long> offeringIds = getOfferingsByLecturerId(lecturerId).stream()
                .map(CourseOffering::getId).collect(Collectors.toList());

        if (offeringIds.isEmpty()) return Collections.emptyMap();

        List<Object[]> results = attendanceRepository.countByOfferingIdsSince(offeringIds, startDate);
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    @Override
    public Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId) {
        return Collections.emptyMap();
    }

    private void verifyOwnership(long offeringId, long lecturerId) {
        if (!courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId)) {
            throw new SecurityException("Access denied");
        }
    }
}
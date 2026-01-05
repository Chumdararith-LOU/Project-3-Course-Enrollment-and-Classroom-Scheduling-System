package com.cource.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.exception.ConflictException;
import com.cource.repository.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseLecturer;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.service.LecturerService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
public class LecturerServiceImpl implements LecturerService {

    private final CourseLecturerRepository courseLecturerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public LecturerServiceImpl(CourseLecturerRepository courseLecturerRepository,
                               AttendanceRepository attendanceRepository,
                               ClassScheduleRepository classScheduleRepository,
                               EnrollmentRepository enrollmentRepository,
                               UserRepository userRepository,
                                CourseOfferingRepository courseOfferingRepository) {
        this.courseLecturerRepository = courseLecturerRepository;
        this.attendanceRepository = attendanceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    @Override
    public List<CourseResponseDTO> getCoursesByLecturerId(long lecturerId) {
        List<CourseLecturer> assignments = courseLecturerRepository.findByLecturerId(lecturerId);

        return assignments.stream()
                .map(cl -> {
                    CourseOffering offering = cl.getOffering();
                    Course course = offering.getCourse();

                    CourseResponseDTO dto = new CourseResponseDTO();
                    dto.setId(offering.getId()); // IMPORTANT: Use Offering ID, not Course ID
                    dto.setCourseCode(course.getCourseCode());
                    dto.setTitle(course.getTitle());
                    dto.setDescription(course.getDescription());
                    dto.setCredits(course.getCredits());
                    dto.setEnrollmentCode(offering.getEnrollmentCode()); // <--- THE MISSING PIECE
                    dto.setActive(offering.getTerm().isActive());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        return classScheduleRepository.findByOfferingIdAndLecturerId(offeringId, lecturerId);
    }

    @Override
    public List<User> getEnrolledStudents(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        List<Long> studentIds = enrollmentRepository.findByOfferingId(offeringId).stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .collect(Collectors.toList());

        return userRepository.findAllById(studentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public LecturerDashboardDTO getDashboardStats(Long lecturerId) {
        List<CourseLecturer> assignments = courseLecturerRepository.findByLecturerId(lecturerId);
        long activeCourses = assignments.size();

        long totalStudents = 0;
        long upcomingClasses = 0;
        long totalAttendanceRecords = 0;
        long totalPresent = 0;

        for (CourseLecturer assignment : assignments) {
            Long offeringId = assignment.getOffering().getId();
            totalStudents += enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
            List<ClassSchedule> schedules = classScheduleRepository.findByOfferingId(offeringId);
            upcomingClasses += schedules.size();

            for (ClassSchedule schedule : schedules) {
                List<Attendance> records = attendanceRepository.findByScheduleId(schedule.getId());
                totalAttendanceRecords += records.size();
                totalPresent += records.stream()
                        .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                        .count();
            }
        }

        double attendanceRate = (totalAttendanceRecords == 0) ? 0.0 : ((double) totalPresent / totalAttendanceRecords) * 100.0;

        return LecturerDashboardDTO.builder()
                .activeCourses(activeCourses)
                .totalStudents(totalStudents)
                .upcomingClasses(upcomingClasses)
                .averageAttendanceRate(attendanceRate)
                .build();
    }

    @Override
    public void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status, long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(attendanceRequestDTO.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        verifyOwnership(schedule.getOffering().getId(), lecturerId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(
                        studentId, schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(
                studentId,
                schedule.getId(),
                enrollment.getId(),
                attendanceRequestDTO.getAttendanceDate()
        );

        if (exists) {
            throw new ConflictException("Attendance already recorded for this student on this date.");
        }

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(attendanceRequestDTO.getAttendanceDate());
        attendance.setStatus(status);

        User lecturer = new User();
        lecturer.setId(lecturerId);
        attendance.setRecordedBy(lecturer);

        if (attendanceRequestDTO.getNotes() != null) {
            attendance.setNotes(attendanceRequestDTO.getNotes());
        }

        attendanceRepository.save(attendance);
    }

    @Override
    public List<Attendance> getAttendanceRecords(long scheduleId, long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        verifyOwnership(schedule.getOffering().getId(), lecturerId);
        return attendanceRepository.findByScheduleId(scheduleId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public String regenerateOfferingCode(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

        String newCode = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        offering.setEnrollmentCode(newCode);
        offering.setEnrollmentCodeExpiresAt(java.time.LocalDateTime.now().plusDays(7));

        courseOfferingRepository.save(offering);
        return newCode;
    }

    private void verifyOwnership(long offeringId, long lecturerId) {
        boolean isOwner = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId);
        if (!isOwner) {
            throw new SecurityException("Lecturer does not have access to this course offering.");
        }
    }
}
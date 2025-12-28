package com.cource.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseLecturer;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.CourseLecturerRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.LecturerService;

@Service
@Transactional
public class LecturerServiceImpl implements LecturerService {

    private final CourseLecturerRepository courseLecturerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LecturerServiceImpl(CourseLecturerRepository courseLecturerRepository,
                               AttendanceRepository attendanceRepository,
                               ClassScheduleRepository classScheduleRepository,
                               EnrollmentRepository enrollmentRepository) {
        this.courseLecturerRepository = courseLecturerRepository;
        this.attendanceRepository = attendanceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public List<Course> getCoursesByLecturerId(long lecturerId) {
        return courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> cl.getOffering().getCourse())
                .distinct()
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
        return enrollmentRepository.findByOfferingId(offeringId).stream()
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LecturerDashboardDTO getDashboardStats(Long lecturerId) {
        // 1. Count Active Courses
        List<CourseLecturer> assignments = courseLecturerRepository.findByLecturerId(lecturerId);
        long activeCourses = assignments.size();

        long totalStudents = 0;
        long upcomingClasses = 0;
        long totalAttendanceRecords = 0;
        long totalPresent = 0;

        for (CourseLecturer assignment : assignments) {
            Long offeringId = assignment.getOffering().getId();

            // 2. Count Students
            totalStudents += enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");

            // 3. Count Upcoming Classes
            List<ClassSchedule> schedules = classScheduleRepository.findByOfferingId(offeringId);
            upcomingClasses += schedules.size();

            // 4. Calculate Attendance Rate
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
                .averageAttendanceRate(attendanceRate) // FIX: Setting the new field
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

    private void verifyOwnership(long offeringId, long lecturerId) {
        boolean isOwner = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId);
        if (!isOwner) {
            throw new SecurityException("Lecturer does not have access to this course offering.");
        }
    }
}
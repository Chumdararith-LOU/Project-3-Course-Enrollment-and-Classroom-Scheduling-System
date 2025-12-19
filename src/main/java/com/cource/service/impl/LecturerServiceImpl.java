package com.cource.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.CourseLecturerRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.LecturerService;

import jakarta.transaction.Transactional;

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
    public void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status) {
        ClassSchedule schedule = classScheduleRepository.findById(attendanceRequestDTO.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        verifyOwnership(schedule.getOffering().getId(), attendanceRequestDTO.getLecturerId());

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(
                studentId, schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(attendanceRequestDTO.getAttendanceDate());
        attendance.setStatus(status);
        
        User lecturer = new User();
        lecturer.setId(attendanceRequestDTO.getLecturerId());
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

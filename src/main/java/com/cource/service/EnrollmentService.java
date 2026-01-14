package com.cource.service;

import com.cource.dto.enrollment.EnrollmentResult;
import com.cource.entity.Attendance;
import com.cource.entity.Enrollment;

import java.util.List;

public interface EnrollmentService {
    long getEnrolledCourseCount(Long studentId);

    EnrollmentResult enrollStudent(Long studentId, Long offeringId);

    /**
     * Enroll a student in a specific offering after validating the offering's
     * enrollment code.
     * Keeps controller thin by centralizing code validation here.
     */
    EnrollmentResult enrollStudentWithOfferingCode(Long studentId, Long offeringId, String enrollmentCode);

    EnrollmentResult dropCourse(Long studentId, Long offeringId);

    EnrollmentResult removeFromWaitlist(Long studentId, Long offeringId);

    EnrollmentResult enrollByCode(Long studentId, String enrollmentCode);

    void processWaitlist(Long offeringId);

    List<Enrollment> getStudentGrades(Long studentId);

    List<Attendance> getStudentAttendance(Long studentId);

    List<Enrollment> getEnrollmentsByOffering(Long offeringId);

    void updateGrade(Long enrollmentId, String grade);
}

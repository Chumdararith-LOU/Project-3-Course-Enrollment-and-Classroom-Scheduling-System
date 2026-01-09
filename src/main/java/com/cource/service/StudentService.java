package com.cource.service;

import com.cource.entity.*;
import java.util.List;

public interface StudentService {
    // Enrollment management
    List<Enrollment> getMyEnrollments(Long studentId);

    List<Enrollment> getMyGrades(Long studentId);

    // Schedule
    List<ClassSchedule> getMySchedule(Long studentId);

    // Attendance
    List<Attendance> getMyAttendance(Long studentId, Long offeringId);

    double getAttendancePercentage(Long studentId, Long offeringId);

    // Waitlist
    List<Waitlist> getMyWaitlistEntries(Long studentId);

    // Terms
    List<AcademicTerm> getActiveTerms();

    // All terms (including past/future) for dropdowns
    List<AcademicTerm> getAllTerms();

    // Offerings available to a student (not already enrolled)
    java.util.List<com.cource.entity.CourseOffering> getAvailableOfferings(Long studentId, Long termId, String keyword);

    // Enroll student into offering (with enrollment code)
    com.cource.entity.Enrollment enrollInOffering(Long studentId, Long offeringId, String enrollmentCode);

    // Student initiated drop
    void dropEnrollment(Long studentId, Long enrollmentId);

    // Student submits attendance request (will be recorded with status 'REQUESTED'
    // and
    // processed by lecturer/admin)
    com.cource.entity.Attendance submitAttendanceRequest(Long studentId, Long scheduleId,
            java.time.LocalDate attendanceDate, String notes);

    // Restore a previously dropped enrollment (student requests to re-enroll)
    com.cource.entity.Enrollment restoreEnrollment(Long studentId, Long enrollmentId);

    // Delete an enrollment record (only allowed for the student and typically when
    // DROPPED)
    void deleteEnrollment(Long studentId, Long enrollmentId);

    // Academic summaries
    double calculateGPA(Long studentId);

    int getCreditsEarned(Long studentId);

    int getCoursesCompleted(Long studentId);
}

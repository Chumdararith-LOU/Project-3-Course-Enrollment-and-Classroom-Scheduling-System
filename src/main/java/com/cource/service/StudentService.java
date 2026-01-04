package com.cource.service;

import com.cource.entity.AcademicTerm;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.Waitlist;
import java.util.List;

public interface StudentService {

    List<Enrollment> getMyEnrollments(Long studentId);
    List<AcademicTerm> getActiveTerms();
    List<AcademicTerm> getAllTerms();

    double calculateGPA(Long studentId);

    int getCreditsEarned(Long studentId);
    int getCoursesCompleted(Long studentId);

    List<ClassSchedule> getMySchedule(Long studentId);
    List<Enrollment> getMyGrades(Long studentId);
    List<Attendance> getMyAttendance(Long studentId, Long offeringId);

    double getAttendancePercentage(Long studentId, Long offeringId);

    List<Waitlist> getMyWaitlistEntries(Long studentId);
    List<CourseOffering> getAvailableOfferings(Long studentId, Long termId, String keyword);

    Enrollment enrollInOffering(Long studentId, Long offeringId, String enrollmentCode);
}
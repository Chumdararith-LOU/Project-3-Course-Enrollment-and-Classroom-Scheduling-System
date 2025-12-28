package com.cource.service;

import com.cource.dto.EnrollmentResult;

public interface EnrollmentService {
    long getEnrolledCourseCount(Long studentId);
    EnrollmentResult enrollStudent(Long studentId, Long offeringId);
    EnrollmentResult dropCourse(Long studentId, Long offeringId);
    void processWaitlist(Long offeringId);
}

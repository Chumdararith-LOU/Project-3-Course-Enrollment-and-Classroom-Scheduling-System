package com.cource.service;

public interface EnrollmentService {
    long getEnrolledCourseCount(Long studentId);
    void enrollStudent(Long studentId, Long offeringId);
}

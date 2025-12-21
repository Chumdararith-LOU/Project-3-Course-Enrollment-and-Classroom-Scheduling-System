package com.cource.service.impl;

import com.cource.repository.EnrollmentRepository;
import com.cource.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public long getEnrolledCourseCount(Long studentId) {
        // We count how many active enrollments the student has
        return enrollmentRepository.countByStudentIdAndStatus(studentId, "ENROLLED");
    }
}

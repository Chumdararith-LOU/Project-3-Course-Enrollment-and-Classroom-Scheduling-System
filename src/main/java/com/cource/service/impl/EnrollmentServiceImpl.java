package com.cource.service.impl;

import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.repository.UserRepository;
import com.cource.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final UserRepository userRepository;

    @Override
    public long getEnrolledCourseCount(Long studentId) {
        return enrollmentRepository.countByStudentIdAndStatus(studentId, "ENROLLED");
    }

    @Override
    @Transactional
    public void enrollStudent(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        if (enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offeringId)) {
            throw new ConflictException("You are already enrolled in this course.");
        }

        int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
        if (currentEnrolled >= offering.getCapacity()) {
            throw new ConflictException("Course is full. Please join the waitlist.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setOffering(offering);
        enrollment.setStatus("ENROLLED");

        enrollmentRepository.save(enrollment);
    }
}

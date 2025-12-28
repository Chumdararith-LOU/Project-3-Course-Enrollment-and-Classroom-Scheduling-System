package com.cource.service.impl;

import com.cource.entity.ClassSchedule;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.repository.UserRepository;
import com.cource.service.EnrollmentService;
import com.cource.util.TimeConflictChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final UserRepository userRepository;

    private final ClassScheduleRepository classScheduleRepository;
    private final TimeConflictChecker timeConflictChecker;

    @Override
    @PreAuthorize("hasRole('STUDENT')")
    public long getEnrolledCourseCount(Long studentId) {
        return enrollmentRepository.countByStudentIdAndStatus(studentId, "ENROLLED");
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('STUDENT')")
    public void enrollStudent(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        // Check if already enrolled
        if (enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offeringId)) {
            throw new ConflictException("You are already enrolled in this course.");
        }

        // Check Capacity of the Course
        int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
        if (currentEnrolled >= offering.getCapacity()) {
            throw new ConflictException("Course is full. Please join the waitlist.");
        }

        // Check Time Conflict
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Fetch schedule for the new course
        List<ClassSchedule> newCourseSchedules = classScheduleRepository.findByOfferingId(offeringId);

        // Fetch schedules for courses the student is ALREADY enrolled in
        List<Enrollment> existingEnrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, "ENROLLED");
        List<Long> enrolledOfferingIds = existingEnrollments.stream()
                .map(e -> e.getOffering().getId())
                .collect(Collectors.toList());

        if (!enrolledOfferingIds.isEmpty()) {
            List<ClassSchedule> studentCurrentSchedules = classScheduleRepository.findByOfferingIdIn(enrolledOfferingIds);

            for (ClassSchedule newSched : newCourseSchedules) {
                if (timeConflictChecker.hasConflict(newSched, studentCurrentSchedules)) {
                    throw new ConflictException("Time conflict detected with course: " + newSched.getCourseOffering().getCourse().getCourseCode());
                }
            }
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setOffering(offering);
        enrollment.setStatus("ENROLLED");

        enrollmentRepository.save(enrollment);
    }
}

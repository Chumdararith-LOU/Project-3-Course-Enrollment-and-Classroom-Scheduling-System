package com.cource.service.impl;

import com.cource.dto.EnrollmentResult;
import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.EnrollmentService;
import com.cource.util.TimeConflictChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final WaitlistRepository waitlistRepository;

    private final ClassScheduleRepository classScheduleRepository;
    private final TimeConflictChecker timeConflictChecker;

    @Override
    public long getEnrolledCourseCount(Long studentId) {
        return enrollmentRepository.countByStudentIdAndStatus(studentId, "ENROLLED");
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('STUDENT')")
    public EnrollmentResult enrollStudent(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        // Check if already enrolled or waitlisted
        if (enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offeringId)) {
            throw new ConflictException("You are already enrolled in this course.");
        }

        // Check Capacity of the Course
        int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
        if (currentEnrolled >= offering.getCapacity()) {
            throw new ConflictException("Course is full. Please join the waitlist.");
        }

        Student student = studentRepository.findById(studentId)
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

        int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");

        if (currentEnrolled < offering.getCapacity()) {
            // Course has space - enroll directly
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setOffering(offering);
            enrollment.setStatus("ENROLLED");

            enrollmentRepository.save(enrollment);
            return new EnrollmentResult("ENROLLED", "Successfully enrolled in course");
        } else {
            return addToWaitlist(studentId, offeringId);
        }
    }

    @Transactional
    public EnrollmentResult addToWaitlist(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        int maxPosition = waitlistRepository.findMaxPositionByOfferingId(offeringId);
        int nextPosition = (maxPosition > 0) ? maxPosition + 1 : 1;

        Waitlist waitlist = new Waitlist();
        waitlist.setStudent(student);
        waitlist.setOffering(offering);
        waitlist.setPosition(nextPosition);
        waitlist.setStatus("PENDING");

        waitlistRepository.save(waitlist);
        return new EnrollmentResult("WAITLISTED", "Course is full. Added to waitlist at position " + nextPosition);
    }

    @Override
    @Transactional
    public EnrollmentResult dropCourse(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getStatus().equals("COMPLETED") || enrollment.getStatus().equals("FAILED")) {
            throw new ConflictException("Cannot drop a course that is already " + enrollment.getStatus());
        }

        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);

        processWaitlist(offeringId);

        return new EnrollmentResult("DROPPED", "Successfully dropped course");
    }

    @Override
    @Transactional
    public void processWaitlist(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");

        if (currentEnrolled < offering.getCapacity()) {
            Optional<Waitlist> nextWaitlist = waitlistRepository.findFirstByOfferingIdOrderByPosition(offeringId);

            if (nextWaitlist.isPresent()) {
                Waitlist waitlist = nextWaitlist.get();

                // Enroll the waitlisted student
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(waitlist.getStudent());
                enrollment.setOffering(offering);
                enrollment.setStatus("ENROLLED");

                enrollmentRepository.save(enrollment);

                // Remove from waitlist
                waitlist.setStatus("ENROLLED");
                waitlistRepository.save(waitlist);

                // Notification for promoted student
                System.out.println("NOTIFICATION: Waitlisted student promoted: " + waitlist.getStudent().getEmail() +
                                 " - Now enrolled in " + offering.getCourse().getTitle());
            }
        }
    }
}

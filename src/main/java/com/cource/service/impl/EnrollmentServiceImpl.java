package com.cource.service.impl;

import com.cource.dto.EnrollmentResult;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.Student;
import com.cource.entity.Waitlist;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.repository.StudentRepository;
import com.cource.repository.WaitlistRepository;
import com.cource.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentRepository studentRepository;
    private final WaitlistRepository waitlistRepository;

    @Override
    public long getEnrolledCourseCount(Long studentId) {
        return enrollmentRepository.countByStudentIdAndStatus(studentId, "ENROLLED");
    }

    @Override
    @Transactional
    public EnrollmentResult enrollStudent(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        // Check if already enrolled or waitlisted
        if (enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offeringId)) {
            throw new ConflictException("You are already enrolled or waitlisted for this course.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

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
            // Course is full - add to waitlist automatically
            return addToWaitlist(studentId, offeringId);
        }
    }

    @Transactional
    public EnrollmentResult addToWaitlist(Long studentId, Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Get next waitlist position
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

        // Find the enrollment
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        // Validation: Prevent dropping if course is completed or failed
        if (enrollment.getStatus().equals("COMPLETED") || enrollment.getStatus().equals("FAILED")) {
            throw new ConflictException("Cannot drop a course that is already " + enrollment.getStatus());
        }

        // Update enrollment status to DROPPED
        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);

        // Auto-promote waitlist: Process waitlist for this offering
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
            // There's space, get first person from waitlist
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

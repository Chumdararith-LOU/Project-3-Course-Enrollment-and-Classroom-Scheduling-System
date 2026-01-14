package com.cource.service.impl;

import com.cource.dto.enrollment.EnrollmentResult;
import com.cource.entity.Attendance;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.entity.Waitlist;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.repository.UserRepository;
import com.cource.repository.WaitlistRepository;
import com.cource.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final UserRepository userRepository;
    private final WaitlistRepository waitlistRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public long getEnrolledCourseCount(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .filter(e -> e.getStatus() != null &&
                        e.getStatus().equalsIgnoreCase("ENROLLED"))
                .count();
    }

    @Override
    public EnrollmentResult enrollStudent(Long studentId, Long offeringId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Course offering not found"));

        var existing = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        if (existing.isPresent()) {
            Enrollment e = existing.get();
            if (e.getStatus() != null && e.getStatus().equalsIgnoreCase("ENROLLED")) {
                return new EnrollmentResult("ENROLLED", "Already enrolled in this course");
            }
            e.setStatus("ENROLLED");
            e.setEnrolledAt(LocalDateTime.now());
            enrollmentRepository.save(e);
            return new EnrollmentResult("ENROLLED", "Enrolled successfully");
        }

        long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
        if (enrolledCount < offering.getCapacity()) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setOffering(offering);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setStatus("ENROLLED");
            enrollmentRepository.save(enrollment);
            return new EnrollmentResult("ENROLLED", "Enrolled successfully");
        }

        if (waitlistRepository.existsByStudentIdAndOfferingId(studentId, offeringId)) {
            return new EnrollmentResult("WAITLISTED", "You are already on the waitlist");
        }

        Integer maxPosition = waitlistRepository.findMaxPositionByOfferingId(offeringId);
        int nextPosition = (maxPosition == null ? 1 : maxPosition + 1);

        Waitlist wait = new Waitlist();
        wait.setStudent(student);
        wait.setOffering(offering);
        wait.setPosition(nextPosition);
        wait.setStatus("PENDING");
        waitlistRepository.save(wait);

        return new EnrollmentResult("WAITLISTED", "Course is full; you have been added to the waitlist");
    }

    @Override
    public EnrollmentResult enrollStudentWithOfferingCode(Long studentId, Long offeringId, String enrollmentCode) {
        if (enrollmentCode == null || enrollmentCode.isBlank()) {
            throw new IllegalArgumentException("Enrollment code is required");
        }

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Offering not found"));

        if (offering.getEnrollmentCode() == null || !offering.getEnrollmentCode().equals(enrollmentCode)) {
            throw new IllegalArgumentException("Invalid enrollment code");
        }

        return enrollStudent(studentId, offeringId);
    }

    @Override
    public EnrollmentResult dropCourse(Long studentId, Long offeringId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);

        processWaitlist(offeringId);
        return new EnrollmentResult("DROPPED", "Course dropped successfully");
    }

    @Override
    public EnrollmentResult removeFromWaitlist(Long studentId, Long offeringId) {
        var entry = waitlistRepository.findByStudentIdAndOfferingIdAndStatus(studentId, offeringId, "PENDING");
        if (entry.isEmpty()) {
            return new EnrollmentResult("NOT_WAITLISTED", "You are not currently on the waitlist for this course");
        }
        waitlistRepository.delete(entry.get());
        return new EnrollmentResult("REMOVED", "Removed from waitlist successfully");
    }

    @Override
    public EnrollmentResult enrollByCode(Long studentId, String enrollmentCode) {
        CourseOffering offering = courseOfferingRepository.findByEnrollmentCode(enrollmentCode)
                .orElseThrow(() -> new RuntimeException("Invalid enrollment code"));

        return enrollStudent(studentId, offering.getId());
    }

    @Override
    public void processWaitlist(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Course offering not found"));

        while (enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED") < offering.getCapacity()) {
            var next = waitlistRepository.findFirstByOfferingIdOrderByPosition(offeringId);
            if (next.isEmpty()) {
                return;
            }

            Waitlist w = next.get();
            if (w.getStudent() == null) {
                w.setStatus("EXPIRED");
                waitlistRepository.save(w);
                continue;
            }

            Long studentId = w.getStudent().getId();

            if (enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId).isPresent()) {
                w.setStatus("EXPIRED");
                waitlistRepository.save(w);
                continue;
            }

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(w.getStudent());
            enrollment.setOffering(offering);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setStatus("ENROLLED");
            enrollmentRepository.save(enrollment);

            w.setStatus("NOTIFIED");
            w.setNotifiedAt(LocalDateTime.now());
            waitlistRepository.save(w);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getStudentGrades(Long studentId) {
        return enrollmentRepository.findByStudentIdAndGradeIsNotNull(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getStudentAttendance(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsByOffering(Long offeringId) {
        return enrollmentRepository.findByOfferingId(offeringId);
    }

    @Override
    public void updateGrade(Long enrollmentId, String grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setGrade(grade);
        enrollmentRepository.save(enrollment);
    }
}

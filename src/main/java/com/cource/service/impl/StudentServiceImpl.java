package com.cource.service.impl;

import com.cource.dto.enrollment.EnrollmentResult;
import com.cource.entity.*;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.EnrollmentService;
import com.cource.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final AcademicTermRepository academicTermRepository;
    private final AttendanceRepository attendanceRepository;
    private final WaitlistRepository waitlistRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentService enrollmentService;

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getMyEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicTerm> getActiveTerms() {
        return academicTermRepository.findAll().stream()
                .filter(AcademicTerm::isActive)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicTerm> getAllTerms() {
        return academicTermRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateGPA(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        double totalPoints = 0;
        int totalCredits = 0;

        for (Enrollment e : enrollments) {
            if ("COMPLETED".equalsIgnoreCase(e.getStatus()) && e.getGrade() != null) {
                double points = convertGradeToPoints(e.getGrade());
                int credits = e.getOffering().getCourse().getCredits();
                totalPoints += (points * credits);
                totalCredits += credits;
            }
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    @Override
    @Transactional(readOnly = true)
    public int getCreditsEarned(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus()))
                .mapToInt(e -> e.getOffering().getCourse().getCredits())
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getCoursesCompleted(Long studentId) {
        return (int) enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus()))
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSchedule> getMySchedule(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        List<Long> offeringIds = enrollments.stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(e -> e.getOffering().getId())
                .collect(Collectors.toList());

        if (offeringIds.isEmpty()) {
            return Collections.emptyList();
        }

        return classScheduleRepository.findByOfferingIdIn(offeringIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getMyGrades(Long studentId) {
        // Typically returns all enrollments that have a grade or status is terminal
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> e.getGrade() != null || "COMPLETED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getMyAttendance(Long studentId, Long offeringId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElse(null);

        if (enrollment == null) return Collections.emptyList();

        return attendanceRepository.findAll().stream()
                .filter(a -> a.getEnrollment().getId().equals(enrollment.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public double getAttendancePercentage(Long studentId, Long offeringId) {
        List<Attendance> records = getMyAttendance(studentId, offeringId);
        if (records.isEmpty()) return 100.0; // Default if no records yet

        long presentOrLate = records.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
                .count();

        return ((double) presentOrLate / records.size()) * 100.0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Waitlist> getMyWaitlistEntries(Long studentId) {
        return waitlistRepository.findAll().stream()
                .filter(w -> w.getStudent().getId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseOffering> getAvailableOfferings(Long studentId, Long termId, String keyword) {
        List<CourseOffering> offerings;
        if (termId != null) {
            offerings = courseOfferingRepository.findAll().stream()
                    .filter(o -> o.getTerm().getId().equals(termId))
                    .collect(Collectors.toList());
        } else {
            offerings = courseOfferingRepository.findAll();
        }

        return offerings.stream()
                .filter(CourseOffering::isActive)
                .filter(o -> keyword == null || keyword.isEmpty()
                        || o.getCourse().getTitle().toLowerCase().contains(keyword.toLowerCase())
                        || o.getCourse().getCourseCode().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Enrollment enrollInOffering(Long studentId, Long offeringId, String enrollmentCode) {
        EnrollmentResult result = enrollmentService.enrollByCode(studentId, enrollmentCode);

        if ("ENROLLED".equals(result.getStatus())) {
            return enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enrollment successful but record not found"));
        } else if ("WAITLISTED".equals(result.getStatus())) {
            throw new IllegalStateException("Course is full. You have been added to the waitlist.");
        } else {
            throw new IllegalStateException(result.getMessage());
        }
    }

    private double convertGradeToPoints(String grade) {
        if (grade == null) return 0.0;
        switch (grade.toUpperCase()) {
            case "A": return 4.0;
            case "B": return 3.0;
            case "C": return 2.0;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return 0.0;
        }
    }
}

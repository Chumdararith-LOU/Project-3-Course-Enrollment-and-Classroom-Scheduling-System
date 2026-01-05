package com.cource.service.impl;

import com.cource.entity.*;
import com.cource.repository.*;
import com.cource.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('STUDENT')")
public class StudentServiceImpl implements StudentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final AttendanceRepository attendanceRepository;
    private final WaitlistRepository waitlistRepository;
    private final AcademicTermRepository academicTermRepository;
    private final com.cource.repository.CourseOfferingRepository courseOfferingRepository;

    @Override
    public List<Enrollment> getMyEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Override
    public List<Enrollment> getMyGrades(Long studentId) {
        return enrollmentRepository.findByStudentIdAndGradeIsNotNull(studentId);
    }

    @Override
    public List<ClassSchedule> getMySchedule(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream()
                .flatMap(e -> classScheduleRepository.findByOfferingId(e.getOffering().getId()).stream())
                .distinct()
                .toList();
    }

    @Override
    public java.util.List<com.cource.entity.CourseOffering> getAvailableOfferings(Long studentId, Long termId,
            String keyword) {
        java.util.List<com.cource.entity.CourseOffering> offerings;
        if (keyword != null && !keyword.isBlank() && termId != null) {
            offerings = courseOfferingRepository.findByCourseTitleContainingIgnoreCaseAndTermId(keyword, termId);
        } else if (keyword != null && !keyword.isBlank()) {
            offerings = courseOfferingRepository.findByCourseTitleContainingIgnoreCase(keyword);
        } else if (termId != null) {
            offerings = courseOfferingRepository.findActiveByTermId(termId);
        } else {
            // default: all active offerings
            offerings = courseOfferingRepository.findByActive(true);
        }

        // filter out offerings where student already enrolled
        java.util.List<com.cource.entity.CourseOffering> out = new java.util.ArrayList<>();
        for (var off : offerings) {
            var exists = enrollmentRepository.findByStudentIdAndOfferingId(studentId, off.getId()).isPresent();
            if (!exists) {
                out.add(off);
            }
        }
        return out;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.cource.entity.Enrollment enrollInOffering(Long studentId, Long offeringId, String enrollmentCode) {
        var off = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new com.cource.exception.ResourceNotFoundException("Offering not found"));
        if (enrollmentCode == null || !off.getEnrollmentCode().equals(enrollmentCode)) {
            throw new IllegalArgumentException("Invalid enrollment code");
        }
        var already = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        if (already.isPresent()) {
            throw new IllegalArgumentException("Student already enrolled");
        }
        Long enrolledCountNullable = courseOfferingRepository.countEnrolledStudents(offeringId);
        long enrolledCount = enrolledCountNullable == null ? 0L : enrolledCountNullable;
        if (enrolledCount >= off.getCapacity()) {
            throw new IllegalStateException("Offering is full");
        }
        com.cource.entity.User student = new com.cource.entity.User();
        student.setId(studentId);
        com.cource.entity.Enrollment e = new com.cource.entity.Enrollment();
        e.setStudent(student);
        e.setOffering(off);
        e.setStatus("ENROLLED");
        return enrollmentRepository.save(e);
    }

    @Override
    public List<Attendance> getMyAttendance(Long studentId, Long offeringId) {
        return attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);
    }

    @Override
    public double getAttendancePercentage(Long studentId, Long offeringId) {
        List<Attendance> attendance = attendanceRepository.findByStudentIdAndOfferingId(studentId,
                offeringId);
        if (attendance.isEmpty()) {
            return 0.0;
        }
        long presentCount = attendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        return (double) presentCount / attendance.size() * 100;
    }

    @Override
    public List<Waitlist> getMyWaitlistEntries(Long studentId) {
        return waitlistRepository.findByStudentIdOrderByPositionAsc(studentId);
    }

    @Override
    public List<AcademicTerm> getActiveTerms() {
        LocalDate now = LocalDate.now();
        return academicTermRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(now, now);
    }

    @Override
    public List<AcademicTerm> getAllTerms() {
        return academicTermRepository.findAll();
    }

    @Override
    public double calculateGPA(Long studentId) {
        List<Enrollment> grades = getMyGrades(studentId);
        if (grades == null || grades.isEmpty())
            return 0.0;

        double totalPoints = 0.0;
        int totalCredits = 0;
        for (var e : grades) {
            String g = e.getGrade();
            if (g == null)
                continue;
            int credits = 0;
            try {
                credits = e.getOffering().getCourse().getCredits();
            } catch (Exception ex) {
                credits = 0;
            }
            double pts = switch (g.toUpperCase()) {
                case "A+", "A" -> 4.0;
                case "A-" -> 3.7;
                case "B+" -> 3.3;
                case "B" -> 3.0;
                case "B-" -> 2.7;
                case "C+" -> 2.3;
                case "C" -> 2.0;
                case "C-" -> 1.7;
                case "D+" -> 1.3;
                case "D" -> 1.0;
                case "F", "W", "I" -> 0.0;
                default -> 0.0;
            };
            totalPoints += pts * credits;
            totalCredits += credits;
        }
        if (totalCredits == 0)
            return 0.0;
        return Math.round((totalPoints / totalCredits) * 100.0) / 100.0; // round to 2 decimals
    }

    @Override
    public int getCreditsEarned(Long studentId) {
        List<Enrollment> grades = getMyGrades(studentId);
        if (grades == null || grades.isEmpty())
            return 0;
        int sum = 0;
        for (var e : grades) {
            String g = e.getGrade();
            if (g == null)
                continue;
            if (g.equalsIgnoreCase("F") || g.equalsIgnoreCase("W") || g.equalsIgnoreCase("I"))
                continue;
            try {
                sum += e.getOffering().getCourse().getCredits();
            } catch (Exception ex) {
            }
        }
        return sum;
    }

    @Override
    public int getCoursesCompleted(Long studentId) {
        List<Enrollment> grades = getMyGrades(studentId);
        if (grades == null || grades.isEmpty())
            return 0;
        int count = 0;
        for (var e : grades) {
            String g = e.getGrade();
            if (g == null)
                continue;
            if (g.equalsIgnoreCase("F") || g.equalsIgnoreCase("W") || g.equalsIgnoreCase("I"))
                continue;
            count++;
        }
        return count;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void dropEnrollment(Long studentId, Long enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new com.cource.exception.ResourceNotFoundException("Enrollment not found"));
        if (enrollment.getStudent() == null || !enrollment.getStudent().getId().equals(studentId)) {
            throw new SecurityException("Student does not have permission to drop this enrollment");
        }
        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.cource.entity.Enrollment restoreEnrollment(Long studentId, Long enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new com.cource.exception.ResourceNotFoundException("Enrollment not found"));
        if (enrollment.getStudent() == null || !enrollment.getStudent().getId().equals(studentId)) {
            throw new SecurityException("Student does not have permission to restore this enrollment");
        }
        if (!"DROPPED".equalsIgnoreCase(enrollment.getStatus())) {
            throw new IllegalArgumentException("Only dropped enrollments can be restored");
        }
        Long offeringId = enrollment.getOffering() == null ? null : enrollment.getOffering().getId();
        if (offeringId == null) {
            throw new com.cource.exception.ResourceNotFoundException("Associated offering not found");
        }
        Long enrolledCountNullable = courseOfferingRepository.countEnrolledStudents(offeringId);
        long enrolledCount = enrolledCountNullable == null ? 0L : enrolledCountNullable;
        var off = enrollment.getOffering();
        if (off != null && enrolledCount >= off.getCapacity()) {
            throw new IllegalStateException("Offering is full; cannot re-enroll");
        }
        enrollment.setStatus("ENROLLED");
        return enrollmentRepository.save(enrollment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteEnrollment(Long studentId, Long enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new com.cource.exception.ResourceNotFoundException("Enrollment not found"));
        if (enrollment.getStudent() == null || !enrollment.getStudent().getId().equals(studentId)) {
            throw new SecurityException("Student does not have permission to delete this enrollment");
        }
        if (!"DROPPED".equalsIgnoreCase(enrollment.getStatus())) {
            throw new IllegalArgumentException("Only dropped enrollments can be deleted by student");
        }
        enrollmentRepository.delete(enrollment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.cource.entity.Attendance submitAttendanceRequest(Long studentId, Long scheduleId,
            java.time.LocalDate attendanceDate, String notes) {
        var schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new com.cource.exception.ResourceNotFoundException("Schedule not found"));

        var enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, schedule.getOffering().getId())
                .orElseThrow(() -> new com.cource.exception.ResourceNotFoundException(
                        "Enrollment not found for student/offering"));

        boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(studentId, scheduleId, enrollment.getId(),
                attendanceDate);
        if (exists) {
            throw new IllegalArgumentException("Attendance already submitted for this date");
        }

        com.cource.entity.Attendance a = new com.cource.entity.Attendance();
        a.setEnrollment(enrollment);
        a.setSchedule(schedule);
        a.setAttendanceDate(attendanceDate);
        a.setStatus("REQUESTED");
        com.cource.entity.User student = new com.cource.entity.User();
        student.setId(studentId);
        a.setRecordedBy(student);
        if (notes != null) {
            a.setNotes(notes);
        }
        try {
            return attendanceRepository.save(a);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            // likely unique constraint violation due to concurrent request
            throw new IllegalArgumentException("Attendance already submitted for this date");
        }
    }
}

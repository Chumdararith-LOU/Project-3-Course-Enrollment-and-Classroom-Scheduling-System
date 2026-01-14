package com.cource.service.impl;

import com.cource.entity.AcademicTerm;
import com.cource.entity.Enrollment;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.StudentReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentReadServiceImpl implements StudentReadService {

    private final EnrollmentRepository enrollmentRepository;
    private final AcademicTermRepository academicTermRepository;

    @Override
    public List<Enrollment> getEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Override
    public List<Enrollment> getGrades(Long studentId) {
        return enrollmentRepository.findByStudentIdAndGradeIsNotNull(studentId);
    }

    @Override
    public List<AcademicTerm> getActiveTerms() {
        LocalDate now = LocalDate.now();
        return academicTermRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(now, now);
    }

    @Override
    public double calculateGPA(Long studentId) {
        List<Enrollment> grades = getGrades(studentId);
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        int totalCredits = 0;

        for (Enrollment e : grades) {
            String g = e.getGrade();
            if (g == null) {
                continue;
            }

            int credits;
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

        if (totalCredits == 0) {
            return 0.0;
        }

        return Math.round((totalPoints / totalCredits) * 100.0) / 100.0;
    }

    @Override
    public int getCreditsEarned(Long studentId) {
        List<Enrollment> grades = getGrades(studentId);
        if (grades == null || grades.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for (Enrollment e : grades) {
            String g = e.getGrade();
            if (g == null) {
                continue;
            }
            if (g.equalsIgnoreCase("F") || g.equalsIgnoreCase("W") || g.equalsIgnoreCase("I")) {
                continue;
            }
            try {
                sum += e.getOffering().getCourse().getCredits();
            } catch (Exception ex) {
                // ignore
            }
        }
        return sum;
    }

    @Override
    public int getCoursesCompleted(Long studentId) {
        List<Enrollment> grades = getGrades(studentId);
        if (grades == null || grades.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Enrollment e : grades) {
            String g = e.getGrade();
            if (g == null) {
                continue;
            }
            if (g.equalsIgnoreCase("F") || g.equalsIgnoreCase("W") || g.equalsIgnoreCase("I")) {
                continue;
            }
            count++;
        }

        return count;
    }
}

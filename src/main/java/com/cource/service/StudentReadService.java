package com.cource.service;

import com.cource.entity.AcademicTerm;
import com.cource.entity.Enrollment;

import java.util.List;

/**
 * Read-only student queries that can be used by non-student roles (e.g.
 * lecturers)
 * after controller-level authorization checks.
 */
public interface StudentReadService {

    List<Enrollment> getEnrollments(Long studentId);

    List<Enrollment> getGrades(Long studentId);

    List<AcademicTerm> getActiveTerms();

    double calculateGPA(Long studentId);

    int getCreditsEarned(Long studentId);

    int getCoursesCompleted(Long studentId);
}

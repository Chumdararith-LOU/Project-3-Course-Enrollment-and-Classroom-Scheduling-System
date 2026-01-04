package com.cource.service.impl;

import com.cource.entity.ClassSchedule;
import com.cource.entity.Enrollment;
import com.cource.repository.StudentRepository;
import com.cource.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final com.cource.repository.EnrollmentRepository enrollmentRepository;
    private final com.cource.repository.ClassScheduleRepository classScheduleRepository;

    @Override
    public List<ClassSchedule> getMySchedule(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        List<Long> offeringIds = enrollments.stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(e -> e.getOffering().getId())
                .collect(Collectors.toList());

        if (offeringIds.isEmpty()) {
            return new ArrayList<>();
        }

        return classScheduleRepository.findByOfferingIdIn(offeringIds);
    }
}

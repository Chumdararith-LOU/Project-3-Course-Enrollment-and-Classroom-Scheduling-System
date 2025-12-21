package com.cource.service.impl;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    // Repositories for Student Features
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;

    // Repositories for Lecturer Features
    private final CourseRepository courseRepository;
    private final AcademicTermRepository termRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCatalogForStudent(Long studentId) {
        List<CourseOffering> offerings = courseOfferingRepository.findAllActiveOfferings();
        List<CourseResponseDTO> catalog = new ArrayList<>();

        for (CourseOffering offering : offerings) {
            CourseResponseDTO dto = new CourseResponseDTO();

            Course course = offering.getCourse();
            dto.setId(offering.getId());
            dto.setCode(course.getCourseCode());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setCredits(course.getCredits());
            dto.setCapacity(offering.getCapacity());

            List<CourseLecturer> lecturers = courseLecturerRepository.findByOfferingIdAndPrimaryTrue(offering.getId());

            if (!lecturers.isEmpty()) {
                dto.setLecturer(lecturers.get(0).getLecturer().getFullName());
            } else {
                dto.setLecturer("TBA");
            }

            List<ClassSchedule> schedules = classScheduleRepository.findByOfferingId(offering.getId());
            if (!schedules.isEmpty()) {
                ClassSchedule s = schedules.get(0);
                String timeStr = s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime();
                dto.setSchedule(timeStr);
                dto.setLocation(s.getRoom().getBuilding() + " - " + s.getRoom().getRoomNumber());
            } else {
                dto.setSchedule("TBA");
                dto.setLocation("TBA");
            }

            int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offering.getId(), "ENROLLED");
            dto.setEnrolled(currentEnrolled);

            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offering.getId());
            dto.setEnrolledStatus(isEnrolled);

            catalog.add(dto);
        }

        return catalog;
    }

    @Override
    @Transactional
    public void createCourse(CourseRequestDTO dto, String lecturerEmail) {
        if (courseRepository.existsByCourseCode(dto.getCourseCode())) {
            throw new ConflictException("Course code " + dto.getCourseCode() + " already exists");
        }

        AcademicTerm term = termRepository.findById(dto.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic Term not found"));

        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        courseRepository.save(course);

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(dto.getCapacity());
        courseOfferingRepository.save(offering);

        CourseLecturer courseLecturer = new CourseLecturer();
        courseLecturer.setOffering(offering);
        courseLecturer.setLecturer(lecturer);
        courseLecturer.setPrimary(true); // This makes them show up in the catalog
        courseLecturerRepository.save(courseLecturer);
    }

    @Override
    public List<CourseResponseDTO> getCoursesByLecturerId(Long lecturerId) {
        return Collections.emptyList();
    }
}
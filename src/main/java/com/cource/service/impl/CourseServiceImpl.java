package com.cource.service.impl;

import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.entity.ClassSchedule;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.entity.Course;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.CourseRepository;
import com.cource.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final ClassScheduleRepository classScheduleRepository;

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    @Override
    @Transactional
    public Course createCourse(CourseCreateRequest request) {
        Course course = new Course();
        course.setCourseCode(request.getCourseCode());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        course.setActive(request.isActive());
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Course course = getCourseById(id);

        if (request.getCourseCode() != null) {
            course.setCourseCode(request.getCourseCode());
        }
        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCredits() != null) {
            course.setCredits(request.getCredits());
        }
        if (request.getActive() != null) {
            course.setActive(request.getActive());
        }

        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(id);
        for (CourseOffering offering : offerings) {
            Long offeringId = offering.getId();
            List<Enrollment> enrollments = enrollmentRepository.findByOfferingId(offeringId);
            enrollmentRepository.deleteAll(enrollments);
            List<CourseLecturer> lecturers = offering.getLecturers();
            courseLecturerRepository.deleteAll(lecturers);
            List<ClassSchedule> schedules = classScheduleRepository.findByOfferingId(offeringId);
            classScheduleRepository.deleteAll(schedules);
        }
        courseOfferingRepository.deleteAll(offerings);
        courseRepository.delete(course);
    }

    @Override
    @Transactional
    public Course toggleCourseStatus(Long id) {
        Course course = getCourseById(id);
        course.setActive(!course.isActive());
        return courseRepository.save(course);
    }

    @Override
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCourses();
        }
        return courseRepository.findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(keyword, keyword);
    }

    @Override
    public String generateEnrollmentCode(String courseCode) {
        String prefix = courseCode.length() >= 3 ? courseCode.substring(0, 3).toUpperCase() : courseCode.toUpperCase();
        Random random = new Random();
        int number = random.nextInt(10000);
        return String.format("%s%04d", prefix, number);
    }

    @Override
    @Transactional
    public Course regenerateEnrollmentCode(Long id) {
        Course course = getCourseById(id);
        System.out.println("Warning: regenerateEnrollmentCode called on Course. This should be done on CourseOffering.");
        return course;
    }

    @Override
    public void assignLecturersToCourse(Long courseId, List<Long> lecturerIds) {
        log.warn(
                "assignLecturersToCourse called for courseId {}. Lecturers are assigned to Course Offerings, not the Course definition.",
                courseId);
    }

    @Override
    public List<User> getLecturersForCourse(Long courseId) {
        return courseOfferingRepository.findByCourseId(courseId).stream()
                .flatMap(offering -> offering.getLecturers().stream())
                .map(CourseLecturer::getLecturer)
                .distinct()
                .collect(Collectors.toList());
    }
}
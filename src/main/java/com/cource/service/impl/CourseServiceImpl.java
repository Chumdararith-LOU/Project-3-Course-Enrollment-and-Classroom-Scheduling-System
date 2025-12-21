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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final AcademicTermRepository termRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCatalogForStudent(Long studentId) {
        return courseOfferingRepository.findAllActiveOfferings()
                .stream()
                .map(offering -> {
                    CourseResponseDTO dto = mapToDTO(offering);
                    boolean isEnrolled = enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offering.getId());
                    dto.setEnrolledStatus(isEnrolled);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createCourse(CourseRequestDTO dto, String lecturerEmail) {
        validateCourseCodeUniqueness(dto.getCourseCode());

        AcademicTerm term = fetchAcademicTerm(dto.getTermId());
        User lecturer = fetchLecturerByEmail(lecturerEmail);

        Course course = createAndSaveCourse(dto);
        CourseOffering offering = createAndSaveCourseOffering(dto, course, term);
        assignLecturerToOffering(lecturer, offering);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByLecturerId(Long lecturerId) {
        return courseLecturerRepository.findByLecturerId(lecturerId)
                .stream()
                .map(assignment -> mapToDTO(assignment.getOffering()))
                .collect(Collectors.toList());
    }

    // --- Helper to Map Entity to DTO ---
    private CourseResponseDTO mapToDTO(CourseOffering offering) {
        CourseResponseDTO dto = new CourseResponseDTO();
        Course course = offering.getCourse();

        dto.setId(offering.getId());
        // Use setCourseCode to match DTO field
        dto.setCourseCode(course.getCourseCode());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setCredits(course.getCredits());
        dto.setCapacity(offering.getCapacity());

        // Determine Active Status based on the Term
        dto.setActive(offering.getTerm() != null && offering.getTerm().isActive());

        // Set lecturer info
        setLecturerInfo(dto, offering);

        // Set schedule info
        setScheduleInfo(dto, offering);

        // Set enrollment count
        setEnrollmentCount(dto, offering);

        return dto;
    }

    private void validateCourseCodeUniqueness(String courseCode) {
        if (courseRepository.existsByCourseCode(courseCode)) {
            throw new ConflictException("Course code " + courseCode + " already exists");
        }
    }

    private AcademicTerm fetchAcademicTerm(Long termId) {
        return termRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Term not found with id: " + termId));
    }

    private User fetchLecturerByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found with email: " + email));
    }

    private Course createAndSaveCourse(CourseRequestDTO dto) {
        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        return courseRepository.save(course);
    }

    private CourseOffering createAndSaveCourseOffering(CourseRequestDTO dto, Course course, AcademicTerm term) {
        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(dto.getCapacity());
        return courseOfferingRepository.save(offering);
    }

    private void assignLecturerToOffering(User lecturer, CourseOffering offering) {
        CourseLecturer courseLecturer = new CourseLecturer();
        courseLecturer.setOffering(offering);
        courseLecturer.setLecturer(lecturer);
        courseLecturer.setPrimary(true);
        courseLecturerRepository.save(courseLecturer);
    }

    private void setLecturerInfo(CourseResponseDTO dto, CourseOffering offering) {
        courseLecturerRepository.findByOfferingIdAndPrimaryTrue(offering.getId())
                .stream()
                .findFirst()
                .ifPresentOrElse(
                        courseLecturer -> dto.setLecturer(courseLecturer.getLecturer().getFullName()),
                        () -> dto.setLecturer("TBA")
                );
    }

    private void setScheduleInfo(CourseResponseDTO dto, CourseOffering offering) {
        classScheduleRepository.findByOfferingId(offering.getId())
                .stream()
                .findFirst()
                .ifPresentOrElse(
                        schedule -> {
                            dto.setSchedule(schedule.getDayOfWeek() + " " +
                                    schedule.getStartTime() + "-" + schedule.getEndTime());

                            // Check if room is not null to avoid NullPointerException
                            if (schedule.getRoom() != null) {
                                dto.setLocation(schedule.getRoom().getBuilding() +
                                        " - " + schedule.getRoom().getRoomNumber());
                            } else {
                                dto.setLocation("TBA");
                            }
                        },
                        () -> {
                            dto.setSchedule("TBA");
                            dto.setLocation("TBA");
                        }
                );
    }

    private void setEnrollmentCount(CourseResponseDTO dto, CourseOffering offering) {
        int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offering.getId(), "ENROLLED");
        dto.setEnrolled(currentEnrolled);
    }
}
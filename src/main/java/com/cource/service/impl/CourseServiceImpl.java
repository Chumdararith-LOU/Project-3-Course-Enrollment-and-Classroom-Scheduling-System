package com.cource.service.impl;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    @PreAuthorize("hasRole('STUDENT')")
    public List<CourseResponseDTO> getCatalogForStudent(Long studentId) {
        List<CourseOffering> offerings = courseOfferingRepository.findAllActiveOfferings();
        List<CourseResponseDTO> catalog = new ArrayList<>();

        for (CourseOffering offering : offerings) {
            CourseResponseDTO dto = mapToDTO(offering);

            // Check enrollment status for this specific student
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offering.getId());
            dto.setEnrolledStatus(isEnrolled);

            catalog.add(dto);
        }

        return catalog;
    }

    @Override
    @Transactional
    // Only Lecturers can create, and they can only create for themselves (email match)
    @PreAuthorize("hasRole('LECTURER') and #lecturerEmail == authentication.name")
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

        // Create Offering
        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(dto.getCapacity());
        courseOfferingRepository.save(offering);

        // Link Lecturer
        CourseLecturer courseLecturer = new CourseLecturer();
        courseLecturer.setOffering(offering);
        courseLecturer.setLecturer(lecturer);
        courseLecturer.setPrimary(true);
        courseLecturerRepository.save(courseLecturer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LECTURER')") // Only Lecturers can view their managed courses
    public List<CourseResponseDTO> getCoursesByLecturerId(Long lecturerId) {
        List<CourseLecturer> assignments = courseLecturerRepository.findByLecturerId(lecturerId);
        List<CourseResponseDTO> myCourses = new ArrayList<>();
        for (CourseLecturer assignment : assignments) {
            myCourses.add(mapToDTO(assignment.getOffering()));
        }
        return myCourses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> searchCourses(String search) {
        return courseRepository.findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(search, search);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Course createCourse(CourseCreateRequest request) {
        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new ConflictException("Course code already exists");
        }
        Course course = new Course();
        course.setCourseCode(request.getCourseCode());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCapacity()); // Mapping capacity to credits based on DTO
        course.setActive(request.isActive());
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Course course = getCourseById(id);

        if (request.getCourseCode() != null) {
            // Check uniqueness if changed
            if (!course.getCourseCode().equals(request.getCourseCode()) &&
                    courseRepository.existsByCourseCode(request.getCourseCode())) {
                throw new ConflictException("Course code already exists");
            }
            course.setCourseCode(request.getCourseCode());
        }
        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCapacity() > 0) {
            course.setCredits(request.getCapacity());
        }

        return courseRepository.save(course);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void toggleCourseStatus(Long id) {
        Course course = getCourseById(id);
        course.setActive(!course.isActive());
        courseRepository.save(course);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void regenerateEnrollmentCode(Long id) {
        Course course = getCourseById(id);
        String newCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        courseRepository.save(course);
    }

    // --- Helper to Map Entity to DTO ---
    private CourseResponseDTO mapToDTO(CourseOffering offering) {
        CourseResponseDTO dto = new CourseResponseDTO();
        Course course = offering.getCourse();

        dto.setId(offering.getId());
        dto.setCourseCode(course.getCourseCode());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setCredits(course.getCredits());
        dto.setCapacity(offering.getCapacity());

        if (offering.getTerm() != null) {
            dto.setActive(offering.getTerm().isActive());
        }
        setLecturerInfo(dto, offering);
        setScheduleInfo(dto, offering);
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
        long currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offering.getId(), "ENROLLED");
        dto.setEnrolled((int) currentEnrolled);
    }
}
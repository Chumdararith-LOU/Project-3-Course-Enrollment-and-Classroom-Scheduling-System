package com.cource.service.impl;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.entity.AcademicTerm;
import com.cource.entity.Course;
import com.cource.entity.User;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.CourseRepository;
import com.cource.repository.UserRepository;
import com.cource.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final AcademicTermRepository termRepository;
    private final UserRepository userRepository;
    // Assuming you have this repository for the join table/entity
    // If not, you might need to create it or save via Cascade
    // private final CourseOfferingRepository courseOfferingRepository;

    @Override
    @Transactional
    public void createCourse(CourseRequestDTO dto, String lecturerEmail) {
        // 1. Validate Uniqueness
        if (courseRepository.existsByCourseCode(dto.getCourseCode())) {
            throw new ConflictException("Course code " + dto.getCourseCode() + " already exists");
        }

        // 2. Fetch Dependencies
        AcademicTerm term = termRepository.findById(dto.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic Term not found"));

        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        // 3. Create & Save Course Definition
        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        // course.setActive(true); // Default true

        courseRepository.save(course);

        // 4. Create Offering (Linking Course + Term + Lecturer)
        // Note: I am assuming your CourseOffering entity structure here based on common patterns
        /* CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(dto.getCapacity());
        // If your CourseOffering has a direct lecturer field:
        // offering.setLecturer(lecturer);
        // Or if you use a CourseLecturer join table, you would save that here.

        // courseOfferingRepository.save(offering);
        */

        // FOR NOW: Just saving the course definition to pass the compilation
        // You will uncomment the Offering logic once you confirm the CourseOffering structure
    }
}

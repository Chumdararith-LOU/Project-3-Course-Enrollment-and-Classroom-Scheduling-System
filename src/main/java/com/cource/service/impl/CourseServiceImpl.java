package com.cource.service.impl;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseLecturer;
import com.cource.entity.CourseOffering;
import com.cource.repository.*;
import com.cource.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCatalogForStudent(Long studentId) {
        // 1. Fetch all active offerings for active terms
        List<CourseOffering> offerings = courseOfferingRepository.findAllActiveOfferings();
        List<CourseResponseDTO> catalog = new ArrayList<>();

        // 2. Transform each Offering into a user-friendly DTO
        for (CourseOffering offering : offerings) {
            CourseResponseDTO dto = new CourseResponseDTO();

            // Basic Info from Course Definition
            Course course = offering.getCourse();
            dto.setId(offering.getId()); // IMPORTANT: Enrollment uses Offering ID, not Course ID
            dto.setCode(course.getCourseCode());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setCredits(course.getCredits());
            dto.setCapacity(offering.getCapacity());

            // 3. Find Primary Lecturer
            Optional<CourseLecturer> lecturer = courseLecturerRepository.findByOfferingIdAndPrimaryTrue(offering.getId());
            if (lecturer.isPresent()) {
                dto.setLecturer(lecturer.get().getLecturer().getFullName());
            } else {
                dto.setLecturer("TBA");
            }

            // 4. Find Schedule & Location
            // (Assuming 1 schedule per offering for simplicity in this DTO, or we format multiple)
            List<ClassSchedule> schedules = classScheduleRepository.findByOfferingId(offering.getId());
            if (!schedules.isEmpty()) {
                ClassSchedule s = schedules.get(0); // Take the first one
                String timeStr = s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime();
                dto.setSchedule(timeStr);
                dto.setLocation(s.getRoom().getBuilding() + " - " + s.getRoom().getRoomNumber());
            } else {
                dto.setSchedule("TBA");
                dto.setLocation("TBA");
            }

            // 5. Calculate Enrollment Stats
            int currentEnrolled = enrollmentRepository.countByOfferingIdAndStatus(offering.getId(), "ENROLLED");
            dto.setEnrolled(currentEnrolled);

            // 6. Check if THIS student is enrolled
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndOfferingId(studentId, offering.getId());
            dto.setEnrolledStatus(isEnrolled);

            catalog.add(dto);
        }

        return catalog;
    }
}

package com.cource.service.impl;

// import com.cource.dto.schedule.ScheduleRequestDTO;
// import com.cource.dto.schedule.ScheduleResponseDTO;
// import com.cource.entity.ClassSchedule;
// import com.cource.entity.CourseLecturer;
// import com.cource.entity.CourseOffering;
// import com.cource.entity.Room;
// import com.cource.entity.User;
// import com.cource.repository.ClassScheduleRepository;
// import com.cource.repository.CourseLecturerRepository;
// import com.cource.repository.CourseOfferingRepository;
// import com.cource.repository.EnrollmentRepository;
// import com.cource.repository.RoomRepository;
// import com.cource.repository.UserRepository;
// import com.cource.service.ScheduleService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.stream.Collectors;

// @Service
// @Transactional
// @RequiredArgsConstructor
public class ScheduleServiceImpl { // implements ScheduleService 

    // private final ClassScheduleRepository scheduleRepository;
    // private final CourseOfferingRepository offeringRepository;
    // private final RoomRepository roomRepository;
    // private final CourseLecturerRepository courseLecturerRepository;
    // private final EnrollmentRepository enrollmentRepository;
    // private final UserRepository userRepository;

    // @Override
    // public ScheduleResponseDTO createSchedule(ScheduleRequestDTO dto, String lecturerEmail) {
    //     CourseOffering offering = offeringRepository.findById(dto.getOfferingId())
    //             .orElseThrow(() -> new RuntimeException("Course offering not found"));
    //     Room room = roomRepository.findById(dto.getRoomId())
    //             .orElseThrow(() -> new RuntimeException("Room not found"));

    //     ClassSchedule schedule = new ClassSchedule();
    //     schedule.setOffering(offering);
    //     schedule.setRoom(room);
    //     schedule.setDayOfWeek(dto.getDayOfWeek());
    //     schedule.setStartTime(dto.getStartTime());
    //     schedule.setEndTime(dto.getEndTime());

    //     schedule = scheduleRepository.save(schedule);
    //     return mapToResponse(schedule, null);
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public List<ScheduleResponseDTO> getSchedulesByLecturer(String lecturerEmail) {
    //     User lecturer = userRepository.findByEmail(lecturerEmail)
    //             .orElseThrow(() -> new RuntimeException("Lecturer not found"));

    //     List<CourseLecturer> courseLecturers = courseLecturerRepository.findByLecturerId(lecturer.getId());

    //     List<ScheduleResponseDTO> result = new ArrayList<>();
    //     for (CourseLecturer cl : courseLecturers) {
    //         List<ClassSchedule> schedules = scheduleRepository.findByOfferingId(cl.getOffering().getId());
    //         for (ClassSchedule cs : schedules) {
    //             result.add(mapToResponse(cs, lecturer.getFullName()));
    //         }
    //     }
    //     return result;
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public List<ScheduleResponseDTO> getStudentSchedule(Long studentId) {
    //     List<Long> enrolledOfferingIds = enrollmentRepository.findByStudentId(studentId)
    //             .stream()
    //             .filter(e -> e.getStatus() != null && e.getStatus().equalsIgnoreCase("ENROLLED"))
    //             .map(e -> e.getOffering().getId())
    //             .collect(Collectors.toList());

    //     if (enrolledOfferingIds.isEmpty()) {
    //         return new ArrayList<>();
    //     }

    //     List<ScheduleResponseDTO> result = new ArrayList<>();
    //     for (Long offeringId : enrolledOfferingIds) {
    //         List<ClassSchedule> schedules = scheduleRepository.findByOfferingId(offeringId);
    //         for (ClassSchedule cs : schedules) {
    //             String lecturerName = getLecturerName(cs.getOffering());
    //             result.add(mapToResponse(cs, lecturerName));
    //         }
    //     }
    //     return result;
    // }

    // private ScheduleResponseDTO mapToResponse(ClassSchedule cs, String lecturerName) {
    //     ScheduleResponseDTO dto = new ScheduleResponseDTO();
    //     dto.setId(cs.getId());
    //     dto.setDayOfWeek(cs.getDayOfWeek());
    //     dto.setStartTime(cs.getStartTime());
    //     dto.setEndTime(cs.getEndTime());

    //     if (cs.getRoom() != null) {
    //         dto.setRoomNumber(cs.getRoom().getRoomNumber());
    //     }

    //     CourseOffering offering = cs.getOffering();
    //     if (offering != null && offering.getCourse() != null) {
    //         dto.setCourseName(offering.getCourse().getTitle());
    //         dto.setCourseCode(offering.getCourse().getCode());
    //     }

    //     dto.setLecturerName(lecturerName);
    //     return dto;
    // }

    // private String getLecturerName(CourseOffering offering) {
    //     if (offering.getLecturers() == null || offering.getLecturers().isEmpty()) {
    //         return null;
    //     }
    //     for (CourseLecturer cl : offering.getLecturers()) {
    //         if (cl.isPrimary() && cl.getLecturer() != null) {
    //             return cl.getLecturer().getFullName();
    //         }
    //     }
    //     CourseLecturer first = offering.getLecturers().get(0);
    //     if (first.getLecturer() != null) {
    //         return first.getLecturer().getFullName();
    //     }
    //     return null;
    // }
}

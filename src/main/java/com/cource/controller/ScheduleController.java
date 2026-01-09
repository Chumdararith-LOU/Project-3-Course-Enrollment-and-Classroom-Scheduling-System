package com.cource.controller;

import com.cource.entity.ClassSchedule;
import com.cource.dto.schedule.ClassScheduleDTO;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.RoomRepository;
import com.cource.entity.Room;
import com.cource.entity.CourseOffering;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.CourseLecturerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private CourseLecturerRepository courseLecturerRepository;

    // Get all schedules (optionally by offering) — return DTOs to avoid
    // lazy-serialization issues. Wrap in try/catch to surface errors in logs.
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long offeringId) {
        try {
            if (offeringId != null) {
                return ResponseEntity.ok(
                        com.cource.dto.schedule.ClassScheduleMapper
                                .toDtoList(classScheduleRepository.findByOfferingId(offeringId)));
            }
            return ResponseEntity
                    .ok(com.cource.dto.schedule.ClassScheduleMapper.toDtoList(classScheduleRepository.findAll()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", ex.toString()));
        }
    }

    // Get a single schedule
    @GetMapping("/{id}")
    public ResponseEntity<ClassScheduleDTO> getById(@PathVariable Long id) {
        return classScheduleRepository.findById(id)
                .map(cs -> ResponseEntity.ok(toDto(cs)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Create a new schedule
    @PostMapping
    public ResponseEntity<ClassScheduleDTO> create(@RequestBody ClassSchedule schedule) {
        // Validate offering and room
        if (schedule.getOffering() == null || schedule.getRoom() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<CourseOffering> offering = courseOfferingRepository.findById(schedule.getOffering().getId());
        Optional<Room> room = roomRepository.findById(schedule.getRoom().getId());
        if (offering.isEmpty() || room.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        schedule.setOffering(offering.get());
        schedule.setRoom(room.get());
        ClassSchedule saved = classScheduleRepository.save(schedule);
        return ResponseEntity.ok(toDto(saved));
    }

    // Update a schedule
    @PutMapping("/{id}")
    public ResponseEntity<ClassScheduleDTO> update(@PathVariable Long id, @RequestBody ClassSchedule schedule) {
        return classScheduleRepository.findById(id)
                .map(existing -> {
                    if (schedule.getOffering() != null) {
                        Optional<CourseOffering> offering = courseOfferingRepository
                                .findById(schedule.getOffering().getId());
                        offering.ifPresent(existing::setOffering);
                    }
                    if (schedule.getRoom() != null) {
                        Optional<Room> room = roomRepository.findById(schedule.getRoom().getId());
                        room.ifPresent(existing::setRoom);
                    }
                    if (schedule.getDayOfWeek() != null)
                        existing.setDayOfWeek(schedule.getDayOfWeek());
                    if (schedule.getStartTime() != null)
                        existing.setStartTime(schedule.getStartTime());
                    if (schedule.getEndTime() != null)
                        existing.setEndTime(schedule.getEndTime());
                    ClassSchedule saved = classScheduleRepository.save(existing);
                    return ResponseEntity.ok(toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete a schedule
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!classScheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        classScheduleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Compatibility endpoint: create schedule from form/URL params
    // (lecturer-friendly)
    @PostMapping("/create-from-params")
    public ResponseEntity<?> createFromParams(
            @RequestParam Long lecturerId,
            @RequestParam Long offeringId,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String roomNumber,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String roomType) {
        // verify lecturer is assigned to offering
        boolean assigned = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId);
        if (!assigned) {
            return ResponseEntity.status(403).body("Lecturer is not assigned to this offering");
        }
        var offeringOpt = courseOfferingRepository.findById(offeringId);
        if (offeringOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid offeringId");
        }

        java.time.LocalTime start;
        java.time.LocalTime end;
        try {
            start = java.time.LocalTime.parse(startTime);
            end = java.time.LocalTime.parse(endTime);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid time format. Use HH:mm or HH:mm:ss");
        }

        Room room = roomRepository.findByRoomNumber(roomNumber).orElseGet(() -> {
            Room r = new Room();
            r.setRoomNumber(roomNumber);
            r.setBuilding(building != null ? building : "");
            r.setCapacity(0);
            r.setRoomType(roomType != null ? roomType : "");
            return roomRepository.save(r);
        });
        // if room exists but building/roomType provided and fields empty, update them
        boolean changed = false;
        if (room.getBuilding() == null || room.getBuilding().isEmpty()) {
            if (building != null && !building.isEmpty()) {
                room.setBuilding(building);
                changed = true;
            }
        }
        if (room.getRoomType() == null || room.getRoomType().isEmpty()) {
            if (roomType != null && !roomType.isEmpty()) {
                room.setRoomType(roomType);
                changed = true;
            }
        }
        if (changed)
            roomRepository.save(room);

        ClassSchedule cs = new ClassSchedule();
        cs.setOffering(offeringOpt.get());
        cs.setRoom(room);
        cs.setDayOfWeek(dayOfWeek);
        cs.setStartTime(start);
        cs.setEndTime(end);

        ClassSchedule saved = classScheduleRepository.save(cs);
        return ResponseEntity.ok(toDto(saved));
    }

    // Compatibility endpoint: update schedule from form/URL params
    @PostMapping("/update-from-params")
    public ResponseEntity<?> updateFromParams(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam Long lecturerId,
            @RequestParam Long offeringId,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String roomNumber,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String roomType) {
        // verify lecturer is assigned to offering
        boolean assigned = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId);
        if (!assigned) {
            return ResponseEntity.status(403).body("Lecturer is not assigned to this offering");
        }
        var offeringOpt = courseOfferingRepository.findById(offeringId);
        if (offeringOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid offeringId");
        }

        java.time.LocalTime start;
        java.time.LocalTime end;
        try {
            start = java.time.LocalTime.parse(startTime);
            end = java.time.LocalTime.parse(endTime);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid time format. Use HH:mm or HH:mm:ss");
        }

        Room room = roomRepository.findByRoomNumber(roomNumber).orElseGet(() -> {
            Room r = new Room();
            r.setRoomNumber(roomNumber);
            r.setBuilding(building != null ? building : "");
            r.setCapacity(0);
            r.setRoomType(roomType != null ? roomType : "");
            return roomRepository.save(r);
        });
        boolean changed = false;
        if (room.getBuilding() == null || room.getBuilding().isEmpty()) {
            if (building != null && !building.isEmpty()) {
                room.setBuilding(building);
                changed = true;
            }
        }
        if (room.getRoomType() == null || room.getRoomType().isEmpty()) {
            if (roomType != null && !roomType.isEmpty()) {
                room.setRoomType(roomType);
                changed = true;
            }
        }
        if (changed)
            roomRepository.save(room);

        ClassSchedule cs;
        if (scheduleId != null) {
            cs = classScheduleRepository.findById(scheduleId).orElse(null);
            if (cs == null) {
                return ResponseEntity.notFound().build();
            }
            cs.setOffering(offeringOpt.get());
            cs.setRoom(room);
            cs.setDayOfWeek(dayOfWeek);
            cs.setStartTime(start);
            cs.setEndTime(end);
        } else {
            cs = new ClassSchedule();
            cs.setOffering(offeringOpt.get());
            cs.setRoom(room);
            cs.setDayOfWeek(dayOfWeek);
            cs.setStartTime(start);
            cs.setEndTime(end);
        }

        ClassSchedule saved = classScheduleRepository.save(cs);
        return ResponseEntity.ok(toDto(saved));
    }

    // Convert entity to DTO for safe JSON serialization
    private ClassScheduleDTO toDto(ClassSchedule cs) {
        ClassScheduleDTO dto = new ClassScheduleDTO();
        dto.setId(cs.getId());
        ClassScheduleDTO.RoomDTO room = new ClassScheduleDTO.RoomDTO();
        if (cs.getRoom() != null) {
            room.setId(cs.getRoom().getId());
            room.setRoomNumber(cs.getRoom().getRoomNumber());
            room.setBuilding(cs.getRoom().getBuilding());
            room.setRoomType(cs.getRoom().getRoomType());
        }
        dto.setRoom(room);
        dto.setDayOfWeek(cs.getDayOfWeek());
        dto.setStartTime(cs.getStartTime() != null ? cs.getStartTime().toString() : null);
        dto.setEndTime(cs.getEndTime() != null ? cs.getEndTime().toString() : null);
        if (cs.getOffering() != null) {
            ClassScheduleDTO.OfferingDTO off = new ClassScheduleDTO.OfferingDTO();
            off.setId(cs.getOffering().getId());
            off.setCapacity(cs.getOffering().getCapacity());
            off.setActive(cs.getOffering().isActive());
            if (cs.getOffering().getCourse() != null) {
                ClassScheduleDTO.CourseDTO c = new ClassScheduleDTO.CourseDTO();
                c.setId(cs.getOffering().getCourse().getId());
                c.setCourseCode(cs.getOffering().getCourse().getCourseCode());
                c.setTitle(cs.getOffering().getCourse().getTitle());
                off.setCourse(c);
            }
            if (cs.getOffering().getTerm() != null) {
                ClassScheduleDTO.TermDTO t = new ClassScheduleDTO.TermDTO();
                t.setId(cs.getOffering().getTerm().getId());
                t.setTermName(cs.getOffering().getTerm().getTermName());
                off.setTerm(t);
            }
            dto.setOffering(off);
        }
        return dto;
    }
}

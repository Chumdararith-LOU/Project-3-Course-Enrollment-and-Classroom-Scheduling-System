package com.cource.controller;

import com.cource.entity.ClassSchedule;
import com.cource.dto.schedule.ClassScheduleDTO;
import com.cource.service.ClassScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
public class ScheduleController {

    private final ClassScheduleService classScheduleService;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long offeringId) {
        return ResponseEntity.ok(classScheduleService.getAll(offeringId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassScheduleDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(classScheduleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ClassScheduleDTO> create(@RequestBody ClassSchedule schedule) {
        return ResponseEntity.ok(classScheduleService.create(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassScheduleDTO> update(@PathVariable Long id, @RequestBody ClassSchedule schedule) {
        return ResponseEntity.ok(classScheduleService.update(id, schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

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
        return ResponseEntity.ok(classScheduleService.createFromParams(
                lecturerId, offeringId, dayOfWeek, startTime, endTime, roomNumber, building, roomType));
    }

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
        return ResponseEntity.ok(classScheduleService.updateFromParams(
                scheduleId, lecturerId, offeringId, dayOfWeek, startTime, endTime, roomNumber, building, roomType));
    }
}

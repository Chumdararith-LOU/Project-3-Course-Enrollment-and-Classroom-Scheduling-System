package com.cource.service.impl;

import com.cource.dto.schedule.ClassScheduleDTO;
import com.cource.dto.schedule.ClassScheduleMapper;
import com.cource.entity.ClassSchedule;
import com.cource.entity.CourseOffering;
import com.cource.entity.Room;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.exception.UnauthorizedException;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.RoomRepository;
import com.cource.service.ClassScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final RoomRepository roomRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassScheduleDTO> getAll(Long offeringId) {
        if (offeringId != null) {
            return ClassScheduleMapper.toDtoList(classScheduleRepository.findByOfferingId(offeringId));
        }
        return ClassScheduleMapper.toDtoList(classScheduleRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDTO getById(Long id) {
        ClassSchedule cs = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        return ClassScheduleMapper.toDto(cs);
    }

    @Override
    public ClassScheduleDTO create(ClassSchedule schedule) {
        validateSchedule(schedule);

        CourseOffering offering = courseOfferingRepository.findById(schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        Room room = roomRepository.findById(schedule.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Check for room time conflicts
        checkRoomTimeConflict(room.getId(), schedule.getDayOfWeek(),
                schedule.getStartTime(), schedule.getEndTime(), null);

        // Check lecturer time conflicts
        if (offering.getLecturer() == null) {
            throw new IllegalStateException("Offering must have an assigned lecturer for scheduling.");
        }
        checkLecturerTimeConflict(offering.getLecturer().getId(), schedule.getDayOfWeek(),
                schedule.getStartTime(), schedule.getEndTime(), null);

        // CRITICAL: Check room capacity vs offering capacity
        checkRoomCapacity(room, offering.getCapacity());

        schedule.setOffering(offering);
        schedule.setRoom(room);

        ClassSchedule saved = classScheduleRepository.save(schedule);
        return ClassScheduleMapper.toDto(saved);
    }

    @Override
    public ClassScheduleDTO update(Long id, ClassSchedule schedule) {
        ClassSchedule existing = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        // Validate input schedule
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule update data cannot be null");
        }

        CourseOffering offeringToUpdate = existing.getOffering();
        Room roomToUpdate = existing.getRoom();

        // Update fields if provided
        if (schedule.getOffering() != null && schedule.getOffering().getId() != null) {
            offeringToUpdate = courseOfferingRepository.findById(schedule.getOffering().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("New Offering not found"));
        }

        if (schedule.getRoom() != null && schedule.getRoom().getId() != null) {
            roomToUpdate = roomRepository.findById(schedule.getRoom().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("New Room not found"));
        }

        String dayOfWeek = schedule.getDayOfWeek() != null ? schedule.getDayOfWeek() : existing.getDayOfWeek();
        LocalTime startTime = schedule.getStartTime() != null ? schedule.getStartTime() : existing.getStartTime();
        LocalTime endTime = schedule.getEndTime() != null ? schedule.getEndTime() : existing.getEndTime();

        // Validate time order
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // Check for conflicts
        checkRoomTimeConflict(roomToUpdate.getId(), dayOfWeek, startTime, endTime, id);

        if (offeringToUpdate.getLecturer() == null) {
            throw new IllegalStateException("Offering must have an assigned lecturer for scheduling.");
        }
        checkLecturerTimeConflict(offeringToUpdate.getLecturer().getId(), dayOfWeek, startTime, endTime, id);

        // CRITICAL: Check room capacity vs offering capacity
        checkRoomCapacity(roomToUpdate, offeringToUpdate.getCapacity());

        // Apply updates
        existing.setOffering(offeringToUpdate);
        existing.setRoom(roomToUpdate);
        existing.setDayOfWeek(dayOfWeek);
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);

        ClassSchedule saved = classScheduleRepository.save(existing);
        return ClassScheduleMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!classScheduleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Schedule not found");
        }
        classScheduleRepository.deleteById(id);
    }

    @Override
    public ClassScheduleDTO createFromParams(Long lecturerId, Long offeringId, String dayOfWeek, String startTime,
                                             String endTime, String roomNumber, String building, String roomType) {

        CourseOffering offering = validateLecturerAssignment(offeringId, lecturerId);

        LocalTime start = parseTime(startTime);
        LocalTime end = parseTime(endTime);

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Room room = getOrCreateRoom(roomNumber, building, roomType);

        checkRoomTimeConflict(room.getId(), dayOfWeek.toUpperCase(), start, end, null);
        checkLecturerTimeConflict(lecturerId, dayOfWeek.toUpperCase(), start, end, null);

        checkRoomCapacity(room, offering.getCapacity());

        ClassSchedule cs = new ClassSchedule();
        cs.setOffering(offering);
        cs.setRoom(room);
        cs.setDayOfWeek(dayOfWeek.toUpperCase());
        cs.setStartTime(start);
        cs.setEndTime(end);

        ClassSchedule saved = classScheduleRepository.save(cs);
        return ClassScheduleMapper.toDto(saved);
    }

    @Override
    public ClassScheduleDTO updateFromParams(Long scheduleId, Long lecturerId, Long offeringId, String dayOfWeek,
                                             String startTime, String endTime, String roomNumber, String building, String roomType) {

        if (scheduleId == null) {
            throw new IllegalArgumentException("scheduleId is required for update");
        }

        CourseOffering offering = validateLecturerAssignment(offeringId, lecturerId);

        LocalTime start = parseTime(startTime);
        LocalTime end = parseTime(endTime);

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Room room = getOrCreateRoom(roomNumber, building, roomType);

        checkRoomTimeConflict(room.getId(), dayOfWeek.toUpperCase(), start, end, scheduleId);
        checkLecturerTimeConflict(lecturerId, dayOfWeek.toUpperCase(), start, end, scheduleId);

        checkRoomCapacity(room, offering.getCapacity());

        ClassSchedule cs = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        cs.setOffering(offering);
        cs.setRoom(room);
        cs.setDayOfWeek(dayOfWeek.toUpperCase());
        cs.setStartTime(start);
        cs.setEndTime(end);

        ClassSchedule saved = classScheduleRepository.save(cs);
        return ClassScheduleMapper.toDto(saved);
    }

    private void validateSchedule(ClassSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule is required");
        }
        if (schedule.getOffering() == null || schedule.getOffering().getId() == null) {
            throw new IllegalArgumentException("offering.id is required");
        }
        if (schedule.getRoom() == null || schedule.getRoom().getId() == null) {
            throw new IllegalArgumentException("room.id is required");
        }
        if (schedule.getDayOfWeek() == null || schedule.getDayOfWeek().trim().isEmpty()) {
            throw new IllegalArgumentException("dayOfWeek is required");
        }
        if (schedule.getStartTime() == null) {
            throw new IllegalArgumentException("startTime is required");
        }
        if (schedule.getEndTime() == null) {
            throw new IllegalArgumentException("endTime is required");
        }

        if (!schedule.getStartTime().isBefore(schedule.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    private CourseOffering validateLecturerAssignment(Long offeringId, Long lecturerId) {
        if (offeringId == null || lecturerId == null) {
            throw new IllegalArgumentException("offeringId and lecturerId are required");
        }

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

        if (offering.getLecturer() == null || !offering.getLecturer().getId().equals(lecturerId)) {
            throw new UnauthorizedException("Lecturer is not assigned to this offering");
        }

        return offering;
    }

    private void checkRoomTimeConflict(Long roomId, String dayOfWeek, LocalTime startTime,
                                       LocalTime endTime, Long excludeScheduleId) {
        List<ClassSchedule> conflicts = classScheduleRepository.findConflictingSchedules(
                roomId, dayOfWeek.toUpperCase(), startTime, endTime, excludeScheduleId);

        if (!conflicts.isEmpty()) {
            ClassSchedule conflict = conflicts.get(0);
            throw new ConflictException(String.format(
                    "Room is already booked on %s from %s to %s for %s",
                    dayOfWeek, conflict.getStartTime(), conflict.getEndTime(),
                    conflict.getOffering().getCourse().getCourseCode()));
        }
    }

    private void checkLecturerTimeConflict(Long lecturerId, String dayOfWeek, LocalTime startTime,
                                           LocalTime endTime, Long excludeScheduleId) {
        List<ClassSchedule> conflicts = classScheduleRepository.findConflictingSchedulesForLecturer(
                lecturerId, dayOfWeek.toUpperCase(), startTime, endTime, excludeScheduleId);

        if (!conflicts.isEmpty()) {
            ClassSchedule conflict = conflicts.get(0);
            throw new ConflictException(String.format(
                    "Lecturer already has a class on %s from %s to %s for %s",
                    dayOfWeek, conflict.getStartTime(), conflict.getEndTime(),
                    conflict.getOffering().getCourse().getCourseCode()));
        }
    }

    private void checkRoomCapacity(Room room, int offeringCapacity) {
        if (room.getCapacity() <= 0) {
            throw new ConflictException(String.format(
                    "Room %s has no capacity set (0 or negative). Please update room capacity before scheduling.",
                    room.getRoomNumber()));
        }

        if (offeringCapacity > room.getCapacity()) {
            throw new ConflictException(String.format(
                    "Room %s capacity (%d) is less than offering capacity (%d). Cannot schedule.",
                    room.getRoomNumber(), room.getCapacity(), offeringCapacity));
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Time is required");
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid time format. Use HH:mm or HH:mm:ss");
        }
    }

    private Room getOrCreateRoom(String roomNumber, String building, String roomType) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("roomNumber is required");
        }

        return roomRepository.findByRoomNumber(roomNumber)
                .orElseGet(() -> {
                    Room newRoom = new Room();
                    newRoom.setRoomNumber(roomNumber);
                    newRoom.setBuilding(building != null ? building : "");
                    newRoom.setCapacity(0); // Default capacity - admin should update
                    newRoom.setRoomType(roomType != null ? roomType : "");
                    newRoom.setActive(true); // Use the correct field name
                    return roomRepository.save(newRoom);
                });
    }
}
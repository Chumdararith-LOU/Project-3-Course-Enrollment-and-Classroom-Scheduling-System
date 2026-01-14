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
import com.cource.repository.CourseLecturerRepository;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.EnrollmentRepository;
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
    private final CourseLecturerRepository courseLecturerRepository;
    private final EnrollmentRepository enrollmentRepository;

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
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule is required");
        }
        if (schedule.getOffering() == null || schedule.getOffering().getId() == null) {
            throw new IllegalArgumentException("offering.id is required");
        }
        if (schedule.getRoom() == null || schedule.getRoom().getId() == null) {
            throw new IllegalArgumentException("room.id is required");
        }

        // Validate time order
        if (schedule.getStartTime() != null && schedule.getEndTime() != null
                && schedule.getStartTime().isAfter(schedule.getEndTime())) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }

        CourseOffering offering = courseOfferingRepository.findById(schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        Room room = roomRepository.findById(schedule.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Check for room time conflicts
        if (classScheduleRepository.existsOverlap(
                room.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime())) {
            throw new ConflictException(String.format(
                    "Room %s is already booked on %s from %s to %s",
                    room.getRoomNumber(), schedule.getDayOfWeek(),
                    schedule.getStartTime(), schedule.getEndTime()));
        }

        // Check room capacity vs enrolled students
        long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(offering.getId(), "ACTIVE");
        if (room.getCapacity() > 0 && enrolledCount > room.getCapacity()) {
            throw new ConflictException(String.format(
                    "Room %s capacity (%d) is less than enrolled students (%d)",
                    room.getRoomNumber(), room.getCapacity(), enrolledCount));
        }

        schedule.setOffering(offering);
        schedule.setRoom(room);

        ClassSchedule saved = classScheduleRepository.save(schedule);
        return ClassScheduleMapper.toDto(saved);
    }

    @Override
    public ClassScheduleDTO update(Long id, ClassSchedule schedule) {
        ClassSchedule existing = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        if (schedule != null) {
            if (schedule.getOffering() != null && schedule.getOffering().getId() != null) {
                CourseOffering offering = courseOfferingRepository.findById(schedule.getOffering().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
                existing.setOffering(offering);
            }
            if (schedule.getRoom() != null && schedule.getRoom().getId() != null) {
                Room room = roomRepository.findById(schedule.getRoom().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
                existing.setRoom(room);
            }
            if (schedule.getDayOfWeek() != null) {
                existing.setDayOfWeek(schedule.getDayOfWeek());
            }
            if (schedule.getStartTime() != null) {
                existing.setStartTime(schedule.getStartTime());
            }
            if (schedule.getEndTime() != null) {
                existing.setEndTime(schedule.getEndTime());
            }

            // Validate time order
            if (existing.getStartTime() != null && existing.getEndTime() != null
                    && existing.getStartTime().isAfter(existing.getEndTime())) {
                throw new IllegalArgumentException("Start time cannot be after end time");
            }

            // Check for room time conflicts (exclude current schedule)
            if (classScheduleRepository.existsOverlapWithId(
                    existing.getRoom().getId(),
                    existing.getDayOfWeek(),
                    existing.getStartTime(),
                    existing.getEndTime(),
                    id)) {
                throw new ConflictException(String.format(
                        "Room %s is already booked on %s from %s to %s",
                        existing.getRoom().getRoomNumber(), existing.getDayOfWeek(),
                        existing.getStartTime(), existing.getEndTime()));
            }

            // Check room capacity vs enrolled students
            long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(existing.getOffering().getId(),
                    "ACTIVE");
            if (existing.getRoom().getCapacity() > 0 && enrolledCount > existing.getRoom().getCapacity()) {
                throw new ConflictException(String.format(
                        "Room %s capacity (%d) is less than enrolled students (%d)",
                        existing.getRoom().getRoomNumber(), existing.getRoom().getCapacity(), enrolledCount));
            }
        }

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

        verifyLecturerAssigned(offeringId, lecturerId);

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid offeringId"));

        LocalTime start = parseTime(startTime);
        LocalTime end = parseTime(endTime);

        // Validate start time is before end time
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Room room = upsertRoom(roomNumber, building, roomType);

        // Check for room time conflict
        List<ClassSchedule> roomConflicts = classScheduleRepository.findConflictingSchedules(
                room.getId(), dayOfWeek.toUpperCase(), start, end, null);
        if (!roomConflicts.isEmpty()) {
            ClassSchedule conflict = roomConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "Room %s is already booked on %s from %s to %s",
                    roomNumber, dayOfWeek, conflict.getStartTime(), conflict.getEndTime()));
        }

        // Check for lecturer time conflict
        List<ClassSchedule> lecturerConflicts = classScheduleRepository.findLecturerConflictingSchedules(
                lecturerId, dayOfWeek.toUpperCase(), start, end, null);
        if (!lecturerConflicts.isEmpty()) {
            ClassSchedule conflict = lecturerConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "Lecturer already has a class on %s from %s to %s",
                    dayOfWeek, conflict.getStartTime(), conflict.getEndTime()));
        }

        // Check room capacity vs enrolled students
        long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ACTIVE");
        if (room.getCapacity() > 0 && enrolledCount > room.getCapacity()) {
            throw new ConflictException(String.format(
                    "Room %s capacity (%d) is less than enrolled students (%d)",
                    roomNumber, room.getCapacity(), enrolledCount));
        }

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

        verifyLecturerAssigned(offeringId, lecturerId);

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid offeringId"));

        LocalTime start = parseTime(startTime);
        LocalTime end = parseTime(endTime);

        // Validate start time is before end time
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Room room = upsertRoom(roomNumber, building, roomType);

        // Check for room time conflict (exclude current schedule)
        List<ClassSchedule> roomConflicts = classScheduleRepository.findConflictingSchedules(
                room.getId(), dayOfWeek.toUpperCase(), start, end, scheduleId);
        if (!roomConflicts.isEmpty()) {
            ClassSchedule conflict = roomConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "Room %s is already booked on %s from %s to %s",
                    roomNumber, dayOfWeek, conflict.getStartTime(), conflict.getEndTime()));
        }

        // Check for lecturer time conflict (exclude current schedule)
        List<ClassSchedule> lecturerConflicts = classScheduleRepository.findLecturerConflictingSchedules(
                lecturerId, dayOfWeek.toUpperCase(), start, end, scheduleId);
        if (!lecturerConflicts.isEmpty()) {
            ClassSchedule conflict = lecturerConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "Lecturer already has a class on %s from %s to %s",
                    dayOfWeek, conflict.getStartTime(), conflict.getEndTime()));
        }

        // Check room capacity vs enrolled students
        long enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ACTIVE");
        if (room.getCapacity() > 0 && enrolledCount > room.getCapacity()) {
            throw new ConflictException(String.format(
                    "Room %s capacity (%d) is less than enrolled students (%d)",
                    roomNumber, room.getCapacity(), enrolledCount));
        }

        ClassSchedule cs;
        if (scheduleId != null) {
            cs = classScheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        } else {
            cs = new ClassSchedule();
        }

        cs.setOffering(offering);
        cs.setRoom(room);
        cs.setDayOfWeek(dayOfWeek.toUpperCase());
        cs.setStartTime(start);
        cs.setEndTime(end);

        ClassSchedule saved = classScheduleRepository.save(cs);
        return ClassScheduleMapper.toDto(saved);
    }

    private void verifyLecturerAssigned(Long offeringId, Long lecturerId) {
        if (offeringId == null || lecturerId == null) {
            throw new IllegalArgumentException("offeringId and lecturerId are required");
        }
        boolean assigned = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId);
        if (!assigned) {
            throw new UnauthorizedException("Lecturer is not assigned to this offering");
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

    private Room upsertRoom(String roomNumber, String building, String roomType) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("roomNumber is required");
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
        if ((room.getBuilding() == null || room.getBuilding().isEmpty())
                && building != null && !building.isEmpty()) {
            room.setBuilding(building);
            changed = true;
        }
        if ((room.getRoomType() == null || room.getRoomType().isEmpty())
                && roomType != null && !roomType.isEmpty()) {
            room.setRoomType(roomType);
            changed = true;
        }
        if (changed) {
            room = roomRepository.save(room);
        }

        return room;
    }
}

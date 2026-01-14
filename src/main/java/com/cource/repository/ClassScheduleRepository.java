package com.cource.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cource.entity.ClassSchedule;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByOfferingId(Long offeringId);

    List<ClassSchedule> findByRoomId(Long roomId);

    List<ClassSchedule> findByOfferingIdIn(List<Long> offeringIds);

    @Query("SELECT cs FROM ClassSchedule cs JOIN cs.offering o JOIN o.lecturers cl WHERE cs.offering.id = :offeringId AND cl.lecturer.id = :lecturerId")
    List<ClassSchedule> findByOfferingIdAndLecturerId(@Param("offeringId") Long offeringId,
            @Param("lecturerId") Long lecturerId);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.id = :scheduleId")
    Optional<ClassSchedule> findScheduleById(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(cs) FROM ClassSchedule cs WHERE cs.offering.id IN :offeringIds")
    long countByOfferingIds(@Param("offeringIds") List<Long> offeringIds);

    // Time conflict check: find schedules in the same room on the same day that overlap with the given time range
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.room.id = :roomId AND cs.dayOfWeek = :dayOfWeek " +
           "AND cs.startTime < :endTime AND cs.endTime > :startTime AND (:excludeId IS NULL OR cs.id <> :excludeId)")
    List<ClassSchedule> findConflictingSchedules(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    // Check lecturer time conflict: find if lecturer has another class at the same time
    @Query("SELECT cs FROM ClassSchedule cs JOIN cs.offering.lecturers cl " +
           "WHERE cl.lecturer.id = :lecturerId AND cs.dayOfWeek = :dayOfWeek " +
           "AND cs.startTime < :endTime AND cs.endTime > :startTime AND (:excludeId IS NULL OR cs.id <> :excludeId)")
    List<ClassSchedule> findLecturerConflictingSchedules(
            @Param("lecturerId") Long lecturerId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}
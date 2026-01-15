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

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.offering.id = :offeringId AND cs.offering.lecturer.id = :lecturerId")
    List<ClassSchedule> findByOfferingIdAndLecturerId(@Param("offeringId") Long offeringId,
                                                      @Param("lecturerId") Long lecturerId);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.id = :scheduleId")
    Optional<ClassSchedule> findScheduleById(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(cs) FROM ClassSchedule cs WHERE cs.offering.id IN :offeringIds")
    long countByOfferingIds(@Param("offeringIds") List<Long> offeringIds);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.room.id = :roomId AND cs.dayOfWeek = :dayOfWeek " +
            "AND cs.startTime < :endTime AND cs.endTime > :startTime AND (:excludeId IS NULL OR cs.id <> :excludeId)")
    List<ClassSchedule> findConflictingSchedules(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    @Query("SELECT cs FROM ClassSchedule cs " +
            "WHERE cs.offering.lecturer.id = :lecturerId " +
            "AND cs.dayOfWeek = :dayOfWeek " +
            "AND cs.startTime < :endTime AND cs.endTime > :startTime " +
            "AND (:excludeId IS NULL OR cs.id <> :excludeId)")
    List<ClassSchedule> findLecturerConflictingSchedules(
            @Param("lecturerId") Long lecturerId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(cs) > 0 FROM ClassSchedule cs " +
            "WHERE cs.room.id = :roomId " +
            "AND cs.dayOfWeek = :dayOfWeek " +
            "AND cs.startTime < :endTime " +
            "AND cs.endTime > :startTime")
    boolean existsOverlap(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(cs) > 0 FROM ClassSchedule cs " +
            "WHERE cs.room.id = :roomId " +
            "AND cs.dayOfWeek = :dayOfWeek " +
            "AND cs.startTime < :endTime " +
            "AND cs.endTime > :startTime " +
            "AND cs.id != :scheduleId")
    boolean existsOverlapWithId(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("scheduleId") Long scheduleId);

    @Query("SELECT cs FROM ClassSchedule cs " +
            "WHERE cs.offering.lecturer.id = :lecturerId " +
            "AND cs.dayOfWeek = :dayOfWeek " +
            "AND cs.startTime < :endTime AND cs.endTime > :startTime " +
            "AND (:excludeId IS NULL OR cs.id <> :excludeId)")
    List<ClassSchedule> findConflictingSchedulesForLecturer(
            @Param("lecturerId") Long lecturerId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}
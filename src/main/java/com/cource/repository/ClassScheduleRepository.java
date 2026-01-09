package com.cource.repository;

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
}
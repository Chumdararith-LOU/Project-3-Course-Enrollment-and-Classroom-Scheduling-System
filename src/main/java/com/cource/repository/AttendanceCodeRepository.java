package com.cource.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.AttendanceCode;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceCodeRepository extends JpaRepository<AttendanceCode, Long> {
    Optional<AttendanceCode> findBySchedule_Id(Long scheduleId);
    Optional<AttendanceCode> findByCode(String code);

    @Query("DELETE FROM AttendanceCode ac WHERE ac.schedule.id = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}

package com.cource.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.AttendanceCode;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceCodeRepository extends JpaRepository<AttendanceCode, Long> {
    Optional<AttendanceCode> findBySchedule_Id(Long scheduleId);

    @Modifying
    @Query("DELETE FROM AttendanceCode ac WHERE ac.schedule.id = :scheduleId")
    void deleteByScheduleId(Long scheduleId);
}

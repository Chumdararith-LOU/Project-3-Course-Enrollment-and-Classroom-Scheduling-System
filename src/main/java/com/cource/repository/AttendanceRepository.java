package com.cource.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cource.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Attendance a WHERE a.enrollment.student.id = :studentId AND a.schedule.id = :scheduleId AND a.enrollment.id = :enrollmentId AND a.attendanceDate = :date")
    boolean existsByStudentIdAndScheduleId(@Param("studentId") Long studentId, @Param("scheduleId") Long scheduleId, @Param("enrollmentId") Long enrollmentId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.enrollment.student.id = :studentId")
    List<Attendance> findByStudentId(@Param("studentId") Long studentId);
    
    List<Attendance> findByScheduleId(Long scheduleId);
}

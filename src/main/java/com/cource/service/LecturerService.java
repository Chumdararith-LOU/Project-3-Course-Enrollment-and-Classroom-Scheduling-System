package com.cource.service;

import java.util.List;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.User;

public interface LecturerService {
    LecturerDashboardDTO getDashboardStats(Long lecturerId);

    List<Course> getCoursesByLecturerId(long lecturerId);

    List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId);

    List<User> getEnrolledStudents(long offeringId, long lecturerId);

    void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status, long lecturerId);

    List<Attendance> getAttendanceRecords(long scheduleId, long lecturerId);

}

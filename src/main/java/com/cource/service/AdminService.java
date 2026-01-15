package com.cource.service;

import com.cource.entity.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface AdminService {

        // User Management
        List<User> getAllUsers();
        List<User> getUsersByRole(String roleName);
        long getTotalStudents();
        long getTotalLecturers();

        // Course Management
        List<Course> getAllCourses();
        long getTotalCourses();

        // Course Offering Management
        List<CourseOffering> getAllCourseOfferings();
        List<CourseOffering> getCourseOfferingsByTerm(Long termId);
        CourseOffering getOfferingById(Long id);
        CourseOffering createOffering(Long courseId, Long termId, Long lecturerId, Integer capacity, Boolean isActive);
        CourseOffering updateOffering(Long id, Long courseId, Long termId, Long lecturerId, Integer capacity, Boolean isActive);
        CourseOffering regenerateOfferingEnrollmentCode(Long offeringId);
        void deleteOffering(Long id);
        CourseOffering toggleOfferingStatus(Long id);

        // Enrollment Management
        List<Enrollment> getAllEnrollments();
        List<Enrollment> getEnrollmentsByOffering(Long offeringId);
        long getTotalEnrollments();
        Enrollment getEnrollmentById(Long id);
        Enrollment createEnrollment(Long studentId, Long offeringId);
        Enrollment updateEnrollmentGrade(Long id, String grade);
        Enrollment updateEnrollmentStatus(Long id, String status);
        void deleteEnrollment(Long id);

        // Schedule management
        List<ClassSchedule> getAllSchedules();
        ClassSchedule getScheduleById(Long id);
        ClassSchedule createSchedule(Long offeringId, Long roomId, String dayOfWeek, LocalTime startTime, LocalTime endTime);
        ClassSchedule updateSchedule(Long id, Long offeringId, Long roomId, String dayOfWeek, LocalTime startTime, LocalTime endTime);
        void deleteSchedule(Long id);
        List<ClassSchedule> getSchedulesByOffering(Long offeringId);
        List<ClassSchedule> getSchedulesByRoom(Long roomId);

        // Room management
        List<Room> getAllRooms();
        Room getRoomById(Long id);
        Room createRoom(String roomNumber, String building, Integer capacity, String roomType, Boolean isActive);
        Room updateRoom(Long id, String roomNumber, String building, Integer capacity, String roomType, Boolean isActive);
        void deleteRoom(Long id);
        Room toggleRoomStatus(Long id);

        // Term management
        List<AcademicTerm> getAllTerms();
        AcademicTerm getTermById(Long id);
        String generateTermCode(java.time.LocalDate startDate);
        AcademicTerm createTerm(String termCode, String termName, LocalDate startDate, LocalDate endDate);
        AcademicTerm updateTerm(Long id, String termCode, String termName, LocalDate startDate, LocalDate endDate);
        void deleteTerm(Long id);
        AcademicTerm toggleTermStatus(Long id);

        // Statistics
        Map<String, Object> getEnrollmentStatsByTerm();
        Map<String, Object> getCoursePopularity();
}
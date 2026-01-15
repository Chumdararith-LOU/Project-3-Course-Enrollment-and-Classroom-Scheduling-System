package com.cource.service;

import com.cource.entity.*;
import java.util.List;
import java.util.Map;

public interface AdminService {
        List<User> getAllUsers();
        List<User> getUsersByRole(String roleName);

        long getTotalStudents();
        long getTotalCourses();
        long getTotalLecturers();

        List<Course> getAllCourses();
        List<CourseOffering> getAllCourseOfferings();
        List<CourseOffering> getCourseOfferingsByTerm(Long termId);

        CourseOffering getOfferingById(Long id);
        CourseOffering createOffering(Long courseId, Long termId, Long lecturerId, Integer capacity, Boolean isActive);
        CourseOffering updateOffering(Long id, Long courseId, Long termId, Long lecturerId, Integer capacity, Boolean isActive);        CourseOffering regenerateOfferingEnrollmentCode(Long offeringId);

        void deleteOffering(Long id);

        CourseOffering toggleOfferingStatus(Long id);

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
        // Room management
        List<Room> getAllRooms();
        // Term management
        List<AcademicTerm> getAllTerms();

        AcademicTerm getTermById(Long id);

        String generateTermCode(java.time.LocalDate startDate);

        AcademicTerm createTerm(String termCode, String termName, java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        AcademicTerm updateTerm(Long id, String termCode, String termName, java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        void deleteTerm(Long id);

        AcademicTerm toggleTermStatus(Long id);

        // Room management - CRUD
        Room getRoomById(Long id);

        Room createRoom(String roomNumber, String building, Integer capacity, String roomType, Boolean isActive);

        Room updateRoom(Long id, String roomNumber, String building, Integer capacity, String roomType,
                        Boolean isActive);

        void deleteRoom(Long id);

        Room toggleRoomStatus(Long id);

        ClassSchedule getScheduleById(Long id);
        ClassSchedule createSchedule(Long offeringId, Long roomId, String dayOfWeek, java.time.LocalTime startTime,
                        java.time.LocalTime endTime);
        ClassSchedule updateSchedule(Long id, Long offeringId, Long roomId, String dayOfWeek,
                        java.time.LocalTime startTime,
                        java.time.LocalTime endTime);

        void deleteSchedule(Long id);

        List<ClassSchedule> getSchedulesByOffering(Long offeringId);
        List<ClassSchedule> getSchedulesByRoom(Long roomId);

        Map<String, Object> getEnrollmentStatsByTerm();
        Map<String, Object> getCoursePopularity();

}
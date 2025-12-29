package com.cource.controller;

import java.util.List;
import java.util.Map;

import com.cource.dto.course.AcademicTermRequestDTO;
import com.cource.dto.schedule.RoomRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.AcademicTerm;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.Room;
import com.cource.entity.User;
import com.cource.service.AdminService;
import com.cource.service.CourseService;
import com.cource.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final CourseService courseService;

    // ===== User Management =====
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/role/{roleName}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(adminService.getUsersByRole(roleName));
    }

    @GetMapping("/users/stats/students")
    public ResponseEntity<Long> getTotalStudents() {
        return ResponseEntity.ok(adminService.getTotalStudents());
    }

    @GetMapping("/users/stats/lecturers")
    public ResponseEntity<Long> getTotalLecturers() {
        return ResponseEntity.ok(adminService.getTotalLecturers());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // ===== Course Management =====
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            return ResponseEntity.ok(courseService.searchCourses(search));
        }
        return ResponseEntity.ok(adminService.getAllCourses());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(@RequestBody CourseCreateRequest request) {
        courseService.createCourse(request);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody CourseUpdateRequest request) {
        courseService.updateCourse(id, request);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PatchMapping("/courses/{id}/toggle-status")
    public ResponseEntity<?> toggleCourseStatus(@PathVariable Long id) {
        courseService.toggleCourseStatus(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PostMapping("/courses/{id}/regenerate-code")
    public ResponseEntity<?> regenerateEnrollmentCode(@PathVariable Long id) {
        courseService.regenerateEnrollmentCode(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @GetMapping("/courses/stats/total")
    public ResponseEntity<Long> getTotalCourses() {
        return ResponseEntity.ok(adminService.getTotalCourses());
    }

    // ===== Course Offerings Management =====
    @GetMapping("/offerings")
    public ResponseEntity<List<CourseOffering>> getAllCourseOfferings() {
        return ResponseEntity.ok(adminService.getAllCourseOfferings());
    }

    // --- Lecturer Assignment Endpoints ---
    @GetMapping("/offerings/{offeringId}/lecturers")
    public ResponseEntity<List<User>> getLecturersForOffering(@PathVariable Long offeringId) {
        return ResponseEntity.ok(adminService.getLecturersForOffering(offeringId));
    }

    @PostMapping("/offerings/{offeringId}/lecturers")
    public ResponseEntity<?> assignLecturersToOffering(@PathVariable Long offeringId,
            @RequestBody List<Long> lecturerIds) {
        adminService.assignLecturersToOffering(offeringId, lecturerIds);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @DeleteMapping("/offerings/{offeringId}/lecturers/{lecturerId}")
    public ResponseEntity<?> removeLecturerFromOffering(@PathVariable Long offeringId, @PathVariable Long lecturerId) {
        adminService.removeLecturerFromOffering(offeringId, lecturerId);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @GetMapping("/offerings/{id}")
    public ResponseEntity<CourseOffering> getOfferingById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getOfferingById(id));
    }

    @GetMapping("/offerings/term/{termId}")
    public ResponseEntity<List<CourseOffering>> getCourseOfferingsByTerm(@PathVariable Long termId) {
        return ResponseEntity.ok(adminService.getCourseOfferingsByTerm(termId));
    }

    @PostMapping("/offerings")
    public ResponseEntity<?> createOffering(@RequestBody Map<String, Object> request) {
        Long courseId = Long.valueOf(request.get("courseId").toString());
        Long termId = Long.valueOf(request.get("termId").toString());
        Integer capacity = Integer.valueOf(request.get("capacity").toString());
        Boolean isActive = request.containsKey("isActive") ? (Boolean) request.get("isActive") : true;
        List<Long> lecturerIds = null;
        if (request.containsKey("lecturerIds")) {
            lecturerIds = ((List<?>) request.get("lecturerIds")).stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
        }
        var offering = adminService.createOffering(courseId, termId, capacity, isActive);
        if (lecturerIds != null && !lecturerIds.isEmpty()) {
            adminService.assignLecturersToOffering(offering.getId(), lecturerIds);
        }
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PutMapping("/offerings/{id}")
    public ResponseEntity<?> updateOffering(@PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Long courseId = Long.valueOf(request.get("courseId").toString());
        Long termId = Long.valueOf(request.get("termId").toString());
        Integer capacity = Integer.valueOf(request.get("capacity").toString());
        Boolean isActive = request.containsKey("isActive") ? (Boolean) request.get("isActive") : null;
        adminService.updateOffering(id, courseId, termId, capacity, isActive);
        List<Long> lecturerIds = null;
        if (request.containsKey("lecturerIds")) {
            lecturerIds = ((List<?>) request.get("lecturerIds")).stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .toList();
        }
        if (lecturerIds != null) {
            adminService.assignLecturersToOffering(id, lecturerIds);
        }
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @DeleteMapping("/offerings/{id}")
    public ResponseEntity<Void> deleteOffering(@PathVariable Long id) {
        adminService.deleteOffering(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/offerings/{id}/toggle")
    public ResponseEntity<?> toggleOffering(@PathVariable Long id) {
        adminService.toggleOfferingStatus(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PatchMapping("/offerings/{id}/toggle-status")
    public ResponseEntity<?> toggleOfferingStatus(@PathVariable Long id) {
        adminService.toggleOfferingStatus(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    // ===== Enrollment Management =====
    @GetMapping("/enrollments")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(adminService.getAllEnrollments());
    }

    @GetMapping("/enrollments/offering/{offeringId}")
    public ResponseEntity<List<Enrollment>> getEnrollmentsByOffering(@PathVariable Long offeringId) {
        return ResponseEntity.ok(adminService.getEnrollmentsByOffering(offeringId));
    }

    @GetMapping("/enrollments/stats/total")
    public ResponseEntity<Long> getTotalEnrollments() {
        return ResponseEntity.ok(adminService.getTotalEnrollments());
    }

    @GetMapping("/enrollments/{id}")
    public ResponseEntity<Enrollment> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEnrollmentById(id));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<Enrollment> createEnrollment(@RequestBody Map<String, Object> request) {
        Long studentId = Long.valueOf(request.get("studentId").toString());
        Long offeringId = Long.valueOf(request.get("offeringId").toString());
        return ResponseEntity.ok(adminService.createEnrollment(studentId, offeringId));
    }

    @PatchMapping("/enrollments/{id}/grade")
    public ResponseEntity<Enrollment> updateEnrollmentGrade(@PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String grade = request.get("grade").toString();
        return ResponseEntity.ok(adminService.updateEnrollmentGrade(id, grade));
    }

    @PatchMapping("/enrollments/{id}/status")
    public ResponseEntity<Enrollment> updateEnrollmentStatus(@PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String status = request.get("status").toString();
        return ResponseEntity.ok(adminService.updateEnrollmentStatus(id, status));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        adminService.deleteEnrollment(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Schedule Management =====
    @GetMapping("/schedules")
    public ResponseEntity<List<ClassSchedule>> getAllSchedules() {
        return ResponseEntity.ok(adminService.getAllSchedules());
    }

    @GetMapping("/schedules/{id}")
    public ResponseEntity<ClassSchedule> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getScheduleById(id));
    }

    @GetMapping("/schedules/offering/{offeringId}")
    public ResponseEntity<List<ClassSchedule>> getSchedulesByOffering(@PathVariable Long offeringId) {
        return ResponseEntity.ok(adminService.getSchedulesByOffering(offeringId));
    }

    @GetMapping("/schedules/room/{roomId}")
    public ResponseEntity<List<ClassSchedule>> getSchedulesByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(adminService.getSchedulesByRoom(roomId));
    }

    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@RequestBody Map<String, Object> request) {
        Long offeringId = Long.valueOf(request.get("offeringId").toString());
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String dayOfWeek = request.get("dayOfWeek").toString();
        java.time.LocalTime startTime = java.time.LocalTime.parse(request.get("startTime").toString());
        java.time.LocalTime endTime = java.time.LocalTime.parse(request.get("endTime").toString());
        adminService.createSchedule(offeringId, roomId, dayOfWeek, startTime, endTime);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Long offeringId = Long.valueOf(request.get("offeringId").toString());
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String dayOfWeek = request.get("dayOfWeek").toString();
        java.time.LocalTime startTime = java.time.LocalTime.parse(request.get("startTime").toString());
        java.time.LocalTime endTime = java.time.LocalTime.parse(request.get("endTime").toString());
        adminService.updateSchedule(id, offeringId, roomId, dayOfWeek, startTime, endTime);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        adminService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(adminService.getAllRooms());
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getRoomById(id));
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@RequestBody @Valid RoomRequestDTO request) {
        adminService.createRoom(
                request.getRoomNumber(),
                request.getBuilding(),
                request.getCapacity(),
                request.getRoomType(),
                request.getIsActive()
        );
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody @Valid RoomRequestDTO request) {
        adminService.updateRoom(
                id,
                request.getRoomNumber(),
                request.getBuilding(),
                request.getCapacity(),
                request.getRoomType(),
                request.getIsActive()
        );
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        adminService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rooms/{id}/toggle-status")
    public ResponseEntity<Room> toggleRoomStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleRoomStatus(id));
    }

    // ===== Term Management =====
    @GetMapping("/terms")
    public ResponseEntity<List<AcademicTerm>> getAllTerms() {
        return ResponseEntity.ok(adminService.getAllTerms());
    }

    @GetMapping("/terms/{id}")
    public ResponseEntity<AcademicTerm> getTermById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTermById(id));
    }

    @PostMapping("/terms")
    public ResponseEntity<?> createTerm(@RequestBody @Valid AcademicTermRequestDTO request) {
        adminService.createTerm(
                request.getTermCode(),
                request.getTermName(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PutMapping("/terms/{id}")
    public ResponseEntity<?> updateTerm(@PathVariable Long id, @RequestBody @Valid AcademicTermRequestDTO request) {
        adminService.updateTerm(
                id,
                request.getTermCode(),
                request.getTermName(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @DeleteMapping("/terms/{id}")
    public ResponseEntity<?> deleteTerm(@PathVariable Long id) {
        adminService.deleteTerm(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    @PatchMapping("/terms/{id}/toggle-status")
    public ResponseEntity<?> toggleTermStatus(@PathVariable Long id) {
        adminService.toggleTermStatus(id);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "success"));
    }

    // ===== Statistics & Reports =====
    @GetMapping("/stats/enrollments-by-term")
    public ResponseEntity<Map<String, Object>> getEnrollmentStatsByTerm() {
        return ResponseEntity.ok(adminService.getEnrollmentStatsByTerm());
    }

    @GetMapping("/stats/course-popularity")
    public ResponseEntity<Map<String, Object>> getCoursePopularity() {
        return ResponseEntity.ok(adminService.getCoursePopularity());
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = Map.of(
                "totalStudents", adminService.getTotalStudents(),
                "totalLecturers", adminService.getTotalLecturers(),
                "totalCourses", adminService.getTotalCourses(),
                "totalEnrollments", adminService.getTotalEnrollments());
        return ResponseEntity.ok(summary);
    }
}

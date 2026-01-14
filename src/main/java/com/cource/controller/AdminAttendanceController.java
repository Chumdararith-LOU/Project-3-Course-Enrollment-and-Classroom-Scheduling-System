package com.cource.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cource.repository.AttendanceRepository;
import com.cource.service.AdminService;
import com.cource.entity.Attendance;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final AdminService adminService;

    @GetMapping("/admin/attendance")
    public String attendancePage(Model model) {
        return attendancePage(null, null, null, null, model);
    }

    @GetMapping("/admin/attendance/list")
    public String attendancePage(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Model model) {
        List<Attendance> rows = null;
        try {
            if (offeringId != null && from != null && to != null) {
                LocalDate f = LocalDate.parse(from);
                LocalDate t = LocalDate.parse(to);
                rows = attendanceRepository.findByOfferingIdBetweenDates(offeringId, f, t);
            } else if (scheduleId != null) {
                rows = attendanceRepository.findByScheduleIdWithStudent(scheduleId);
            } else {
                rows = attendanceRepository.findAll();
            }
        } catch (Exception ex) {
            rows = attendanceRepository.findAll();
        }

        model.addAttribute("attendanceRows", rows);

        try {
            model.addAttribute("offerings", adminService.getAllCourseOfferings());
            if (offeringId != null) {
                model.addAttribute("schedules", adminService.getSchedulesByOffering(offeringId));
            } else {
                model.addAttribute("schedules", adminService.getAllSchedules());
            }
        } catch (Exception ignored) {
            model.addAttribute("offerings", new java.util.ArrayList<>());
            model.addAttribute("schedules", new java.util.ArrayList<>());
        }

        model.addAttribute("offeringId", offeringId);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        model.addAttribute("userId", 1);
        model.addAttribute("role", "ADMIN");

        return "admin/attendance";
    }

    @GetMapping("/api/admin/attendance")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Attendance> rows = attendanceRepository.findAll();
        List<Map<String, Object>> out = rows.stream().map(a -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("date", a.getAttendanceDate());
            m.put("status", a.getStatus());
            if (a.getEnrollment() != null && a.getEnrollment().getStudent() != null) {
                m.put("student", a.getEnrollment().getStudent().getFullName());
            }
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/api/admin/attendance/{id}/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        var a = attendanceRepository.findById(id).orElse(null);
        if (a == null)
            return ResponseEntity.notFound().build();
        attendanceRepository.delete(a);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

}

package com.cource.controller.Admin;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cource.util.SecurityHelper;
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
    private final SecurityHelper securityHelper;

    @GetMapping({"/admin/attendance", "/admin/attendance/list"})
    public String attendancePage(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long adminId,
            Model model) {

        Long userId = adminId != null ? adminId : securityHelper.getCurrentUserId();
        if (userId != null) {
            model.addAttribute("userId", userId);
            model.addAttribute("adminId", userId);
            model.addAttribute("role", "ADMIN");
        }

        List<Attendance> rows;
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
            rows = Collections.emptyList();
        }

        long totalRecords = rows.size();
        long presentCount = 0;
        long absentCount = 0;
        long lateCount = 0;

        for (Attendance a : rows) {
            if (a.getStatus() != null) {
                switch (a.getStatus().toUpperCase()) {
                    case "PRESENT":
                        presentCount++;
                        break;
                    case "ABSENT":
                        absentCount++;
                        break;
                    case "LATE":
                        lateCount++;
                        break;
                }
            }
        }

        model.addAttribute("attendanceRows", rows);
        model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("lateCount", lateCount);

        try {
            model.addAttribute("offerings", adminService.getAllCourseOfferings());
            model.addAttribute("schedules", adminService.getAllSchedules());
        } catch (Exception ignored) {
            model.addAttribute("offerings", Collections.emptyList());
            model.addAttribute("schedules", Collections.emptyList());
        }

        model.addAttribute("offeringId", offeringId);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);

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

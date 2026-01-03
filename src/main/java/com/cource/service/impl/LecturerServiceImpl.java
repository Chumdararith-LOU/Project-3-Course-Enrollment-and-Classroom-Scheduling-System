package com.cource.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.exception.ConflictException;
import com.cource.repository.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseLecturer;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.service.CourseService;
import com.cource.service.LecturerService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
public class LecturerServiceImpl implements LecturerService {

    private final CourseLecturerRepository courseLecturerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final AcademicTermRepository academicTermRepository;
    private final CourseService courseService;

    public LecturerServiceImpl(CourseLecturerRepository courseLecturerRepository,
            AttendanceRepository attendanceRepository,
            ClassScheduleRepository classScheduleRepository,
            EnrollmentRepository enrollmentRepository,
            CourseOfferingRepository courseOfferingRepository,
            CourseRepository courseRepository,
            AcademicTermRepository academicTermRepository,
            CourseService courseService) {
        this.attendanceRepository = attendanceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseLecturerRepository = courseLecturerRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseRepository = courseRepository;
        this.academicTermRepository = academicTermRepository;
        this.courseService = courseService;
    }

    @Override
    public List<Course> getCoursesByLecturerId(long lecturerId) {
        return courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> cl.getOffering().getCourse())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        return classScheduleRepository.findByOfferingIdAndLecturerId(offeringId, lecturerId);
    }

    @Override
    public List<User> getEnrolledStudents(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        List<User> students = enrollmentRepository.findByOfferingId(offeringId).stream()
                // Include students with null status (default) or ENROLLED status
                .filter(e -> e.getStatus() == null || "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
        System.out.println("[DEBUG] getEnrolledStudents for offeringId=" + offeringId + ", lecturerId=" + lecturerId);
        for (User s : students) {
            System.out.println("[DEBUG] Student: id=" + s.getId() + ", email=" + s.getEmail() + ", firstName="
                    + s.getFirstName() + ", lastName=" + s.getLastName() + ", active=" + s.isActive() + ", role="
                    + (s.getRole() != null ? s.getRole().getRoleName() : "null"));
        }
        return students;
    }

    @Override
    public void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status) {
        ClassSchedule schedule = classScheduleRepository.findById(attendanceRequestDTO.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        verifyOwnership(schedule.getOffering().getId(), attendanceRequestDTO.getLecturerId());

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(
                studentId, schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(attendanceRequestDTO.getAttendanceDate());
        attendance.setStatus(status);

        User lecturer = new User();
        lecturer.setId(attendanceRequestDTO.getLecturerId());
        attendance.setRecordedBy(lecturer);

        if (attendanceRequestDTO.getNotes() != null) {
            attendance.setNotes(attendanceRequestDTO.getNotes());
        }

        attendanceRepository.save(attendance);
    }

    @Override
    public Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        // verify lecturer owns the offering for this attendance
        if (attendance.getSchedule() == null || attendance.getSchedule().getOffering() == null) {
            throw new ResourceNotFoundException("Associated schedule/offering not found");
        }
        if (lecturerId != null) {
            verifyOwnership(attendance.getSchedule().getOffering().getId(), lecturerId);
        }

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            attendance.setStatus(dto.getStatus());
        }
        if (dto.getAttendanceDate() != null) {
            attendance.setAttendanceDate(dto.getAttendanceDate());
        }
        if (dto.getNotes() != null) {
            attendance.setNotes(dto.getNotes());
        }

        return attendanceRepository.save(attendance);
    }

    @Override
    public void deleteAttendance(long attendanceId, Long lecturerId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        if (attendance.getSchedule() == null || attendance.getSchedule().getOffering() == null) {
            throw new ResourceNotFoundException("Associated schedule/offering not found");
        }
        if (lecturerId != null) {
            verifyOwnership(attendance.getSchedule().getOffering().getId(), lecturerId);
        }
        attendanceRepository.delete(attendance);
    }

    @Override
    public List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        if (lecturerId != null) {
            verifyOwnership(schedule.getOffering().getId(), lecturerId);
        }
        List<Attendance> rows = attendanceRepository.findByScheduleId(scheduleId);
        // Sort by attendanceDate descending (latest first)
        rows.sort((a, b) -> b.getAttendanceDate().compareTo(a.getAttendanceDate()));
        // Initialize lazy associations while still in transaction to avoid
        // LazyInitializationException during JSON serialization
        for (Attendance a : rows) {
            if (a.getEnrollment() != null) {
                var enrol = a.getEnrollment();
                if (enrol.getStudent() != null) {
                    enrol.getStudent().getId();
                    enrol.getStudent().getFirstName();
                    enrol.getStudent().getLastName();
                }
                enrol.getId();
            }
            if (a.getRecordedBy() != null) {
                a.getRecordedBy().getId();
            }
            if (a.getSchedule() != null) {
                a.getSchedule().getId();
            }
        }
        return rows;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId) {
        System.out.println(
                "[DEBUG] getAttendanceRecordsAsDto start for scheduleId=" + scheduleId + ", lecturerId=" + lecturerId);
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        try {
            List<Attendance> rows = attendanceRepository.findByScheduleIdWithStudent(scheduleId);
            System.out.println("[DEBUG] loaded attendance rows count=" + (rows == null ? 0 : rows.size()));
            for (Attendance a : rows) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", a.getId());
                m.put("attendanceDate", a.getAttendanceDate());
                m.put("status", a.getStatus());
                m.put("notes", a.getNotes());
                if (a.getEnrollment() != null && a.getEnrollment().getStudent() != null) {
                    var student = a.getEnrollment().getStudent();
                    java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                    s.put("id", student.getId());
                    s.put("firstName", student.getFirstName());
                    s.put("lastName", student.getLastName());
                    s.put("email", student.getEmail());
                    s.put("fullName", student.getFullName());
                    m.put("student", s);
                }
                if (a.getRecordedBy() != null) {
                    var rb = a.getRecordedBy();
                    java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
                    r.put("id", rb.getId());
                    r.put("fullName", rb.getFullName());
                    m.put("recordedBy", r);
                }
                if (a.getRecordedAt() != null) {
                    m.put("recordedAt", a.getRecordedAt().toString());
                }
                out.add(m);
            }
            System.out.println("[DEBUG] mapped dto count=" + out.size());
            return out;
        } catch (Exception ex) {
            System.err.println("[ERROR] getAttendanceRecordsAsDto failed: " + ex.getMessage());
            ex.printStackTrace();
            // fallback: try to load with the previous method (will initialize lazies)
            try {
                List<Attendance> rows = getAttendanceRecords(scheduleId, lecturerId);
                for (Attendance a : rows) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("attendanceDate", a.getAttendanceDate());
                    m.put("status", a.getStatus());
                    m.put("notes", a.getNotes());
                    if (a.getEnrollment() != null && a.getEnrollment().getStudent() != null) {
                        var student = a.getEnrollment().getStudent();
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("id", student.getId());
                        s.put("firstName", student.getFirstName());
                        s.put("lastName", student.getLastName());
                        s.put("email", student.getEmail());
                        s.put("fullName", student.getFullName());
                        m.put("student", s);
                    }
                    if (a.getRecordedBy() != null) {
                        var rb = a.getRecordedBy();
                        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
                        r.put("id", rb.getId());
                        r.put("fullName", rb.getFullName());
                        m.put("recordedBy", r);
                    }
                    if (a.getRecordedAt() != null) {
                        m.put("recordedAt", a.getRecordedAt().toString());
                    }
                    out.add(m);
                }
            } catch (Exception ex2) {
                System.err.println("[ERROR] fallback mapping also failed: " + ex2.getMessage());
                ex2.printStackTrace();
            }
            return out;
        }
    }

    @Override
    public java.util.Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days) {
        var offerings = courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> cl.getOffering().getId()).distinct().toList();

        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        if (offerings.isEmpty())
            return result;

        java.time.LocalDate from = java.time.LocalDate.now().minusDays(days - 1);
        var rows = attendanceRepository.countByOfferingIdsSince(offerings, from);
        for (Object[] r : rows) {
            java.time.LocalDate date = (java.time.LocalDate) r[0];
            Long count = ((Number) r[1]).longValue();
            result.put(date.toString(), count);
        }
        return result;
    }

    @Override
    public java.util.Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId) {
        var offerings = courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> cl.getOffering()).distinct().toList();
        java.util.Map<String, Double> out = new java.util.LinkedHashMap<>();
        for (var off : offerings) {
            var enrolls = enrollmentRepository.findByOfferingId(off.getId()).stream()
                    .filter(e -> e.getGrade() != null && !e.getGrade().isEmpty()).toList();
            if (enrolls.isEmpty()) {
                out.put(off.getCourse().getCourseCode() + " - " + off.getCourse().getTitle(), 0.0);
                continue;
            }
            double sum = 0.0;
            int count = 0;
            for (var e : enrolls) {
                String g = e.getGrade().toUpperCase();
                Double val = switch (g) {
                    case "A" -> 4.0;
                    case "B" -> 3.0;
                    case "C" -> 2.0;
                    case "D" -> 1.0;
                    case "F" -> 0.0;
                    default -> null;
                };
                if (val != null) {
                    sum += val;
                    count++;
                }
            }
            double avg = count == 0 ? 0.0 : sum / count;
            out.put(off.getCourse().getCourseCode() + " - " + off.getCourse().getTitle(), avg);
        }
        return out;
    }

    @Override
    public java.util.List<com.course.entity.CourseOffering> getOfferingsByLecturerId(long lecturerId) {
        var offerings = courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> cl.getOffering())
                .distinct()
                .toList();
        System.out.println("[DEBUG] getOfferingsByLecturerId for lecturerId=" + lecturerId);
        for (var off : offerings) {
            System.out.println("[DEBUG] Offering: id=" + off.getId() + ", course="
                    + (off.getCourse() != null ? off.getCourse().getCourseCode() : "null") + ", term="
                    + (off.getTerm() != null ? off.getTerm().getTermCode() : "null"));
        }
        return offerings;
    }

    private void verifyOwnership(long offeringId, long lecturerId) {
        boolean isOwner = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId);
        if (!isOwner) {
            throw new SecurityException("Lecturer does not have access to this course offering.");
        }
    }

    @Override
    public com.course.entity.CourseOffering createCourseOffering(long lecturerId,
            com.course.dto.course.CourseOfferingRequestDTO dto) {
        if (dto.getCourseId() == null || dto.getTermId() == null) {
            throw new IllegalArgumentException("courseId and termId are required");
        }
        var course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        var term = academicTermRepository.findById(dto.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));

        // Check unique offering
        var existing = courseOfferingRepository.findByCourseIdAndTermId(course.getId(), term.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("An offering for this course and term already exists");
        }

        com.course.entity.CourseOffering offering = new com.course.entity.CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        if (dto.getCapacity() != null)
            offering.setCapacity(dto.getCapacity());
        if (dto.getActive() != null)
            offering.setActive(dto.getActive());

        // determine enrollment code: use provided or generate a new one
        if (dto.getEnrollmentCode() != null && !dto.getEnrollmentCode().isBlank()) {
            if (courseOfferingRepository.existsByEnrollmentCode(dto.getEnrollmentCode())) {
                throw new IllegalArgumentException("Enrollment code already in use");
            }
            offering.setEnrollmentCode(dto.getEnrollmentCode());
        } else {
            offering.setEnrollmentCode(courseService.generateEnrollmentCode(course.getCourseCode()));
        }

        offering = courseOfferingRepository.save(offering);

        // assign lecturer as primary
        com.course.entity.User lecturer = new com.course.entity.User();
        lecturer.setId(lecturerId);
        com.course.entity.CourseLecturer cl = new com.course.entity.CourseLecturer();
        cl.setOffering(offering);
        cl.setLecturer(lecturer);
        cl.setPrimary(true);
        courseLecturerRepository.save(cl);

        return offering;
    }

    @Override
    public com.course.entity.CourseOffering updateCourseOffering(long lecturerId, long offeringId,
            com.course.dto.course.CourseOfferingRequestDTO dto) {
        verifyOwnership(offeringId, lecturerId);
        var offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        if (dto.getCourseId() != null
                && (offering.getCourse() == null || !offering.getCourse().getId().equals(dto.getCourseId()))) {
            var course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
            offering.setCourse(course);
        }
        if (dto.getTermId() != null
                && (offering.getTerm() == null || !offering.getTerm().getId().equals(dto.getTermId()))) {
            var term = academicTermRepository.findById(dto.getTermId())
                    .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
            offering.setTerm(term);
        }
        if (dto.getCapacity() != null)
            offering.setCapacity(dto.getCapacity());
        if (dto.getActive() != null)
            offering.setActive(dto.getActive());
        if (dto.getEnrollmentCode() != null) {
            String newCode = dto.getEnrollmentCode().trim();
            if (!newCode.isBlank()) {
                boolean exists = courseOfferingRepository.existsByEnrollmentCode(newCode);
                if (exists && (offering.getEnrollmentCode() == null || !offering.getEnrollmentCode().equals(newCode))) {
                    throw new IllegalArgumentException("Enrollment code already in use");
                }
                offering.setEnrollmentCode(newCode);
            } else {
                // if blank explicitly, ignore to avoid violating NOT NULL DB constraint
            }
        }
        return courseOfferingRepository.save(offering);
    }

    @Override
    public com.course.entity.CourseOffering getOfferingById(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        return courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
    }

    @Override
    public com.course.entity.CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        var offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        String newCode;
        int attempts = 0;
        do {
            newCode = courseService.generateEnrollmentCode(offering.getCourse().getCourseCode());
            attempts++;
            if (attempts > 10)
                break; // safety
        } while (courseOfferingRepository.existsByEnrollmentCode(newCode));
        offering.setEnrollmentCode(newCode);
        return courseOfferingRepository.save(offering);
    }

    @Override
    public void deleteCourseOffering(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        courseOfferingRepository.deleteById(offeringId);
    }
}
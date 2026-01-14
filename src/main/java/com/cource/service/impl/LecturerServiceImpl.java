package com.cource.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.dto.lecturer.LecturerCourseDetailDTO;
import com.cource.dto.lecturer.LecturerCourseReportDTO;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.CourseLecturerRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.EnrollmentService;
import com.cource.service.LecturerService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class LecturerServiceImpl implements LecturerService {

    private static final java.util.List<String> ATTENDED_STATUSES = java.util.List.of("PRESENT", "LATE", "EXCUSED");
    private static final java.util.Set<String> PASSING_GRADES = java.util.Set.of("A", "B", "C", "D");
    private static final java.util.Set<String> GRADED_FOR_PASS_RATE = java.util.Set.of("A", "B", "C", "D", "F");

    private final CourseLecturerRepository courseLecturerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final com.cource.repository.CourseOfferingRepository courseOfferingRepository;
    private final com.cource.repository.CourseRepository courseRepository;
    private final com.cource.repository.AcademicTermRepository academicTermRepository;
    private final com.cource.service.CourseService courseService;
    private final EnrollmentService enrollmentService;

    public LecturerServiceImpl(CourseLecturerRepository courseLecturerRepository,
            AttendanceRepository attendanceRepository,
            ClassScheduleRepository classScheduleRepository,
            EnrollmentRepository enrollmentRepository,
            com.cource.repository.CourseOfferingRepository courseOfferingRepository,
            com.cource.repository.CourseRepository courseRepository,
            com.cource.repository.AcademicTermRepository academicTermRepository,
            com.cource.service.CourseService courseService,
            EnrollmentService enrollmentService) {
        this.attendanceRepository = attendanceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseLecturerRepository = courseLecturerRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseRepository = courseRepository;
        this.academicTermRepository = academicTermRepository;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
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

        Attendance attendance = attendanceRepository
                .findByEnrollmentScheduleAndDate(enrollment.getId(), schedule.getId(),
                        attendanceRequestDTO.getAttendanceDate())
                .orElseGet(Attendance::new);

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
    public com.cource.entity.Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId) {
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
        if (rows == null) {
            rows = java.util.Collections.emptyList();
        }
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
            if (rows == null) {
                rows = java.util.Collections.emptyList();
            }
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
    public java.util.Map<String, Long> getAttendanceCountsByDateRange(long lecturerId, java.time.LocalDate from,
            java.time.LocalDate to, Long offeringId, String studentStatus) {
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        if (from == null || to == null) {
            return result;
        }

        java.util.List<Long> offeringIds;
        if (offeringId != null) {
            verifyOwnership(offeringId, lecturerId);
            offeringIds = java.util.List.of(offeringId);
        } else {
            offeringIds = courseLecturerRepository.findByLecturerId(lecturerId).stream()
                    .map(cl -> cl.getOffering().getId()).distinct().toList();
        }
        if (offeringIds.isEmpty()) {
            return result;
        }

        var rows = attendanceRepository.countByOfferingIdsBetween(offeringIds, from, to, studentStatus);
        for (Object[] r : rows) {
            java.time.LocalDate date = (java.time.LocalDate) r[0];
            Long count = ((Number) r[1]).longValue();
            result.put(date.toString(), count);
        }
        return result;
    }

    @Override
    public double calculatePassRate(long lecturerId, long offeringId, String studentStatus) {
        verifyOwnership(offeringId, lecturerId);
        var enrollments = enrollmentRepository.findByOfferingId(offeringId).stream()
                .filter(e -> studentStatus == null || (e.getStatus() != null
                        && e.getStatus().equalsIgnoreCase(studentStatus)))
                .toList();
        if (enrollments.isEmpty()) {
            return 0.0;
        }

        long graded = 0;
        long passed = 0;
        for (var e : enrollments) {
            if (e.getGrade() == null || e.getGrade().isBlank()) {
                continue;
            }
            String g = e.getGrade().trim().toUpperCase();
            if (!GRADED_FOR_PASS_RATE.contains(g)) {
                continue;
            }
            graded++;
            if (PASSING_GRADES.contains(g)) {
                passed++;
            }
        }
        if (graded == 0) {
            return 0.0;
        }
        return (passed * 100.0) / graded;
    }

    @Override
    public double calculateAverageAttendance(long lecturerId, java.time.LocalDate from, java.time.LocalDate to,
            Long offeringId, String studentStatus) {
        if (from == null || to == null) {
            return 0.0;
        }
        java.util.List<Long> offeringIds;
        if (offeringId != null) {
            verifyOwnership(offeringId, lecturerId);
            offeringIds = java.util.List.of(offeringId);
        } else {
            offeringIds = courseLecturerRepository.findByLecturerId(lecturerId).stream()
                    .map(cl -> cl.getOffering().getId()).distinct().toList();
        }
        if (offeringIds.isEmpty()) {
            return 0.0;
        }

        long total = 0;
        long attended = 0;
        for (Long offId : offeringIds) {
            total += attendanceRepository.countByOfferingAndDateRange(offId, from, to, studentStatus);
            attended += attendanceRepository.countByOfferingAndDateRangeWithStatuses(offId, from, to, ATTENDED_STATUSES,
                    studentStatus);
        }
        if (total == 0) {
            return 0.0;
        }
        return (attended * 100.0) / total;
    }

    @Override
    public java.util.List<LecturerCourseReportDTO> getCourseReports(long lecturerId, java.time.LocalDate from,
            java.time.LocalDate to, String studentStatus) {
        var offerings = courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(cl -> cl.getOffering()).distinct().toList();

        java.util.List<LecturerCourseReportDTO> out = new java.util.ArrayList<>();
        for (var off : offerings) {
            long studentCount;
            if (studentStatus == null || studentStatus.isBlank()) {
                studentCount = enrollmentRepository.countByOfferingId(off.getId());
            } else {
                studentCount = enrollmentRepository.findByOfferingId(off.getId()).stream()
                        .filter(e -> e.getStatus() != null && e.getStatus().equalsIgnoreCase(studentStatus))
                        .count();
            }
            double avgAttendance = calculateAverageAttendance(lecturerId, from, to, off.getId(), studentStatus);
            double passRate = calculatePassRate(lecturerId, off.getId(), studentStatus);
            var dto = new LecturerCourseReportDTO(off.getId(),
                    off.getCourse() != null ? off.getCourse().getCourseCode() : "",
                    off.getCourse() != null ? off.getCourse().getTitle() : "",
                    off.getTerm() != null ? off.getTerm().getTermName() : "",
                    studentCount,
                    avgAttendance,
                    passRate,
                    off.isActive());
            out.add(dto);
        }
        return out;
    }

    @Override
    public LecturerCourseDetailDTO getDetailedCourseReport(long lecturerId, long offeringId, java.time.LocalDate from,
            java.time.LocalDate to, String studentStatus) {
        verifyOwnership(offeringId, lecturerId);

        var offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

        long studentCount;
        if (studentStatus == null || studentStatus.isBlank()) {
            studentCount = enrollmentRepository.countByOfferingId(offeringId);
        } else {
            studentCount = enrollmentRepository.findByOfferingId(offeringId).stream()
                    .filter(e -> e.getStatus() != null && e.getStatus().equalsIgnoreCase(studentStatus))
                    .count();
        }
        double avgAttendance = calculateAverageAttendance(lecturerId, from, to, offeringId, studentStatus);
        double passRate = calculatePassRate(lecturerId, offeringId, studentStatus);

        var summary = new LecturerCourseReportDTO(offeringId,
                offering.getCourse() != null ? offering.getCourse().getCourseCode() : "",
                offering.getCourse() != null ? offering.getCourse().getTitle() : "",
                offering.getTerm() != null ? offering.getTerm().getTermName() : "",
                studentCount,
                avgAttendance,
                passRate,
                offering.isActive());

        java.util.Map<String, Long> gradeDist = new java.util.LinkedHashMap<>();
        for (String g : java.util.List.of("A", "B", "C", "D", "F", "W", "I")) {
            gradeDist.put(g, 0L);
        }
        var gradeRows = enrollmentRepository.countGradesByOffering(offeringId, studentStatus);
        for (Object[] r : gradeRows) {
            String g = r[0] != null ? r[0].toString().toUpperCase() : "";
            long c = ((Number) r[1]).longValue();
            gradeDist.put(g, c);
        }

        var enrollments = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, studentStatus);
        java.util.List<java.util.Map<String, Object>> studentGrades = new java.util.ArrayList<>();
        for (var e : enrollments) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            if (e.getStudent() != null) {
                row.put("studentId", e.getStudent().getId());
                row.put("fullName", e.getStudent().getFullName());
                row.put("email", e.getStudent().getEmail());
            }
            row.put("status", e.getStatus());
            row.put("grade", e.getGrade());
            studentGrades.add(row);
        }

        return new LecturerCourseDetailDTO(summary, gradeDist, studentGrades);
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
    public Enrollment updateEnrollmentGrade(long lecturerId, long enrollmentId, String grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getOffering() == null || enrollment.getOffering().getId() == null) {
            throw new ResourceNotFoundException("Associated offering not found");
        }

        verifyOwnership(enrollment.getOffering().getId(), lecturerId);

        String normalized = null;
        if (grade != null && !grade.isBlank()) {
            normalized = grade.trim().toUpperCase();
        }
        enrollment.setGrade(normalized);
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public java.util.List<com.cource.entity.CourseOffering> getOfferingsByLecturerId(long lecturerId) {
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
    public com.cource.entity.CourseOffering createCourseOffering(long lecturerId,
            com.cource.dto.course.CourseOfferingRequestDTO dto) {
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

        com.cource.entity.CourseOffering offering = new com.cource.entity.CourseOffering();
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
        com.cource.entity.User lecturer = new com.cource.entity.User();
        lecturer.setId(lecturerId);
        com.cource.entity.CourseLecturer cl = new com.cource.entity.CourseLecturer();
        cl.setOffering(offering);
        cl.setLecturer(lecturer);
        cl.setPrimary(true);
        courseLecturerRepository.save(cl);

        return offering;
    }

    @Override
    public com.cource.entity.CourseOffering updateCourseOffering(long lecturerId, long offeringId,
            com.cource.dto.course.CourseOfferingRequestDTO dto) {
        verifyOwnership(offeringId, lecturerId);
        var offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

        Integer oldCapacity = offering.getCapacity();
        boolean oldActive = offering.isActive();
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
        var saved = courseOfferingRepository.save(offering);

        boolean capacityIncreased = dto.getCapacity() != null
                && (oldCapacity == null || dto.getCapacity() > oldCapacity);
        boolean activated = dto.getActive() != null && dto.getActive() && !oldActive;
        if (saved.isActive() && (capacityIncreased || activated)) {
            enrollmentService.processWaitlist(saved.getId());
        }

        return saved;
    }

    @Override
    public com.cource.entity.CourseOffering getOfferingById(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        return courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
    }

    @Override
    public com.cource.entity.CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId) {
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

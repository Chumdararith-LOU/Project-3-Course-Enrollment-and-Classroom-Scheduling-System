package com.cource.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

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
import com.cource.service.LecturerService;

import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
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

    public LecturerServiceImpl(CourseLecturerRepository courseLecturerRepository,
            AttendanceRepository attendanceRepository,
            ClassScheduleRepository classScheduleRepository,
            EnrollmentRepository enrollmentRepository,
            com.cource.repository.CourseOfferingRepository courseOfferingRepository,
            com.cource.repository.CourseRepository courseRepository,
            com.cource.repository.AcademicTermRepository academicTermRepository,
            com.cource.service.CourseService courseService) {
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
    public List<CourseOffering> getOfferingsByLecturerId(long lecturerId) {
        return courseLecturerRepository.findByLecturerId(lecturerId).stream()
                .map(CourseLecturer::getOffering)
                .collect(Collectors.toList());
    }

    @Override
    public CourseOffering getOfferingById(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        return courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
    }

    @Override
    public CourseOffering createCourseOffering(long lecturerId, CourseOfferingRequestDTO dto) {
        var course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        var term = academicTermRepository.findById(dto.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Term not found"));

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(dto.getCapacity());
        offering.setActive(dto.getActive() != null ? dto.getActive() : true);
        offering.setEnrollmentCode(courseService.generateEnrollmentCode(course.getCourseCode()));

        offering = courseOfferingRepository.save(offering);

        CourseLecturer cl = new CourseLecturer();
        cl.setOffering(offering);
        User l = new User(); l.setId(lecturerId);
        cl.setLecturer(l);
        cl.setPrimary(true);
        courseLecturerRepository.save(cl);

        return offering;
    }

    @Override
    public CourseOffering updateCourseOffering(long lecturerId, long offeringId, CourseOfferingRequestDTO dto) {
        verifyOwnership(offeringId, lecturerId);
        var offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        if (dto.getCapacity() != null) offering.setCapacity(dto.getCapacity());
        if (dto.getActive() != null) offering.setActive(dto.getActive());
        return courseOfferingRepository.save(offering);
    }

    @Override
    public void deleteCourseOffering(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        courseOfferingRepository.deleteById(offeringId);
    }

    @Override
    public String regenerateOfferingCode(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
        String newCode = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        offering.setEnrollmentCode(newCode);
        offering.setEnrollmentCodeExpiresAt(LocalDateTime.now().plusDays(7));
        courseOfferingRepository.save(offering);
        return newCode;
    }

    @Override
    public CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId) {
        verifyOwnership(offeringId, lecturerId);
        regenerateOfferingCode(offeringId);
        return courseOfferingRepository.findById(offeringId).orElseThrow();
    }

    @Override
    public List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        return classScheduleRepository.findByOfferingIdAndLecturerId(offeringId, lecturerId);
    }

    @Override
    public List<Student> getEnrolledStudents(long offeringId, long lecturerId) {
        verifyOwnership(offeringId, lecturerId);
        return enrollmentRepository.findByOfferingId(offeringId).stream()
                .filter(e -> e.getStatus() == null || "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
    }

    @Override
    public void recordAttendance(AttendanceRequestDTO dto, long studentId, String status) {
        if (dto.getLecturerId() == null) throw new IllegalArgumentException("Lecturer ID required");
        recordAttendance(dto, studentId, status, dto.getLecturerId());
    }

    @Override
    public void recordAttendance(AttendanceRequestDTO request, long studentId, String status, long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        verifyOwnership(schedule.getOffering().getId(), lecturerId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(
                        studentId, schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (attendanceRepository.existsByStudentIdAndScheduleId(studentId, schedule.getId(), enrollment.getId(), request.getAttendanceDate())) {
            throw new ConflictException("Attendance already recorded");
        }

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(status);

        User lecturer = new User();
        lecturer.setId(lecturerId);
        attendance.setRecordedBy(lecturer);

        if (request.getNotes() != null) attendance.setNotes(request.getNotes());
        attendanceRepository.save(attendance);
    }

    @Override
    public com.cource.entity.Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        if (lecturerId != null) {
            verifyOwnership(attendance.getSchedule().getOffering().getId(), lecturerId);
        }
        if (dto.getStatus() != null) attendance.setStatus(dto.getStatus());
        if (dto.getAttendanceDate() != null) attendance.setAttendanceDate(dto.getAttendanceDate());
        if (dto.getNotes() != null) attendance.setNotes(dto.getNotes());
        return attendanceRepository.save(attendance);
    }

    @Override
    public void deleteAttendance(long attendanceId, Long lecturerId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        if (lecturerId != null) verifyOwnership(attendance.getSchedule().getOffering().getId(), lecturerId);
        attendanceRepository.delete(attendance);
    }

    @Override
    public List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (lecturerId != null) verifyOwnership(schedule.getOffering().getId(), lecturerId);
        return attendanceRepository.findByScheduleId(scheduleId);
    }

    @Override
    public List<Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId) {
        List<Attendance> records = getAttendanceRecords(scheduleId, lecturerId);
        return records.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("studentName", a.getEnrollment().getStudent().getFullName());
            map.put("studentId", a.getEnrollment().getStudent().getStudentId());
            map.put("date", a.getAttendanceDate());
            map.put("status", a.getStatus());
            map.put("notes", a.getNotes());
            return map;
        }).collect(Collectors.toList());
    }


    @Override
    public LecturerDashboardDTO getDashboardStats(Long lecturerId) {
        long activeCourses = courseLecturerRepository.findByLecturerId(lecturerId).size();
        return LecturerDashboardDTO.builder().activeCourses(activeCourses).build();
    }

    @Override
    public Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Long> offeringIds = getOfferingsByLecturerId(lecturerId).stream()
                .map(CourseOffering::getId).collect(Collectors.toList());

        if (offeringIds.isEmpty()) return Collections.emptyMap();

        List<Object[]> results = attendanceRepository.countByOfferingIdsSince(offeringIds, startDate);
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
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

        enrollment.setGrade(grade);
        if ("F".equalsIgnoreCase(grade)) {
            enrollment.setStatus("FAILED");
        } else {
            enrollment.setStatus("COMPLETED");
        }

        enrollmentRepository.save(enrollment);
    }

    private void verifyOwnership(long offeringId, long lecturerId) {
        if (!courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId)) {
            throw new SecurityException("Access denied");
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

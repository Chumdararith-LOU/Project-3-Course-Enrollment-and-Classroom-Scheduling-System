package com.cource.service.impl;

import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.AdminService;
import com.cource.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final com.cource.service.CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final RoomRepository roomRepository;
    private final AcademicTermRepository academicTermRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final EnrollmentService enrollmentService;

    @Override
    public List<User> getLecturersForOffering(Long offeringId) {
        CourseOffering offering = getOfferingById(offeringId);
        return offering.getLecturers().stream()
                .map(cl -> cl.getLecturer())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignLecturersToOffering(Long offeringId, List<Long> lecturerIds) {
        CourseOffering offering = getOfferingById(offeringId);
        // Remove existing assignments
        List<CourseLecturer> current = courseLecturerRepository.findAll().stream()
                .filter(cl -> cl.getOffering().getId().equals(offeringId))
                .collect(Collectors.toList());
        for (CourseLecturer cl : current) {
            courseLecturerRepository.delete(cl);
        }
        // Add new assignments (unique only, and check for existing)
        List<Long> uniqueLecturerIds = lecturerIds == null ? List.of() : lecturerIds.stream().distinct().toList();
        for (Long lecturerId : uniqueLecturerIds) {
            if (courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, lecturerId)) {
                continue; // Defensive: skip if already exists
            }
            User lecturer = userRepository.findById(lecturerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found with id: " + lecturerId));
            CourseLecturer cl = new CourseLecturer();
            cl.setOffering(offering);
            cl.setLecturer(lecturer);
            cl.setPrimary(false); // Admin assigns, not primary by default
            courseLecturerRepository.save(cl);
        }
    }

    @Override
    @Transactional
    public void removeLecturerFromOffering(Long offeringId, Long lecturerId) {
        List<CourseLecturer> matches = courseLecturerRepository.findAll().stream()
                .filter(cl -> cl.getOffering().getId().equals(offeringId)
                        && cl.getLecturer().getId().equals(lecturerId))
                .collect(Collectors.toList());
        for (CourseLecturer cl : matches) {
            courseLecturerRepository.delete(cl);
        }
    }

    @Override
    @Transactional
    public int bulkAssignLecturerToAllOfferings(Long lecturerId) {
        var lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found with id: " + lecturerId));
        List<CourseOffering> offerings = courseOfferingRepository.findAll();
        int created = 0;
        for (CourseOffering off : offerings) {
            boolean exists = courseLecturerRepository.existsByOfferingIdAndLecturerId(off.getId(), lecturerId);
            if (exists)
                continue;
            CourseLecturer cl = new CourseLecturer();
            cl.setOffering(off);
            cl.setLecturer(lecturer);
            cl.setPrimary(false);
            courseLecturerRepository.save(cl);
            created++;
        }
        log.info("[ADMIN] bulkAssignLecturerToAllOfferings created={} for lecturerId={}", created, lecturerId);
        return created;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getUsersByRole(String roleCode) {
        return userRepository.findByRole_RoleCode(roleCode);
    }

    @Override
    public long getTotalStudents() {
        return userRepository.countByRole_RoleName("Student");
    }

    @Override
    public long getTotalLecturers() {
        return userRepository.countByRole_RoleName("Lecturer");
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public long getTotalCourses() {
        return courseRepository.count();
    }

    @Override
    public List<CourseOffering> getAllCourseOfferings() {
        return courseOfferingRepository.findAll();
    }

    @Override
    public List<CourseOffering> getCourseOfferingsByTerm(Long termId) {
        return courseOfferingRepository.findByTermId(termId);
    }

    @Override
    public CourseOffering getOfferingById(Long id) {
        return courseOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + id));
    }

    @Override
    @Transactional
    public CourseOffering createOffering(Long courseId, Long termId, Integer capacity, Boolean isActive) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (!course.isActive()) {
            throw new IllegalArgumentException("Cannot create offering for inactive course: " + course.getCourseCode());
        }
        AcademicTerm term = academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found with id: " + termId));

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(capacity);
        offering.setActive(isActive != null ? isActive : true);
        // Ensure enrollment code is set (generate from course code)
        offering.setEnrollmentCode(courseService.generateEnrollmentCode(course.getCourseCode()));

        return courseOfferingRepository.save(offering);
    }

    @Override
    @Transactional
    public CourseOffering updateOffering(Long id, Long courseId, Long termId, Integer capacity, Boolean isActive) {
        CourseOffering offering = getOfferingById(id);

        Integer oldCapacity = offering.getCapacity();
        boolean oldActive = offering.isActive();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (!course.isActive()) {
            throw new IllegalArgumentException("Cannot use inactive course for offering: " + course.getCourseCode());
        }
        AcademicTerm term = academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found with id: " + termId));

        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(capacity);
        if (isActive != null) {
            offering.setActive(isActive);
        }

        CourseOffering saved = courseOfferingRepository.save(offering);

        boolean capacityIncreased = capacity != null && (oldCapacity == null || capacity > oldCapacity);
        boolean activated = isActive != null && isActive && !oldActive;
        if (saved.isActive() && (capacityIncreased || activated)) {
            enrollmentService.processWaitlist(saved.getId());
        }

        return saved;
    }

    @Override
    @Transactional
    public CourseOffering regenerateOfferingEnrollmentCode(Long offeringId) {
        CourseOffering offering = getOfferingById(offeringId);
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
    @Transactional
    public void deleteOffering(Long id) {
        CourseOffering offering = getOfferingById(id);
        courseOfferingRepository.delete(offering);
    }

    @Override
    @Transactional
    public CourseOffering toggleOfferingStatus(Long id) {
        CourseOffering offering = getOfferingById(id);
        boolean oldActive = offering.isActive();
        offering.setActive(!oldActive);
        CourseOffering saved = courseOfferingRepository.save(offering);
        if (!oldActive && saved.isActive()) {
            enrollmentService.processWaitlist(saved.getId());
        }
        return saved;
    }

    @Override
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Override
    public List<Enrollment> getEnrollmentsByOffering(Long offeringId) {
        return enrollmentRepository.findByOfferingId(offeringId);
    }

    @Override
    public long getTotalEnrollments() {
        return enrollmentRepository.count();
    }

    @Override
    public Enrollment getEnrollmentById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
    }

    @Override
    @Transactional
    public Enrollment createEnrollment(Long studentId, Long offeringId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + offeringId));

        // Check if student is already enrolled
        if (enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId).isPresent()) {
            throw new ConflictException("Student is already enrolled in this offering");
        }

        // Check capacity
        long enrolledCount = enrollmentRepository.countByOfferingId(offeringId);
        if (enrolledCount >= offering.getCapacity()) {
            throw new ConflictException("Offering is at full capacity");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setOffering(offering);
        enrollment.setStatus("ENROLLED");
        enrollment.setEnrolledAt(java.time.LocalDateTime.now());

        return enrollmentRepository.save(enrollment);
    }

    private static final java.util.Set<String> VALID_GRADES = java.util.Set.of(
            "A", "A+", "A-", "B", "B+", "B-", "C", "C+", "C-", "D", "D+", "D-", "F", "W", "I");

    @Override
    @Transactional
    public Enrollment updateEnrollmentGrade(Long id, String grade) {
        if (grade != null && !grade.isBlank() && !VALID_GRADES.contains(grade.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid grade: " + grade
                            + ". Valid grades are: A, A+, A-, B, B+, B-, C, C+, C-, D, D+, D-, F, W, I");
        }
        Enrollment enrollment = getEnrollmentById(id);
        enrollment.setGrade(grade != null ? grade.toUpperCase() : null);
        return enrollmentRepository.save(enrollment);
    }

    private static final java.util.Set<String> VALID_STATUSES = java.util.Set.of(
            "ENROLLED", "DROPPED", "COMPLETED", "FAILED", "WAITLISTED");

    @Override
    @Transactional
    public Enrollment updateEnrollmentStatus(Long id, String status) {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid status: " + status
                            + ". Valid statuses are: ENROLLED, DROPPED, COMPLETED, FAILED, WAITLISTED");
        }
        Enrollment enrollment = getEnrollmentById(id);
        String upperStatus = status != null ? status.toUpperCase() : null;
        enrollment.setStatus(upperStatus);
        // Auto-set grade to "W" (Withdrawal) when status is changed to DROPPED
        if ("DROPPED".equals(upperStatus)) {
            enrollment.setGrade("W");
        }
        return enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void deleteEnrollment(Long id) {
        Enrollment enrollment = getEnrollmentById(id);
        enrollmentRepository.delete(enrollment);
    }

    @Override
    public List<ClassSchedule> getAllSchedules() {
        return classScheduleRepository.findAll();
    }

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public List<AcademicTerm> getAllTerms() {
        return academicTermRepository.findAll();
    }

    @Override
    public AcademicTerm getTermById(Long id) {
        return academicTermRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Term not found with id: " + id));
    }

    @Override
    public String generateTermCode(java.time.LocalDate startDate) {
        if (startDate == null) {
            startDate = java.time.LocalDate.now();
        }
        int year = startDate.getYear();
        int month = startDate.getMonthValue();
        String season;
        if (month >= 1 && month <= 5) {
            season = "SPRING";
        } else if (month >= 6 && month <= 8) {
            season = "SUMMER";
        } else {
            season = "FALL";
        }
        String prefix = season + year;

        // Find existing codes with this prefix
        java.util.List<String> existingCodes = academicTermRepository.findTermCodesByPrefix(prefix);

        // Find the next available number
        int nextNum = 1;
        for (String code : existingCodes) {
            if (code.startsWith(prefix + "-")) {
                try {
                    int num = Integer.parseInt(code.substring(prefix.length() + 1));
                    if (num >= nextNum) {
                        nextNum = num + 1;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return prefix + "-" + nextNum;
    }

    @Override
    @Transactional
    public AcademicTerm createTerm(String termCode, String termName, java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
        // Validate dates
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        // Term cannot start in the past (before today)
        if (startDate.isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Term start date cannot be in the past");
        }

        // ensure termCode is unique
        if (academicTermRepository.findByTermCode(termCode).isPresent()) {
            throw new com.cource.exception.ConflictException("Term code already exists: " + termCode);
        }

        AcademicTerm term = new AcademicTerm();
        term.setTermCode(termCode);
        term.setTermName(termName);
        term.setStartDate(startDate);
        term.setEndDate(endDate);
        term.setActive(true);
        return academicTermRepository.save(term);
    }

    @Override
    @Transactional
    public AcademicTerm updateTerm(Long id, String termCode, String termName, java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
        AcademicTerm term = getTermById(id);
        // if changing code, ensure uniqueness
        if (!term.getTermCode().equals(termCode)) {
            if (academicTermRepository.findByTermCode(termCode).isPresent()) {
                throw new com.cource.exception.ConflictException("Term code already exists: " + termCode);
            }
            term.setTermCode(termCode);
        }
        term.setTermName(termName);
        term.setStartDate(startDate);
        term.setEndDate(endDate);
        return academicTermRepository.save(term);
    }

    @Override
    @Transactional
    public void deleteTerm(Long id) {
        AcademicTerm term = getTermById(id);
        academicTermRepository.delete(term);
    }

    @Override
    @Transactional
    public AcademicTerm toggleTermStatus(Long id) {
        AcademicTerm term = getTermById(id);
        term.setActive(!term.isActive());
        return academicTermRepository.save(term);
    }

    // Room CRUD Implementation
    @Override
    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    @Override
    @Transactional
    public Room createRoom(String roomNumber, String building, Integer capacity, String roomType, Boolean isActive) {
        // Check for duplicate room number
        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new ConflictException("Room number '" + roomNumber + "' already exists");
        }
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setBuilding(building);
        room.setCapacity(capacity);
        room.setRoomType(roomType);
        room.setActive(isActive != null ? isActive : true);
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room updateRoom(Long id, String roomNumber, String building, Integer capacity, String roomType,
            Boolean isActive) {
        Room room = getRoomById(id);
        // Check for duplicate room number (exclude current room)
        if (roomRepository.existsByRoomNumberAndIdNot(roomNumber, id)) {
            throw new ConflictException("Room number '" + roomNumber + "' already exists");
        }
        room.setRoomNumber(roomNumber);
        room.setBuilding(building);
        room.setCapacity(capacity);
        room.setRoomType(roomType);
        if (isActive != null) {
            room.setActive(isActive);
        }
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }

    @Override
    @Transactional
    public Room toggleRoomStatus(Long id) {
        Room room = getRoomById(id);
        room.setActive(!room.isActive());
        return roomRepository.save(room);
    }

    // Schedule CRUD Implementation
    @Override
    public ClassSchedule getScheduleById(Long id) {
        return classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }

    @Override
    @Transactional
    public ClassSchedule createSchedule(Long offeringId, Long roomId, String dayOfWeek, java.time.LocalTime startTime,
            java.time.LocalTime endTime) {
        CourseOffering offering = getOfferingById(offeringId);
        Room room = getRoomById(roomId);

        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }

        if (classScheduleRepository.existsOverlap(roomId, dayOfWeek.toUpperCase(), startTime, endTime)) {
            List<ClassSchedule> conflicts = classScheduleRepository.findConflictingSchedules(
                    roomId, dayOfWeek.toUpperCase(), startTime, endTime, null);
            if (!conflicts.isEmpty()) {
                ClassSchedule conflict = conflicts.get(0);
                throw new ConflictException(String.format(
                        "Room %s is already booked on %s from %s to %s. Your requested time %s - %s overlaps with this schedule.",
                        room.getRoomNumber(), dayOfWeek,
                        conflict.getStartTime(), conflict.getEndTime(),
                        startTime, endTime));
            }
            throw new ConflictException(String.format(
                    "Room %s is already booked on %s during the requested time slot %s - %s",
                    room.getRoomNumber(), dayOfWeek, startTime, endTime));
        }

        ClassSchedule schedule = new ClassSchedule();
        schedule.setOffering(offering);
        schedule.setRoom(room);
        schedule.setDayOfWeek(dayOfWeek.toUpperCase());
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        return classScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public ClassSchedule updateSchedule(Long id, Long offeringId, Long roomId, String dayOfWeek,
            java.time.LocalTime startTime, java.time.LocalTime endTime) {
        ClassSchedule schedule = getScheduleById(id);
        CourseOffering offering = getOfferingById(offeringId);
        Room room = getRoomById(roomId);

        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }

        if (classScheduleRepository.existsOverlapWithId(roomId, dayOfWeek.toUpperCase(), startTime, endTime, id)) {
            List<ClassSchedule> conflicts = classScheduleRepository.findConflictingSchedules(
                    roomId, dayOfWeek.toUpperCase(), startTime, endTime, id);
            if (!conflicts.isEmpty()) {
                ClassSchedule conflict = conflicts.get(0);
                throw new ConflictException(String.format(
                        "Room %s is already booked on %s from %s to %s. Your requested time %s - %s overlaps with this schedule.",
                        room.getRoomNumber(), dayOfWeek,
                        conflict.getStartTime(), conflict.getEndTime(),
                        startTime, endTime));
            }
            throw new ConflictException(String.format(
                    "Room %s is already booked on %s during the requested time slot %s - %s",
                    room.getRoomNumber(), dayOfWeek, startTime, endTime));
        }

        schedule.setOffering(offering);
        schedule.setRoom(room);
        schedule.setDayOfWeek(dayOfWeek.toUpperCase());
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        return classScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        ClassSchedule schedule = getScheduleById(id);
        classScheduleRepository.delete(schedule);
    }

    @Override
    public List<ClassSchedule> getSchedulesByOffering(Long offeringId) {
        return classScheduleRepository.findByOfferingId(offeringId);
    }

    @Override
    public List<ClassSchedule> getSchedulesByRoom(Long roomId) {
        return classScheduleRepository.findByRoomId(roomId);
    }

    @Override
    public Map<String, Object> getEnrollmentStatsByTerm() {
        Map<String, Object> stats = new HashMap<>();
        List<AcademicTerm> terms = academicTermRepository.findAll();
        for (AcademicTerm term : terms) {
            long count = enrollmentRepository.countByOffering_Term_Id(term.getId());
            stats.put(term.getTermName(), count);
        }
        return stats;
    }

    @Override
    public Map<String, Object> getCoursePopularity() {
        Map<String, Object> popularity = new HashMap<>();
        List<Course> courses = courseRepository.findAll();
        for (Course course : courses) {
            long count = enrollmentRepository.countByOffering_Course_Id(course.getId());
            popularity.put(course.getTitle(), count);
        }
        return popularity;
    }
}

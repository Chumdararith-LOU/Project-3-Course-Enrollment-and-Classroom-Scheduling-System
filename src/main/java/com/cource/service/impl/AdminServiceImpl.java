package com.cource.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.cource.entity.AcademicTerm;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseLecturer;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.Room;
import com.cource.entity.User;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.CourseLecturerRepository;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.CourseRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.repository.RoomRepository;
import com.cource.repository.UserRepository;
import com.cource.service.AdminService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final RoomRepository roomRepository;
    private final AcademicTermRepository academicTermRepository;
    private final CourseLecturerRepository courseLecturerRepository;

    @Override
    public List<User> getLecturersForOffering(Long offeringId) {
        CourseOffering offering = getOfferingById(offeringId);
        return offering.getLecturers().stream()
                .map(CourseLecturer::getLecturer)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void assignLecturersToOffering(Long offeringId, List<Long> lecturerIds) {
        CourseOffering offering = getOfferingById(offeringId);
        // Remove existing assignments
        List<CourseLecturer> current = courseLecturerRepository.findAll().stream()
                .filter(cl -> cl.getOffering().getId().equals(offeringId))
                .collect(Collectors.toList());
        courseLecturerRepository.deleteAll(current);

        // Add new assignments (unique only, and check for existing)
        List<Long> uniqueLecturerIds = lecturerIds == null ? List.of() : lecturerIds.stream().distinct().toList();
        for (Long lecturerId : uniqueLecturerIds) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public void removeLecturerFromOffering(Long offeringId, Long lecturerId) {
        List<CourseLecturer> matches = courseLecturerRepository.findAll().stream()
                .filter(cl -> cl.getOffering().getId().equals(offeringId)
                        && cl.getLecturer().getId().equals(lecturerId))
                .collect(Collectors.toList());
        courseLecturerRepository.deleteAll(matches);
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
    @PreAuthorize("hasRole('ADMIN')")
    public CourseOffering createOffering(Long courseId, Long termId, Integer capacity, Boolean isActive) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        AcademicTerm term = academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found with id: " + termId));

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(capacity);
        offering.setActive(isActive != null ? isActive : true);

        return courseOfferingRepository.save(offering);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CourseOffering updateOffering(Long id, Long courseId, Long termId, Integer capacity, Boolean isActive) {
        CourseOffering offering = getOfferingById(id);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        AcademicTerm term = academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found with id: " + termId));

        offering.setCourse(course);
        offering.setTerm(term);
        offering.setCapacity(capacity);
        if (isActive != null) {
            offering.setActive(isActive);
        }

        return courseOfferingRepository.save(offering);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOffering(Long id) {
        CourseOffering offering = getOfferingById(id);
        courseOfferingRepository.delete(offering);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CourseOffering toggleOfferingStatus(Long id) {
        CourseOffering offering = getOfferingById(id);
        offering.setActive(!offering.isActive());
        return courseOfferingRepository.save(offering);
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
    @PreAuthorize("hasRole('ADMIN')")
    public Enrollment createEnrollment(Long studentId, Long offeringId) {
        User studentUser = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + offeringId));

        if (enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId).isPresent()) {
            throw new ConflictException("Student is already enrolled in this offering");
        }

        long enrolledCount = enrollmentRepository.countByOfferingId(offeringId);
        if (enrolledCount >= offering.getCapacity()) {
            throw new ConflictException("Offering is at full capacity");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setOffering(offering);
        enrollment.setStatus("ENROLLED");
        enrollment.setEnrolledAt(java.time.LocalDateTime.now());

        return enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Enrollment updateEnrollmentGrade(Long id, String grade) {
        Enrollment enrollment = getEnrollmentById(id);
        enrollment.setGrade(grade);
        return enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Enrollment updateEnrollmentStatus(Long id, String status) {
        Enrollment enrollment = getEnrollmentById(id);
        enrollment.setStatus(status);
        return enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEnrollment(Long id) {
        Enrollment enrollment = getEnrollmentById(id);
        enrollmentRepository.delete(enrollment);
    }

    @Override
    public List<ClassSchedule> getAllSchedules() {
        return classScheduleRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    public List<AcademicTerm> getAllTerms() {
        return academicTermRepository.findAll();
    }

    @Override
    public AcademicTerm getTermById(Long id) {
        return academicTermRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Term not found with id: " + id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AcademicTerm createTerm(String termCode, String termName, java.time.LocalDate startDate,
                                   java.time.LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        if (academicTermRepository.findByTermCode(termCode).isPresent()) {
            throw new ConflictException("Term code already exists: " + termCode);
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
    @PreAuthorize("hasRole('ADMIN')")
    public AcademicTerm updateTerm(Long id, String termCode, String termName, java.time.LocalDate startDate,
                                   java.time.LocalDate endDate) {
        AcademicTerm term = getTermById(id);
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        if (!term.getTermCode().equals(termCode)) {
            if (academicTermRepository.findByTermCode(termCode).isPresent()) {
                throw new ConflictException("Term code already exists: " + termCode);
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
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTerm(Long id) {
        AcademicTerm term = getTermById(id);
        academicTermRepository.delete(term);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public Room createRoom(String roomNumber, String building, Integer capacity, String roomType, Boolean isActive) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0.");
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
    @PreAuthorize("hasRole('ADMIN')")
    public Room updateRoom(Long id, String roomNumber, String building, Integer capacity, String roomType,
                           Boolean isActive) {
        Room room = getRoomById(id);

        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0.");
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
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ClassSchedule createSchedule(Long offeringId, Long roomId, String dayOfWeek, java.time.LocalTime startTime,
                                        java.time.LocalTime endTime) {
        CourseOffering offering = getOfferingById(offeringId);
        Room room = getRoomById(roomId);

        ClassSchedule schedule = new ClassSchedule();
        schedule.setOffering(offering);
        schedule.setRoom(room);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        return classScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ClassSchedule updateSchedule(Long id, Long offeringId, Long roomId, String dayOfWeek,
                                        java.time.LocalTime startTime, java.time.LocalTime endTime) {
        ClassSchedule schedule = getScheduleById(id);
        CourseOffering offering = getOfferingById(offeringId);
        Room room = getRoomById(roomId);

        schedule.setOffering(offering);
        schedule.setRoom(room);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        return classScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
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
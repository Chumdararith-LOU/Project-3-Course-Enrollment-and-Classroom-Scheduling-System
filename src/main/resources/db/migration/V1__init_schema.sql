-- 1. USERS & PROFILES
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    id_card VARCHAR(50) UNIQUE COMMENT 'Student/Lecturer ID',
    is_active BOOLEAN DEFAULT TRUE,
    role ENUM('STUDENT', 'LECTURER', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_active_role (is_active, role),
    INDEX idx_users_id_card (id_card)
);

CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone VARCHAR(20),
    date_of_birth DATE,
    department VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(255),
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_profiles_department (department)
);

-- 2. ACADEMIC TERMS
CREATE TABLE academic_terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_name VARCHAR(100) NOT NULL,
    term_type ENUM('SEMESTER', 'QUARTER', 'TRIMESTER') NOT NULL DEFAULT 'SEMESTER',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_terms_active (is_active),
    INDEX idx_terms_dates (start_date, end_date)
);

-- 3. COURSES
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    capacity INT NOT NULL DEFAULT 30,
    credits INT DEFAULT 3,
    lecturer_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lecturer_id) REFERENCES users(id),
    FOREIGN KEY (term_id) REFERENCES academic_terms(id),

    INDEX idx_courses_code (course_code),
    INDEX idx_courses_lecturer (lecturer_id),
    INDEX idx_courses_term_active (term_id, is_active)
);

-- 4. ENROLLMENT & WAITLIST
CREATE TABLE enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ENROLLED', 'DROPPED', 'COMPLETED', 'FAILED', 'WAITLISTED') DEFAULT 'ENROLLED',
    grade ENUM('A', 'B', 'C', 'D', 'F', 'W', 'I') NULL COMMENT 'I = Incomplete',
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),

    UNIQUE KEY uk_enrollment (student_id, course_id),

    INDEX idx_enrollments_status (status),
    INDEX idx_enrollments_student_course (student_id, course_id),
    INDEX idx_enrollments_course_status (course_id, status)
);

CREATE TABLE waitlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    position INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMP NULL,
    status ENUM('PENDING', 'NOTIFIED', 'EXPIRED') DEFAULT 'PENDING',
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    UNIQUE KEY uk_waitlist (student_id, course_id),

    INDEX idx_waitlist_course_position (course_id, position),
    INDEX idx_waitlist_status (status)
);

-- 5. ROOMS & SCHEDULING
CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    building VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    facilities TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    room_type ENUM('LECTURE_HALL', 'LAB', 'SEMINAR', 'OFFICE', 'OTHER') DEFAULT 'LECTURE_HALL',

    INDEX idx_rooms_building_number (building, room_number),
    INDEX idx_rooms_type_capacity (room_type, capacity)
);

CREATE TABLE class_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL COMMENT 'MON, TUE, WED...',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    UNIQUE KEY uk_schedule_room_time (room_id, day_of_week, start_time, end_time)
);

-- 6. ATTENDANCE
CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT', 'EXCUSED', 'LATE', 'LEFT_EARLY') NOT NULL DEFAULT 'PRESENT',
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recorded_by BIGINT, -- Lecturer who marked it
    notes TEXT,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    FOREIGN KEY (schedule_id) REFERENCES class_schedules(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id),
    UNIQUE KEY uk_attendance (enrollment_id, schedule_id, attendance_date)
);

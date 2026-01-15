-- 1. ROLES
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(20) NOT NULL UNIQUE COMMENT 'STUDENT, LECTURER, ADMIN',
    role_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code)
);

-- 2. USERS
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    id_card VARCHAR(50) UNIQUE COMMENT 'Student/Lecturer ID',
    is_active BOOLEAN DEFAULT TRUE,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 3. USER PROFILES (Department Removed)
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone VARCHAR(20),
    date_of_birth DATE,
    bio TEXT,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_profiles_user (user_id)
);

-- 4. ACADEMIC TERMS
CREATE TABLE academic_terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_code VARCHAR(20) NOT NULL UNIQUE,
    term_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. COURSES
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    credits INT NOT NULL DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 6. COURSE OFFERINGS (No Section)
CREATE TABLE course_offerings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    capacity INT NOT NULL DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (term_id) REFERENCES academic_terms(id),
    UNIQUE KEY uk_offering (course_id, term_id)
);

-- 7. COURSE LECTURERS
CREATE TABLE course_lecturers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (offering_id) REFERENCES course_offerings(id) ON DELETE CASCADE,
    FOREIGN KEY (lecturer_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_course_lecturer (offering_id, lecturer_id)
);

-- 8. ENROLLMENTS
CREATE TABLE enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    offering_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ENROLLED',
    grade VARCHAR(5) NULL,
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (offering_id) REFERENCES course_offerings(id),
    UNIQUE KEY uk_enrollment (student_id, offering_id)
);

-- 9. WAITLIST
CREATE TABLE waitlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    offering_id BIGINT NOT NULL,
    position INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (offering_id) REFERENCES course_offerings(id),
    UNIQUE KEY uk_waitlist (student_id, offering_id)
);

-- 10. ROOMS (Updated: room_type varchar, no facilities)
CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    building VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    room_type VARCHAR(50), -- 'LECTURE_HALL, LAB, SEMINAR, OTHER'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 11. CLASS SCHEDULES
CREATE TABLE class_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (offering_id) REFERENCES course_offerings(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    UNIQUE KEY uk_schedule (room_id, day_of_week, start_time, end_time)
);

-- 12. ATTENDANCE
CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PRESENT',
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recorded_by BIGINT,
    notes TEXT,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    FOREIGN KEY (schedule_id) REFERENCES class_schedules(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id),
    UNIQUE KEY uk_attendance (enrollment_id, schedule_id, attendance_date)
);
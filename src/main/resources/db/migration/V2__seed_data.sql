-- 1. INSERT ROLES
INSERT INTO roles (role_code, role_name) VALUES
('ADMIN', 'Administrator'),
('LECTURER', 'Lecturer'),
('STUDENT', 'Student');

-- 2. INSERT USERS
INSERT INTO users (email, password_hash, first_name, last_name, id_card, role_id, is_active) VALUES
('admin@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Lou', 'Chumdararith', 'ADM001', (SELECT id FROM roles WHERE role_code = 'ADMIN'), TRUE),
('lecturer@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Heng', 'Nguonhour', 'LEC001', (SELECT id FROM roles WHERE role_code = 'LECTURER'), TRUE),
('student01@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Din', 'Rasy', 'STU001', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE),
('student02@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Cheang', 'Seyha', 'STU002', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE),
('student03@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Chork', 'Ratanakdevid', 'STU003', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE);

-- 3. INSERT ACADEMIC TERM
INSERT INTO academic_terms (term_code, term_name, start_date, end_date, is_active)
VALUES ('SP2025', 'Semester I 2025', '2025-12-15', '2026-05-30', TRUE);

-- 4. INSERT COURSES
INSERT INTO courses (course_code, title, description, credits) VALUES
('GIC25DB', 'Intro to Database', 'Basic concepts of Database and MySQL', 3),
('GIC25SE', 'Software Engineering', 'Spring Boot and UML', 3);

-- 5. INSERT COURSE OFFERING
INSERT INTO course_offerings (course_id, term_id, capacity) VALUES
(
    (SELECT id FROM courses WHERE course_code = 'GIC25DB'),
    (SELECT id FROM academic_terms WHERE term_code = 'SP2025'),
    30
);

-- 6. ASSIGN LECTURER TO OFFERING
INSERT INTO course_lecturers (offering_id, lecturer_id, is_primary) VALUES
(
    (SELECT co.id FROM course_offerings co
     JOIN courses c ON co.course_id = c.id
     WHERE c.course_code = 'GIC25DB'),
    (SELECT id FROM users WHERE email = 'lecturer@test.com'),
    TRUE
);

-- 7. INSERT ROOMS
INSERT INTO rooms (room_number, building, capacity, room_type) VALUES
('F106', 'Building F', 30, 'LAB'),
('J702', 'Building J', 100, 'LECTURE_HALL');

-- 8. USER PROFILE (No department)
INSERT INTO user_profiles (user_id, bio)
SELECT id, 'Senior Lecturer'
FROM users WHERE email = 'lecturer@test.com';
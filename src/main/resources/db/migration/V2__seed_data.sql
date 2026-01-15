-- 1. INSERT ROLES
INSERT INTO roles (role_code, role_name) VALUES
('ADMIN', 'Administrator'),
('LECTURER', 'Lecturer'),
('STUDENT', 'Student');

-- 2. INSERT USERS
-- Password: 'password' → BCrypt hash
SET @pwd = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

INSERT INTO users (email, password_hash, first_name, last_name, id_card, role_id, is_active) VALUES
('admin@test.com', @pwd, 'Lou', 'Chumdararith', 'ADM001', (SELECT id FROM roles WHERE role_code = 'ADMIN'), TRUE),
('lecturer01@test.com', @pwd, 'Heng', 'Nguonhour', 'LEC001', (SELECT id FROM roles WHERE role_code = 'LECTURER'), TRUE),
('lecturer02@test.com', @pwd, 'Sarah', 'Janes', 'LEC002', (SELECT id FROM roles WHERE role_code = 'LECTURER'), TRUE),
('student01@test.com', @pwd, 'Din', 'Rasy', 'STU001', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE),
('student02@test.com', @pwd, 'Cheang', 'Seyha', 'STU002', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE),
('student03@test.com', @pwd, 'Chork', 'Ratanakdevid', 'STU003', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE),
('student04@test.com', @pwd, 'Bob', 'Gray', 'STU004', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE),
('student05@test.com', @pwd, 'Penny', 'Wise', 'STU005', (SELECT id FROM roles WHERE role_code = 'STUDENT'), TRUE);

-- 3. USER PROFILES
INSERT INTO user_profiles (user_id, bio, phone) VALUES
((SELECT id FROM users WHERE email = 'lecturer01@test.com'), 'Expert in Distributed Systems', '012-333-444'),
((SELECT id FROM users WHERE email = 'lecturer02@test.com'), 'Frontend Specialist', '010-555-666');

-- 4. ACADEMIC TERMS
INSERT INTO academic_terms (term_code, term_name, start_date, end_date, is_active) VALUES
('FA2024', 'Fall 2024', '2024-09-01', '2025-01-30', FALSE),
('SP2025', 'Spring 2025', '2025-02-15', '2025-06-30', TRUE);

-- 5. ROOMS
INSERT INTO rooms (room_number, building, capacity, room_type) VALUES
('F106', 'Building F', 30, 'LAB'),
('J702', 'Building J', 100, 'LECTURE_HALL'),
('I610', 'Building I', 10, 'LAB');

-- 6. COURSES
INSERT INTO courses (course_code, title, description, credits) VALUES
('GIC25DB', 'Intro to Database', 'MySQL and Database Design', 3),
('GIC25SE', 'Software Engineering', 'Spring Boot, UML, and Agile', 3),
('GIC25WEB', 'Web Development', 'HTML, CSS, JS, and React', 3);

-- 7. COURSE OFFERINGS (with lecturer_id and enrollment_code)
-- Generate deterministic enrollment codes: e.g., 'db1234-1'
INSERT INTO course_offerings (course_id, term_id, lecturer_id, capacity, enrollment_code, enrollment_code_expires_at) VALUES
(
  (SELECT id FROM courses WHERE course_code = 'GIC25DB'),
  (SELECT id FROM academic_terms WHERE term_code = 'SP2025'),
  (SELECT id FROM users WHERE email = 'lecturer01@test.com'),
  30,
  'db1234-1',
  DATE_ADD(NOW(), INTERVAL 7 DAY)
),
(
  (SELECT id FROM courses WHERE course_code = 'GIC25SE'),
  (SELECT id FROM academic_terms WHERE term_code = 'SP2025'),
  (SELECT id FROM users WHERE email = 'lecturer01@test.com'),
  30,
  'se5678-2',
  DATE_ADD(NOW(), INTERVAL 7 DAY)
),
(
  (SELECT id FROM courses WHERE course_code = 'GIC25WEB'),
  (SELECT id FROM academic_terms WHERE term_code = 'SP2025'),
  (SELECT id FROM users WHERE email = 'lecturer02@test.com'),
  2,
  'web90ab-3',
  DATE_ADD(NOW(), INTERVAL 7 DAY)
);

-- 8. CLASS SCHEDULES
INSERT INTO class_schedules (offering_id, room_id, day_of_week, start_time, end_time) VALUES
(1, (SELECT id FROM rooms WHERE room_number = 'F106'), 'MON', '07:00:00', '09:00:00'),
(2, (SELECT id FROM rooms WHERE room_number = 'J702'), 'TUE', '13:00:00', '15:00:00'),
(3, (SELECT id FROM rooms WHERE room_number = 'I610'), 'FRI', '13:00:00', '17:00:00');

-- 9. ENROLLMENTS
INSERT INTO enrollments (student_id, offering_id, status) VALUES
((SELECT id FROM users WHERE email = 'student01@test.com'), 1, 'ENROLLED'),
((SELECT id FROM users WHERE email = 'student02@test.com'), 1, 'ENROLLED'),
((SELECT id FROM users WHERE email = 'student04@test.com'), 3, 'ENROLLED'),
((SELECT id FROM users WHERE email = 'student05@test.com'), 3, 'ENROLLED');

-- 10. WAITLIST
INSERT INTO waitlist (student_id, offering_id, position, status) VALUES
((SELECT id FROM users WHERE email = 'student03@test.com'), 3, 1, 'PENDING');
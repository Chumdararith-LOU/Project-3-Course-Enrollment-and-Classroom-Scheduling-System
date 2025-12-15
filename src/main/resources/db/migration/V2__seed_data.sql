insert into users (email, password_hash, first_name, last_name, id_card, role, is_active)
values  ('admin@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Lou', 'Chumdararith', 'ADM001', 'ADMIN', TRUE),
        ('lecturer@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Heng', 'Nguonhour', 'LEC001', 'LECTURER', TRUE),
        ('student01@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Din', 'Rasy', 'STU001', 'STUDENT', TRUE),
        ('student02@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Cheang', 'Seyha', 'STU002', 'STUDENT', TRUE),
        ('student03@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Chork', 'Ratanakdevid', 'STU003', 'STUDENT', TRUE);



INSERT INTO academic_terms (term_name, term_type, start_date, end_date, is_active)
VALUES ('Semester I 2025', 'SEMESTER', '2025-12-15', '2026-05-30', TRUE);


INSERT INTO rooms (room_number, building, capacity, room_type) VALUES
('F106', 'Building F', 30, 'LAB'),
('J702', 'Building J', 100, 'LECTURE_HALL');


INSERT INTO user_profiles (user_id, department, bio)
SELECT id, 'Information and communication engineering', 'Senior Lecturer' FROM users WHERE email = 'lecturer@test.com';
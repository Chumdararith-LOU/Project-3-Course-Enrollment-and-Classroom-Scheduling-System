-- Add enrollment_code column to course_offerings
ALTER TABLE course_offerings ADD COLUMN enrollment_code VARCHAR(16) NOT NULL UNIQUE;
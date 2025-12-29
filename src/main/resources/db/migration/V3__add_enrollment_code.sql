-- Add enrollment_code column to courses table
ALTER TABLE courses ADD COLUMN enrollment_code VARCHAR(10) UNIQUE;

-- Generate unique enrollment codes for existing courses
UPDATE courses SET enrollment_code = CONCAT(
    SUBSTRING(course_code, 1, 3),
    LPAD(FLOOR(RAND() * 10000), 4, '0')
) WHERE enrollment_code IS NULL;

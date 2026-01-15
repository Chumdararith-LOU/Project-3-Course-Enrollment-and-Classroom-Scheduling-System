ALTER TABLE course_offerings
ADD COLUMN enrollment_code VARCHAR(255),
ADD COLUMN enrollment_code_expires_at TIMESTAMP;

UPDATE course_offerings
SET enrollment_code = CONCAT(SUBSTRING(MD5(RAND()), 1, 6), '-', id),
    enrollment_code_expires_at = DATE_ADD(NOW(), INTERVAL 7 DAY) 
WHERE enrollment_code IS NULL;

ALTER TABLE course_offerings MODIFY enrollment_code VARCHAR(255) NOT NULL;
ALTER TABLE course_offerings ADD CONSTRAINT uk_offering_enrollment_code UNIQUE (enrollment_code);
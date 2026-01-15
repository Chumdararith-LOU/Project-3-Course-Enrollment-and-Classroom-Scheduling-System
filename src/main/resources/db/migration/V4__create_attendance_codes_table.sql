CREATE TABLE attendance_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    issued_at BIGINT NOT NULL,
    created_by BIGINT,
    present_window_minutes INT,
    late_window_minutes INT,

    CONSTRAINT fk_attendance_codes_schedule
        FOREIGN KEY (schedule_id)
        REFERENCES class_schedules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_attendance_codes_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    INDEX idx_attendance_codes_schedule (schedule_id)
);
-- Trigger for INSERT operations on class_schedules
DELIMITER $$
CREATE TRIGGER check_class_schedule_capacity_insert
    BEFORE INSERT ON class_schedules
    FOR EACH ROW
BEGIN
    DECLARE offering_capacity INT DEFAULT 0;
    DECLARE room_capacity INT DEFAULT 0;

    -- Get the capacity of the offering being scheduled
    SELECT co.capacity INTO offering_capacity
    FROM course_offerings co
    WHERE co.id = NEW.offering_id;

    -- Get the capacity of the room where it's being scheduled
    SELECT r.capacity INTO room_capacity
    FROM rooms r
    WHERE r.id = NEW.room_id;

    -- Check if offering capacity exceeds room capacity
    IF offering_capacity > room_capacity THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insert Error: Course offering capacity exceeds room capacity.';
    END IF;

END$$

-- Trigger for UPDATE operations on class_schedules
CREATE TRIGGER check_class_schedule_capacity_update
    BEFORE UPDATE ON class_schedules
    FOR EACH ROW
BEGIN
    DECLARE offering_capacity INT DEFAULT 0;
    DECLARE room_capacity INT DEFAULT 0;

    -- Get the capacity of the (potentially new) offering being scheduled
    SELECT co.capacity INTO offering_capacity
    FROM course_offerings co
    WHERE co.id = NEW.offering_id;

    -- Get the capacity of the (potentially new) room where it's being scheduled
    SELECT r.capacity INTO room_capacity
    FROM rooms r
    WHERE r.id = NEW.room_id;

    -- Check if offering capacity exceeds room capacity
    IF offering_capacity > room_capacity THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Update Error: Course offering capacity exceeds room capacity.';
    END IF;

END$$

DELIMITER ;
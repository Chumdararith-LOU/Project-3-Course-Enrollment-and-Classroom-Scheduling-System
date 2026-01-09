-- V4: Fix enrollment data - Remove lecturer enrollments and add proper student enrollments
-- Step 1: Delete any enrollments where student_id points to non-student users
DELETE FROM enrollments
WHERE
    student_id IN (
        SELECT
            u.id
        FROM
            users u
            JOIN roles r ON u.role_id = r.id
        WHERE
            r.role_code != 'STUDENT'
    );

-- Step 2: Add proper student enrollments for offering 23 (GIC25WEB - Spring 2026, taught by lecturer01)
INSERT IGNORE INTO
    enrollments (student_id, offering_id, status)
VALUES
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student01@test.com'
        ),
        23,
        'ENROLLED'
    ),
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student02@test.com'
        ),
        23,
        'ENROLLED'
    ),
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student03@test.com'
        ),
        23,
        'ENROLLED'
    );

-- Step 3: Add proper student enrollments for offering 24 (GIC25DB - Spring 2025, taught by lecturer01)
INSERT IGNORE INTO
    enrollments (student_id, offering_id, status)
VALUES
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student04@test.com'
        ),
        24,
        'ENROLLED'
    ),
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student05@test.com'
        ),
        24,
        'ENROLLED'
    );

-- Step 4: Add enrollments for offering 3 (GIC25WEB - Spring 2025, taught by lecturer02/Sarah Janes)
INSERT IGNORE INTO
    enrollments (student_id, offering_id, status)
VALUES
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student01@test.com'
        ),
        3,
        'ENROLLED'
    ),
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student02@test.com'
        ),
        3,
        'ENROLLED'
    ),
    (
        (
            SELECT
                id
            FROM
                users
            WHERE
                email = 'student03@test.com'
        ),
        3,
        'ENROLLED'
    );
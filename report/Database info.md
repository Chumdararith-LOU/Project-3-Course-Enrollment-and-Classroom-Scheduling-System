# **Complete Cardinality, Relationship, and Constraint Summary**


## 🔗 **CARDINALITIES & RELATIONSHIPS**

| From Table | To Table | Relationship Type | Cardinality | Description |
|------------|----------|-------------------|-------------|-------------|
| **users** | **user_profiles** | One-to-One | 1:1 | One user has exactly one profile |
| **users** | **courses** (as lecturer) | One-to-Many | 1:N | One lecturer can teach many courses |
| **users** | **enrollments** (as student) | One-to-Many | 1:N | One student can have many enrollments |
| **users** | **waitlist** | One-to-Many | 1:N | One student can be on many waitlists |
| **users** | **attendance** (recorded_by) | One-to-Many | 1:N | One user can record many attendance entries |
| **academic_terms** | **courses** | One-to-Many | 1:N | One term can have many courses |
| **courses** | **enrollments** | One-to-Many | 1:N | One course can have many enrollments |
| **courses** | **waitlist** | One-to-Many | 1:N | One course can have many waitlisted students |
| **courses** | **class_schedules** | One-to-Many | 1:N | One course can have multiple scheduled sessions |
| **enrollments** | **attendance** | One-to-Many | 1:N | One enrollment can have many attendance records |
| **rooms** | **class_schedules** | One-to-Many | 1:N | One room can host many scheduled sessions |
| **class_schedules** | **attendance** | One-to-Many | 1:N | One schedule can have many attendance records |

---

## 🛡️ **TABLE-SPECIFIC CONSTRAINTS**

### **1. `users`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `email` UNIQUE | Column Constraint | No duplicate emails |
| `id_card` UNIQUE | Column Constraint | Unique student/lecturer ID |
| `role` ENUM-like | Application Logic | Must be STUDENT, LECTURER, ADMIN |
| `is_active` DEFAULT true | Column Constraint | Soft delete support |

### **2. `user_profiles`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `user_id` UNIQUE | Column Constraint | 1:1 relationship with users |
| `user_id` NOT NULL + FK | Referential Integrity | Must reference valid user |

### **3. `academic_terms`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `term_code` UNIQUE | Column Constraint | Unique term identifier |
| `start_date` < `end_date` | Application Logic | Valid date range |
| `is_active` DEFAULT true | Column Constraint | Active term flag |

### **4. `courses`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `course_code` UNIQUE | Column Constraint | Unique course identifier |
| `capacity` > 0 | Application Logic | Positive capacity |
| `lecturer_id` FK + NOT NULL | Referential Integrity | Must have a valid lecturer |
| `term_id` FK + NOT NULL | Referential Integrity | Must belong to a term |
| `is_active` DEFAULT true | Column Constraint | Soft delete support |

### **5. `enrollments`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `(student_id, course_id)` UNIQUE | Composite Constraint | No duplicate enrollments |
| `student_id` FK | Referential Integrity | Valid student |
| `course_id` FK | Referential Integrity | Valid course |
| `status` ENUM-like | Application Logic | ENROLLED, DROPPED, COMPLETED, FAILED, WAITLISTED |
| `grade` NULL allowed | Column Constraint | Grades assigned later |

### **6. `waitlist`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `(student_id, course_id)` UNIQUE | Composite Constraint | No duplicate waitlist entries |
| `position` > 0 | Application Logic | Positive queue position |
| `student_id` FK | Referential Integrity | Valid student |
| `course_id` FK | Referential Integrity | Valid course |

### **7. `rooms`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `room_number` UNIQUE | Column Constraint | Unique room identifier |
| `capacity` > 0 | Application Logic | Positive capacity |
| `is_active` DEFAULT true | Column Constraint | Soft delete support |

### **8. `class_schedules`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `(room_id, day_of_week, start_time, end_time)` UNIQUE | Composite Constraint | Prevent room double-booking |
| `end_time` > `start_time` | Application Logic | Valid time range |
| `course_id` FK + NOT NULL | Referential Integrity | Must belong to a course |
| `room_id` FK + NOT NULL | Referential Integrity | Must be a valid room |

### **9. `attendance`**
| Constraint | Type | Purpose |
|------------|------|---------|
| `(enrollment_id, schedule_id, attendance_date)` UNIQUE | Composite Constraint | One attendance record per student per class per day |
| `enrollment_id` FK | Referential Integrity | Valid enrollment |
| `schedule_id` FK | Referential Integrity | Valid class schedule |
| `status` ENUM-like | Application Logic | PRESENT, ABSENT, EXCUSED, LATE |

---

![alt text](Database.png)

---

## ⚠️ **APPLICATION-LEVEL BUSINESS RULES**

| Rule | Affected Tables | Implementation |
|------|----------------|----------------|
| Student cannot enroll in overlapping schedules | `enrollments`, `class_schedules` | Service-layer validation before enrollment |
| Course capacity not exceeded | `enrollments`, `courses` | Check count of enrolled students vs capacity |
| Waitlist auto-promotion | `waitlist`, `enrollments` | Trigger when enrollment is dropped |
| Only lecturers can mark attendance for their courses | `attendance`, `courses`, `users` | `@PreAuthorize` in Spring Security |
| Students cannot enroll in completed/failed courses | `enrollments` | Check `status` before enrollment |
| Room capacity ≥ enrolled students | `class_schedules`, `rooms`, `enrollments` | Validation when scheduling or enrolling |

---

## 📌 **INDEX SUMMARY**

| Table | Indexes | Purpose |
|-------|---------|---------|
| **users** | email, id_card, role, is_active | Fast lookups by email, ID, role, active status |
| **user_profiles** | user_id | Quick profile retrieval by user |
| **academic_terms** | term_code, is_active | Term lookup, active term filtering |
| **courses** | course_code, lecturer_id, term_id | Course search, lecturer courses, term courses |
| **enrollments** | (student_id, course_id), course_id, status | Prevent duplicates, course enrollments, status filtering |
| **waitlist** | (student_id, course_id), course_id, position | Prevent duplicates, course waitlist, queue order |
| **rooms** | room_number | Room lookup |
| **class_schedules** | (room_id, day_of_week, start_time, end_time), course_id | Prevent double-booking, course schedules |
| **attendance** | (enrollment_id, schedule_id, attendance_date), schedule_id, attendance_date | Prevent duplicates, schedule-based lookups, date filtering |

---

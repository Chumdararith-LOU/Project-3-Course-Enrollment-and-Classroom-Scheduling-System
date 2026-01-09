# Academic Project Submission: Course Enrollment and Classroom Scheduling System

## 1. Project Metadata

| Field                         | Details                                                             |
| ----------------------------- | ------------------------------------------------------------------- |
| **Project Title**       | Course Enrollment and Classroom Scheduling System                   |
| **Student Name**        | 
$$
Enter Your Name Here
$$

                                             |
| **Student ID**          | 
$$
Enter Your ID Here
$$

                                               |
| **Course Code & Name**  | 
$$
Enter Course Code, e.g., CS400 Web Application Development
$$

       |
| **Semester/Year**       | 
$$
e.g., Semester 1, 2025
$$

                                           |
| **Lecturer/Supervisor** | 
$$
Enter Lecturer Name
$$

                                              |
| **Submission Date**     | 
$$
Enter Date
$$

                                                       |
| **Project Type**        | ☑ Course Project ☐ Capstone ☐ Research Project ☐ Other: _______ |
| **Repository URL**      | 
$$
Insert your GitHub Link Here
$$

                                     |
| **Live Demo/Prototype** | 
$$
Insert Deployment Link or "Localhost"
$$

                            |

---

## 2. Executive Summary

**What problem did you solve?**
Universities often face logistical chaos during course registration, resulting in double-booked classrooms, student enrollment conflicts, and manual administrative bottlenecks. This project solves the "Digital Registrar" problem by automating the intersection of three critical resources: Students, Lecturers, and Physical Rooms.

**What did you build?**
We developed a comprehensive Course Enrollment and Classroom Scheduling System using Spring Boot and Thymeleaf. The system features a three-tier role-based architecture (Admin, Lecturer, Student) that handles course creation, dynamic scheduling with conflict detection, automated waitlist promotion, and attendance tracking.

**How did you measure success?**
Success was measured by the system's ability to strictly enforce business rules: zero database inconsistencies regarding room capacity (e.g., preventing 31 students in a 30-seat room) and 100% prevention of time-slot conflicts for both rooms and students.

**Why does it matter?**
This project demonstrates the application of enterprise-grade application security and data integrity principles. It moves beyond simple CRUD operations to handle complex many-to-many relationships and transactional business logic, directly aligning with the course's advanced web development learning outcomes.

---

## 3. Problem Statement & Objectives

### 3.1 The "Why" Behind Your Project

**Context & Motivation:**
Academic institutions rely on the precise coordination of time and space. Manual or spreadsheet-based scheduling is prone to human error, specifically "double-booking" (two classes in one room) and "over-enrollment" (exceeding fire safety capacities).

**Specific Problem:**
Existing basic CRUD systems often fail to validate temporal constraints. They allow a lecturer to book Room A205 at 8:00 AM even if it is already occupied. Furthermore, they often lack a fair, automated mechanism for managing waitlists when enrolled students drop courses.

### 3.2 Learning Objectives & Success Criteria

| Objective Type | Description                                | Success Metric                                                                                 |
| -------------- | ------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| Technical      | Implement Role-Based Access Control (RBAC) | Secure endpoints: Students cannot access `/admin/**` or `/lecturer/**`.                    |
| Technical      | Complex Business Validation (Scheduling)   | Time conflict algorithm passes 100% of test cases for overlapping intervals.                   |
| User Impact    | Automate Waitlist Management               | Dropping a course immediately promotes the next waitlisted student without admin intervention. |

---

## 4. Background & Related Work

**Key Concepts:**

- **RBAC (Role-Based Access Control):** Restricting system access to authorized users based on roles (Student, Lecturer, Admin).
- **Temporal Logic:** The mathematical handling of time intervals to detect overlaps.
- **Database Normalization:** Ensuring data integrity across Users, Courses, and Schedules.

**Existing Solutions:**
Commercial solutions like Blackboard or Moodle exist but are often monolithic, expensive, and difficult to customize for specific departmental needs. Simple open-source tutorials often cover e-commerce or blogs but rarely cover the "Scheduling Constraint" problem.

**Gap Your Project Fills:**
This project bridges the gap between basic data management and complex logic. Unlike a simple blog, this system must validate the state of the database (Is the room free? Is the student already busy?) before allowing a write operation.

---

## 5. Methodology & Design

### 5.1 Architectural Design

**Design Pattern:** Layered MVC Architecture (Model-View-Controller)
**Justification:** This industry-standard pattern ensures separation of concerns. The Controller handles HTTP requests, the Service layer contains the complex business logic (like conflict checking), and the Repository layer interacts with the database.

**System Overview:**The system is built using a classic Spring Boot layered architecture to ensure separation of concerns, testability, and maintainability. It supports three primary user roles:

- **Student:** Browse courses, enroll, view schedule.
- **Lecturer:** Create courses, assign schedules, record attendance.
- **Admin:** Manage academic terms, rooms, and system metrics.

**System Diagram:**

$$
Insert Image Here: System Architecture Diagram
$$

> **Figure 1:** High-Level MVC Architecture showing the flow from Client Browsers through Thymeleaf Controllers, down to the Service Layer and MySQL Database.

### 5.2 Technology Stack

| Layer      | Technology              | Rationale (Why this tool?)                                                                                           |
| ---------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Frontend   | Thymeleaf, Tailwind CSS | Server-side rendering for security; Tailwind for rapid, responsive UI development without writing custom CSS files.  |
| Backend    | Spring Boot 3, Java 17  | Robust ecosystem for dependency injection and security (`spring-boot-starter-security`).                           |
| Database   | MySQL, Flyway           | MySQL for relational data integrity; Flyway for version-controlled database migrations (`V1__init`, `V2__seed`). |
| Security   | Spring Security 6       | Standard for handling authentication/authorization chains and CSRF protection.                                       |
| Build Tool | Maven                   | Dependency management and build automation.                                                                          |

### 5.3 Key Design Decisions

| Decision                 | Alternatives Considered       | Trade-offs & Academic Rationale                                                                                                                 |
| ------------------------ | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Flyway for Migrations    | Hibernate `ddl-auto=update` | `ddl-auto` is unpredictable in production. Flyway ensures every team member has the exact same DB schema state.                               |
| Service-Layer Validation | Database Triggers             | Implementing logic in Java (`TimeConflictChecker.java`) makes the code testable and version-controlled, unlike SQL triggers hidden in the DB. |

---

## 6. Implementation Narrative

### Case Study 1: The "Double-Booking" Prevention Algorithm

**Problem:**
A critical requirement was ensuring no two classes could occupy the same room at the same time. A naive check of `start_time == start_time` fails if Class A is 08:00–10:00 and Class B tries to book 09:00–11:00.

**Solution:**
I implemented a robust `TimeConflictChecker` utility.

$$
Insert Image Here: Timeline Visualization
$$

> **Figure 2:** Visual representation of the Time Conflict logic. The algorithm detects the intersection between Interval A and Interval B.

**Implementation Highlights:**
The logic checks for the intersection of time intervals using the formula:
`(StartA < EndB) && (EndA > StartB)`
This is applied in the `ScheduleService` before saving any entity.

From `src/main/java/com/cource/util/TimeConflictChecker.java`:

```java
public boolean hasConflict(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
    return start1.isBefore(end2) && start2.isBefore(end1);
}
```


**Outcome:**
This automated a manual administrative task, ensuring fair, first-come-first-served access to course seats.

## 7. Results & Impact

### 7.1 Performance & Reliability Metrics

| Metric                       | Target         | Actual   | Tool Used                           |
| ---------------------------- | -------------- | -------- | ----------------------------------- |
| **Conflict Detection** | 100% Rejection | 100%     | Unit Tests (TimeConflictChecker)    |
| **Role Isolation**     | Strict         | Verified | Spring Security (`@PreAuthorize`) |
| **Startup Time**       | < 10s          | ~4.5s    | Spring Boot Actuator                |

### 7.2 User Testing & Feedback

**Testing Method:** Peer testing within the project group.

**Key Findings:**

- **Issue:** Initial UI for the schedule was confusing (just a list of dates).**Fix:** Implemented a grid view in `student/schedule.html` using Thymeleaf loops to visualize the week.
- **Issue:** Lecturers could accidentally delete active courses.
  **Fix:** Added foreign key constraints in SQL (`V1__init_schema.sql`) to prevent deletion if enrollments exist.

> $$
> Insert Image Here: Student Schedule UI Screenshot
> $$

*Figure 4: The improved Student Schedule Grid View implemented after user feedback, clearly showing class blocks.*

## 8. Reflection & Lessons Learned

### 8.1 Technical Reflections

**What worked well?**
The use of DTOs (Data Transfer Objects). Separating the internal entities (`User`, `Course`) from the API requests (`UserCreateRequest`, `CourseResponseDTO`) prevented over-posting attacks and circular reference issues in JSON serialization.

**What would you do differently?**
I initially struggled with Many-to-Many relationships (Students <-> Courses). In the future, I would spend more time designing the ER Diagram before coding to avoid complex JPA refactoring mid-project.

### 8.2 Process Reflections

- **Collaboration:** Using Git branches (e.g., `feature/enrollment-drop-waitlist`) as defined in our Sprint documents helped avoid merge conflicts.
- **Security Configuration:** Configuring the `SecurityFilterChain` in Spring Boot 3 was challenging due to recent deprecations (removal of `WebSecurityConfigurerAdapter`), forcing me to learn the modern, component-based security configuration.

### 8.3 Connection to Course Learning Outcomes

This project directly demonstrates the ability to "Design and develop data-driven web applications," specifically satisfying the requirement to handle authentication, session management, and relational database transactions.

## 9. Professional Development & Next Steps

**Skills Gained:**

- Advanced Spring Boot (Security, JPA, Validation).
- Database Migration strategies (Flyway).
- Front-end logic with Thymeleaf and Tailwind CSS.

**Future Enhancements:**

- **Email Notifications:** Integrate `JavaMailSender` to notify students when they are promoted from the waitlist.
- **Calendar Export:** Allow students to export their schedule to `.ics` format for Google Calendar.

## 10. References

1. Walls, C. (2018). *Spring Boot in Action*. Manning Publications.
2. Spring.io. (2024). *Spring Security Reference Documentation*. Retrieved from https://docs.spring.io
3. Meier, M. (2024). *Thymeleaf + Spring Boot Tutorial*.

## 11. Appendices

### Appendix A: Database Schema

> $$
> Insert Image Here: Entity Relationship Diagram (ERD)
> $$

*Figure 5: Full Entity Relationship Diagram showing all 12 tables and relationships (Users, Courses, Schedules, Enrollments).*

(See `src/main/resources/db/migration/V1__init_schema.sql` in repository for full DDL).

### Appendix B: Key API Endpoints

| Method | Endpoint              | Description             | Role     |
| ------ | --------------------- | ----------------------- | -------- |
| GET    | `/login`            | Login page              | Public   |
| POST   | `/login`            | Authenticate user       | Public   |
| GET    | `/student/catalog`  | View available courses  | STUDENT  |
| POST   | `/enrollments`      | Enroll in course        | STUDENT  |
| GET    | `/lecturer/courses` | View my courses         | LECTURER |
| POST   | `/courses`          | Create course offering  | LECTURER |
| GET    | `/admin/dashboard`  | Admin metrics dashboard | ADMIN    |

### Appendix C: Team Member Contributions

| Role                   | Team Member | Key Deliverables                                                     |
| ---------------------- | ----------- | -------------------------------------------------------------------- |
| Security & Auth Lead   | David       | Spring Security config, role-based redirects, login/logout.          |
| Student Features       | Rasy        | Course, Enrollment, Waitlist entities/services, capacity logic.      |
| Lecturer Features      | Hour        | Room, ClassSchedule, Attendance design; scheduling conflict rules.   |
| Admin Features         | Seyha       | Admin dashboard, Thymeleaf layout, role-aware UI fragments.          |
| Database & DevOps Lead | RITH        | Full Flyway migrations, MySQL setup, seed data, final documentation. |

## 12. Statement of Academic Integrity

I certify that this project submission is my own original work, except where otherwise acknowledged. All external sources, code, and assistance received are properly cited. I understand that academic dishonesty may result in severe penalties.

**Student Signature:** _________________________
**Date:** __________

```

```

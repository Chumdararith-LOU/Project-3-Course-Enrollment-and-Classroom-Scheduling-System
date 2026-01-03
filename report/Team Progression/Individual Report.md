### **1. Database & DevOps Lead (LOU Chumdararith)**
**Role:** System Infrastructure & Integration

| Category              | Details                                                                                                                                                                                                                                                                                                                                        |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Assigned Tasks**   | - Design MySQL database schema (users, courses, enrollments, etc.)<br>- Implement Flyway migrations for schema versioning<br>- Create seed data (Admin, Lecturer, Student, Rooms, Spring 2025 Term)<br>- Configure Spring Boot database connection pooling                                                                                     |
| **Additional Work**  | - Merged all feature branches (Security, Student, Lecturer)<br>- Finalized Spring Security form login and role-based post-login redirection<br>- Built Course Catalog UI (Tailwind) and implemented backend logic to fetch active courses by term<br>- Fully implemented “Create Course” backend: auto lecturer_id, term filtering, validation |
| **Status**           | All assigned and integration tasks completed                                                                                                                                                                                                                                                                                                   |
| **Notes**            | Served as primary integrator; completed critical unfinished work from other sub-teams to ensure week delivery.                                                                                                                                                                                                                                 |

---

### **2. Security & Authentication Developer (CHORK Ratanakdavid)**
**Role:** Sub-Team – Security & System Core

| Category              | Details                                                                                                                                                                         |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Assigned Tasks**   | - Configure Spring Security for role-based access (STUDENT, LECTURER, ADMIN)<br>- Implement login/logout logic and UI protection                                                |
| **Completed Work**   | - Defined core security architecture<br>- Implemented secure logout with session invalidation<br>- Set up BCrypt password encoding<br>- Configured access-denied error handling |
| **Status**           | Partially complete (core infrastructure done)                                                                                                                                   |
| **Notes**            | Form login configuration and role-based redirect logic were drafted but **finalized by Database Lead** to ensure consistency and functionality.                                 |

---

### **3. Student Module Developer (DIN Rasy)**
**Role:** Sub-Team – Student Features

| Category              | Details                                                                                                                                                              |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Assigned Tasks**   | - Build Student Course Catalog<br>- Implement Enrollment flow UI                                                                                                     |
| **Completed Work**   | - Designed UI for Student Dashboard, My Courses, Schedule, Grades, and Attendance pages<br>- Implemented responsive sidebar navigation and header using Tailwind CSS |
| **Status**           | Partially complete (UI only)                                                                                                                                         |
| **Notes**            | Course Catalog **layout was enhanced**, but **backend logic to fetch active courses** was implemented by Database Lead. Enrollment flow backend not delivered.       |

---

### **4. Lecturer Module Developer (HENG Nguonhour)**
**Role:** Sub-Team – Lecturer Features

| Category              | Details                                                                                                                                                                             |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Assigned Tasks**   | - Build “Create Course” form<br>- Implement Lecturer Dashboard                                                                                                                      |
| **Completed Work**   | - Built UI for Lecturer Dashboard, Schedule view, and Student List<br>- Structured Thymeleaf views and Spring MVC controller layer                                                  |
| **Status**           | Partially complete (frontend only)                                                                                                                                                  |
| **Notes**            | “Create Course” **backend logic and validation** (code format, uniqueness, term filtering) were **completed by Database Lead** due to implementation errors in original submission. |

---

### **5. Admin Module Developer (CHEANG Seyha)**
**Role:** Sub-Team – Admin Features

| Category              | Details                                                                                                                              |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| **Assigned Tasks**   | - Build Admin Dashboard<br>- Provide minimal system overview                                                                         |
| **Completed Work**   | - Implemented Admin Dashboard UI<br>- Created management interfaces for Users, Courses, Rooms, and Academic Terms (CRUD-ready views) |
| **Status**           | UI tasks completed<br> Not yet integrated                                                                                            |
| **Notes**            | Feature branch is **ready but not merged** into the main codebase (pending integration by Database Lead).                            |


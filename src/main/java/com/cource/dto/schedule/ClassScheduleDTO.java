package com.cource.dto.schedule;

public class ClassScheduleDTO {
    private Long id;
    private OfferingDTO offering;
    private RoomDTO room;
    private String dayOfWeek;
    private String startTime;
    private String endTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OfferingDTO getOffering() {
        return offering;
    }

    public void setOffering(OfferingDTO offering) {
        this.offering = offering;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public static class OfferingDTO {
        private Long id;
        private CourseDTO course;
        private TermDTO term;
        private Integer capacity;
        private Boolean active;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public CourseDTO getCourse() {
            return course;
        }

        public void setCourse(CourseDTO course) {
            this.course = course;
        }

        public TermDTO getTerm() {
            return term;
        }

        public void setTerm(TermDTO term) {
            this.term = term;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class CourseDTO {
        private Long id;
        private String courseCode;
        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public void setCourseCode(String courseCode) {
            this.courseCode = courseCode;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class TermDTO {
        private Long id;
        private String termName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTermName() {
            return termName;
        }

        public void setTermName(String termName) {
            this.termName = termName;
        }
    }

    public static class RoomDTO {
        private Long id;
        private String roomNumber;
        private String building;
        private String roomType;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        public String getBuilding() {
            return building;
        }

        public void setBuilding(String building) {
            this.building = building;
        }

        public String getRoomType() {
            return roomType;
        }

        public void setRoomType(String roomType) {
            this.roomType = roomType;
        }
    }
}

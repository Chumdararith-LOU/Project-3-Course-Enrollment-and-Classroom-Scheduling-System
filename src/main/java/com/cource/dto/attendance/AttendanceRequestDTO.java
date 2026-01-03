package com.cource.dto.attendance;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDTO {

    private Long studentId;
    private Long scheduleId;
    private String status; // "PRESENT", "ABSENT", "LATE", "EXCUSED"
    private LocalDate attendanceDate;
    private String notes;

    public void setLecturerId(Long currentUserId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLecturerId'");
    }

    public Object getLecturerId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLecturerId'");
    }
}

package com.cource.dto.attendance;

public class AttendanceCodeEnterResultDTO {
    private final boolean saved;
    private final boolean exists;
    private final Long attendanceId;
    private final String status;

    public AttendanceCodeEnterResultDTO(boolean saved, boolean exists, Long attendanceId, String status) {
        this.saved = saved;
        this.exists = exists;
        this.attendanceId = attendanceId;
        this.status = status;
    }

    public boolean isSaved() {
        return saved;
    }

    public boolean isExists() {
        return exists;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public String getStatus() {
        return status;
    }
}

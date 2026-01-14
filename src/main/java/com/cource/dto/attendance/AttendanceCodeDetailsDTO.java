package com.cource.dto.attendance;

public class AttendanceCodeDetailsDTO {
    private final String code;
    private final long issuedAt;
    private final Integer presentMinutes;
    private final Integer lateMinutes;
    private final Long offeringId;
    private final long enrolledCount;

    public AttendanceCodeDetailsDTO(String code, long issuedAt, Integer presentMinutes, Integer lateMinutes,
            Long offeringId, long enrolledCount) {
        this.code = code;
        this.issuedAt = issuedAt;
        this.presentMinutes = presentMinutes;
        this.lateMinutes = lateMinutes;
        this.offeringId = offeringId;
        this.enrolledCount = enrolledCount;
    }

    public String getCode() {
        return code;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public Integer getPresentMinutes() {
        return presentMinutes;
    }

    public Integer getLateMinutes() {
        return lateMinutes;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public long getEnrolledCount() {
        return enrolledCount;
    }
}

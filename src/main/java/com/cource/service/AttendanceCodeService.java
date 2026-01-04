package com.cource.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

public interface AttendanceCodeService {

    @Getter
    @AllArgsConstructor
    class CodeInfo {
        private final String code;
        private final long issuedAt; // epoch seconds
        private final Long createdBy;
        private final Integer presentWindowMinutes;
        private final Integer lateWindowMinutes;
    }
    CodeInfo generate(Long scheduleId, Long creatorId);
    CodeInfo generate(Long scheduleId, Long creatorId, Integer presentWindowMinutes, Integer lateWindowMinutes);
    CodeInfo get(Long scheduleId);
    void delete(Long scheduleId);
}
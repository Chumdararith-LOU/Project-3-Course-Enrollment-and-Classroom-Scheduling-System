package com.cource.service;

import java.time.Instant;

import java.util.Optional;

import com.cource.entity.AttendanceCode;
import com.cource.repository.AttendanceCodeRepository;
import jakarta.transaction.Transactional;

public class AttendanceCodeService {
    private final AttendanceCodeRepository repo;

    // expiry seconds for a code (default 2 hours)
    private final long expirySeconds = 2 * 60 * 60;

    public static class CodeInfo {
        private final String code;
        private final long issuedAt; // epoch seconds
        private final Long createdBy;
        private final Integer presentWindowMinutes;
        private final Integer lateWindowMinutes;

        public CodeInfo(String code, long issuedAt, Long createdBy, Integer presentWindowMinutes,
                Integer lateWindowMinutes) {
            this.code = code;
            this.issuedAt = issuedAt;
            this.createdBy = createdBy;
            this.presentWindowMinutes = presentWindowMinutes;
            this.lateWindowMinutes = lateWindowMinutes;
        }

        public String getCode() {
            return code;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public Long getCreatedBy() {
            return createdBy;
        }

        public Integer getPresentWindowMinutes() {
            return presentWindowMinutes;
        }

        public Integer getLateWindowMinutes() {
            return lateWindowMinutes;
        }
    }

    public AttendanceCodeService(AttendanceCodeRepository repo) {
        this.repo = repo;
    }

    public CodeInfo generate(Long scheduleId, Long creatorId) {
        return generate(scheduleId, creatorId, null, null);
    }

    public CodeInfo generate(Long scheduleId, Long creatorId, Integer presentWindowMinutes, Integer lateWindowMinutes) {
        String code = generateShortCode();
        long now = Instant.now().getEpochSecond();
        // remove existing
        repo.deleteByScheduleId(scheduleId);
        AttendanceCode ac = new AttendanceCode(scheduleId, code, now, creatorId, presentWindowMinutes,
                lateWindowMinutes);
        ac = repo.save(ac);
        return new CodeInfo(ac.getCode(), ac.getIssuedAt(), ac.getCreatedBy(), ac.getPresentWindowMinutes(),
                ac.getLateWindowMinutes());
    }

    public CodeInfo get(Long scheduleId) {
        Optional<AttendanceCode> o = repo.findByScheduleId(scheduleId);
        if (o.isEmpty())
            return null;
        AttendanceCode ac = o.get();
        long now = Instant.now().getEpochSecond();
        if (expirySeconds > 0 && now - ac.getIssuedAt() > expirySeconds) {
            repo.delete(ac);
            return null;
        }
        return new CodeInfo(ac.getCode(), ac.getIssuedAt(), ac.getCreatedBy(), ac.getPresentWindowMinutes(),
                ac.getLateWindowMinutes());
    }

    @Transactional
    public void delete(Long scheduleId) {
        repo.deleteByScheduleId(scheduleId);
    }

    private String generateShortCode() {
        int num = (int) (Math.floor(Math.random() * 900000) + 100000);
        return Integer.toString(num);
    }
}

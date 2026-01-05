package com.cource.service.impl;

import com.cource.entity.AttendanceCode;
import com.cource.repository.AttendanceCodeRepository;
import com.cource.service.AttendanceCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceCodeServiceImpl implements AttendanceCodeService {

    private final AttendanceCodeRepository repo;

    private final long expirySeconds = 2 * 60 * 60;

    @Override
    @Transactional
    public CodeInfo generate(Long scheduleId, Long creatorId) {
        return generate(scheduleId, creatorId, null, null);
    }

    @Override
    @Transactional
    public CodeInfo generate(Long scheduleId, Long creatorId, Integer presentWindowMinutes, Integer lateWindowMinutes) {
        String code = generateShortCode();
        long now = Instant.now().getEpochSecond();

        repo.deleteByScheduleId(scheduleId);

        AttendanceCode ac = new AttendanceCode();
        ac.setScheduleId(scheduleId);
        ac.setCode(code);
        ac.setIssuedAt(now);
        ac.setCreatedBy(creatorId);
        ac.setPresentWindowMinutes(presentWindowMinutes);
        ac.setLateWindowMinutes(lateWindowMinutes);

        ac = repo.save(ac);

        return new CodeInfo(ac.getCode(), ac.getIssuedAt(), ac.getCreatedBy(), ac.getPresentWindowMinutes(), ac.getLateWindowMinutes());
    }

    @Override
    @Transactional(readOnly = true)
    public CodeInfo get(Long scheduleId) {
        Optional<AttendanceCode> o = repo.findByScheduleId(scheduleId);
        if (o.isEmpty()) {
            return null;
        }

        AttendanceCode ac = o.get();
        long now = Instant.now().getEpochSecond();

        if (expirySeconds > 0 && now - ac.getIssuedAt() > expirySeconds) {
            repo.delete(ac);
            return null;
        }

        return new CodeInfo(ac.getCode(), ac.getIssuedAt(), ac.getCreatedBy(), ac.getPresentWindowMinutes(), ac.getLateWindowMinutes());
    }

    @Override
    @Transactional
    public void delete(Long scheduleId) {
        repo.deleteByScheduleId(scheduleId);
    }

    private String generateShortCode() {
        int num = (int) (Math.floor(Math.random() * 900000) + 100000);
        return Integer.toString(num);
    }
}

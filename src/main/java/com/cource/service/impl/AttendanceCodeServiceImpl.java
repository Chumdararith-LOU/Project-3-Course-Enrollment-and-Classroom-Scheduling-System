package com.cource.service.impl;

import com.cource.entity.AttendanceCode;
import com.cource.entity.ClassSchedule;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AttendanceCodeRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.service.AttendanceCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceCodeServiceImpl implements AttendanceCodeService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static final long CODE_EXPIRY_SECONDS = 2 * 60 * 60;

    private final AttendanceCodeRepository attendanceCodeRepository;
    private final ClassScheduleRepository classScheduleRepository;

    @Override
    @Transactional
    public AttendanceCode generateAttendanceCode(Long scheduleId, Long lecturerId) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        attendanceCodeRepository.deleteByScheduleId(scheduleId);

        String code = generateCode(6);
        while (attendanceCodeRepository.findByCode(code).isPresent()) {
            code = generateCode(6); // ensure uniqueness
        }

        AttendanceCode attendanceCode = new AttendanceCode();
        attendanceCode.setSchedule(schedule);
        attendanceCode.setCode(code);
        attendanceCode.setIssuedAt(Instant.now().getEpochSecond());

        User createdBy = new User();
        createdBy.setId(lecturerId);
        attendanceCode.setCreatedBy(createdBy);

        attendanceCode.setPresentWindowMinutes(10);
        attendanceCode.setLateWindowMinutes(20);

        return attendanceCodeRepository.save(attendanceCode);
    }

    @Override
    public AttendanceCode findByScheduleId(Long scheduleId) {
        return attendanceCodeRepository.findBySchedule_Id(scheduleId)
                .filter(this::isNotExpired)
                .orElse(null);
    }

    @Override
    public AttendanceCode findByCode(String code) {
        return attendanceCodeRepository.findByCode(code)
                .filter(this::isNotExpired)
                .orElse(null);
    }

    @Override
    public AttendanceCode save(AttendanceCode code) {
        return attendanceCodeRepository.save(code);
    }

    @Override
    public boolean isValidCode(String code, Long scheduleId) {
        return attendanceCodeRepository.findByCode(code)
                .filter(ac -> ac.getSchedule().getId().equals(scheduleId))
                .isPresent();
    }

    @Override
    public void deleteByScheduleId(Long scheduleId) {
        attendanceCodeRepository.deleteByScheduleId(scheduleId);
    }

    private boolean isNotExpired(AttendanceCode code) {
        if (CODE_EXPIRY_SECONDS <= 0) return true;
        long now = Instant.now().getEpochSecond();
        return (now - code.getIssuedAt()) <= CODE_EXPIRY_SECONDS;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}

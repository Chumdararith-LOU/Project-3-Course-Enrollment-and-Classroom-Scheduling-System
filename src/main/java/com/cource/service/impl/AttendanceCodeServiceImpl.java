package com.cource.service.impl;

import com.cource.entity.AttendanceCode;
import com.cource.entity.ClassSchedule;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AttendanceCodeRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.UserRepository;
import com.cource.service.AttendanceCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AttendanceCodeServiceImpl implements AttendanceCodeService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static final long CODE_EXPIRY_SECONDS = 2 * 60 * 60;

    private final AttendanceCodeRepository attendanceCodeRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AttendanceCode generateAttendanceCode(Long scheduleId, Long lecturerId, Long issuedAt) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        AttendanceCode attendanceCode = attendanceCodeRepository.findBySchedule_Id(scheduleId)
                .orElse(new AttendanceCode());

        if (attendanceCode.getId() == null) {
            attendanceCode.setSchedule(schedule);
        }

        String code = generateCode(6);
        while (attendanceCodeRepository.findByCode(code).isPresent()) {
            code = generateCode(6);
        }
        attendanceCode.setCode(code);

        if (issuedAt != null) {
            attendanceCode.setIssuedAt(issuedAt);
        } else {
            attendanceCode.setIssuedAt(Instant.now().getEpochSecond());
        }

        if (lecturerId != null) {
            User createdBy = userRepository.findById(lecturerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));
            attendanceCode.setCreatedBy(createdBy);
        }

        if (attendanceCode.getPresentWindowMinutes() == null) attendanceCode.setPresentWindowMinutes(10);
        if (attendanceCode.getLateWindowMinutes() == null) attendanceCode.setLateWindowMinutes(20);

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

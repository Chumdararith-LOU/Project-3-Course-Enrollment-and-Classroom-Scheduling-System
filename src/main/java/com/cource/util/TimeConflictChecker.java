package com.cource.util;

import com.cource.entity.ClassSchedule;

import java.time.LocalTime;
import java.util.List;

public class TimeConflictChecker {
    public boolean hasConflict(ClassSchedule target, List<ClassSchedule> existingSchedules) {
        if (target == null || existingSchedules == null || existingSchedules.isEmpty()) {
            return false;
        }

        for (ClassSchedule existing : existingSchedules) {
            if (existing.getDayOfWeek().equalsIgnoreCase(target.getDayOfWeek())) {
                if (isTimeOverlapping(target, existing)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTimeOverlapping(ClassSchedule newSchedule, ClassSchedule existingSchedule) {
        LocalTime start1 = newSchedule.getStartTime();
        LocalTime end1 = newSchedule.getEndTime();
        LocalTime start2 = existingSchedule.getStartTime();
        LocalTime end2 = existingSchedule.getEndTime();

        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}

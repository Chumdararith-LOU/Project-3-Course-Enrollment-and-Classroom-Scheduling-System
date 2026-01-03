package com.cource.dto.schedule;

import com.cource.entity.ClassSchedule;

import java.util.List;
import java.util.stream.Collectors;

public class ClassScheduleMapper {

    public static ClassScheduleDTO toDto(ClassSchedule cs) {
        if (cs == null)
            return null;
        ClassScheduleDTO dto = new ClassScheduleDTO();
        dto.setId(cs.getId());
        ClassScheduleDTO.RoomDTO room = new ClassScheduleDTO.RoomDTO();
        if (cs.getRoom() != null) {
            room.setId(cs.getRoom().getId());
            room.setRoomNumber(cs.getRoom().getRoomNumber());
            room.setBuilding(cs.getRoom().getBuilding());
            room.setRoomType(cs.getRoom().getRoomType());
        }
        dto.setRoom(room);
        dto.setDayOfWeek(cs.getDayOfWeek());
        dto.setStartTime(cs.getStartTime() != null ? cs.getStartTime().toString() : null);
        dto.setEndTime(cs.getEndTime() != null ? cs.getEndTime().toString() : null);
        if (cs.getOffering() != null) {
            ClassScheduleDTO.OfferingDTO off = new ClassScheduleDTO.OfferingDTO();
            off.setId(cs.getOffering().getId());
            off.setCapacity(cs.getOffering().getCapacity());
            off.setActive(cs.getOffering().isActive());
            if (cs.getOffering().getCourse() != null) {
                ClassScheduleDTO.CourseDTO c = new ClassScheduleDTO.CourseDTO();
                c.setId(cs.getOffering().getCourse().getId());
                c.setCourseCode(cs.getOffering().getCourse().getCourseCode());
                c.setTitle(cs.getOffering().getCourse().getTitle());
                off.setCourse(c);
            }
            if (cs.getOffering().getTerm() != null) {
                ClassScheduleDTO.TermDTO t = new ClassScheduleDTO.TermDTO();
                t.setId(cs.getOffering().getTerm().getId());
                t.setTermName(cs.getOffering().getTerm().getTermName());
                off.setTerm(t);
            }
            dto.setOffering(off);
        }
        return dto;
    }

    public static List<ClassScheduleDTO> toDtoList(List<ClassSchedule> list) {
        if (list == null)
            return List.of();
        return list.stream().map(ClassScheduleMapper::toDto).collect(Collectors.toList());
    }
}

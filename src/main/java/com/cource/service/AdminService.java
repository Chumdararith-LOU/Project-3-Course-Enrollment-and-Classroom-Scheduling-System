package com.cource.service;

import com.cource.entity.AcademicTerm;
import com.cource.entity.Room;

import java.util.List;

public interface AdminService {
    AcademicTerm createTerm(AcademicTerm term);
    AcademicTerm updateTerm(Long id, AcademicTerm termDetails);
    List<AcademicTerm> getAllTerms();

    Room createRoom(Room room);
    Room updateRoom(Long id, Room roomDetails);
    List<Room> getAllRooms();
}

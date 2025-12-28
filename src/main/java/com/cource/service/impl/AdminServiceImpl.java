package com.cource.service.impl;

import com.cource.entity.AcademicTerm;
import com.cource.entity.Room;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.RoomRepository;
import com.cource.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AcademicTermRepository termRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // Security: Only ADMIN
    public AcademicTerm createTerm(AcademicTerm term) {
        if (term.getStartDate() != null && term.getEndDate() != null
                && term.getStartDate().isAfter(term.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        return termRepository.save(term);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AcademicTerm updateTerm(Long id, AcademicTerm termDetails) {
        AcademicTerm term = termRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found"));

        term.setTermName(termDetails.getTermName());
        term.setStartDate(termDetails.getStartDate());
        term.setEndDate(termDetails.getEndDate());
        term.setActive(termDetails.isActive());

        return termRepository.save(term);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')") // Public read for authenticated users
    public List<AcademicTerm> getAllTerms() {
        return termRepository.findAll();
    }


    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // Security: Only ADMIN
    public Room createRoom(Room room) {
        if (room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0");
        }

        return roomRepository.save(room);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Room updateRoom(Long id, Room roomDetails) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (roomDetails.getCapacity() <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0");
        }

        room.setRoomNumber(roomDetails.getRoomNumber());
        room.setBuilding(roomDetails.getBuilding());
        room.setCapacity(roomDetails.getCapacity());

        return roomRepository.save(room);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')") // Lecturers need to see rooms to schedule
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}

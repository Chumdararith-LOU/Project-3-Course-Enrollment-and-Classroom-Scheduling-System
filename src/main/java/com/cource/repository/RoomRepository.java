package com.cource.repository;

import com.cource.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    // Check if room number exists (for create)
    boolean existsByRoomNumber(String roomNumber);

    // Check if room number exists excluding a specific ID (for update)
    boolean existsByRoomNumberAndIdNot(String roomNumber, Long id);
}
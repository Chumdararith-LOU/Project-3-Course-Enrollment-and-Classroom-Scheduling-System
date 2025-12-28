package com.cource.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
}

package com.cource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cource.entity.ClassSchedule;
import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByOfferingId(Long offeringId);

    List<ClassSchedule> findByRoomId(Long roomId);
}

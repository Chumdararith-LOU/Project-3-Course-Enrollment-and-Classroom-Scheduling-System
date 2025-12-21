package com.cource.repository;

import com.cource.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    @Query("SELECT co FROM CourseOffering co JOIN co.term t WHERE co.active = true AND t.active = true")
    List<CourseOffering> findAllActiveOfferings();
}

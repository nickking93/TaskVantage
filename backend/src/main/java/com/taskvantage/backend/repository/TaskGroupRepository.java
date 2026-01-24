package com.taskvantage.backend.repository;

import com.taskvantage.backend.model.TaskGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {

    List<TaskGroup> findByUserIdOrderByDisplayOrderAsc(Long userId);

    @Query("SELECT COALESCE(MAX(tg.displayOrder), 0) FROM TaskGroup tg WHERE tg.userId = :userId")
    Integer findMaxDisplayOrderByUserId(@Param("userId") Long userId);
}

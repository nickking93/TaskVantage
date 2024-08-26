package com.taskvantage.backend.repository;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT new com.taskvantage.backend.dto.TaskSummary(t.id, t.title, t.description, t.priority, t.status, t.dueDate, t.creationDate, t.lastModifiedDate, t.startDate) " +
            "FROM Task t WHERE t.userId = :userId")
    List<TaskSummary> findTaskSummariesByUserId(Long userId);


    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.startDate = :startDate, t.status = 'In Progress' WHERE t.id = :taskId")
    void startTask(Long taskId, LocalDateTime startDate);
}

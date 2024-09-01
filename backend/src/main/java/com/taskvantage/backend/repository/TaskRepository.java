package com.taskvantage.backend.repository;

import com.taskvantage.backend.dto.TaskSummary;
import com.taskvantage.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT new com.taskvantage.backend.dto.TaskSummary(t.id, t.title, t.description, t.priority, t.status, t.dueDate, t.creationDate, t.lastModifiedDate, t.startDate, t.completionDateTime, t.duration) " +
            "FROM Task t WHERE t.userId = :userId")
    List<TaskSummary> findTaskSummariesByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.startDate = :startDate, t.status = 'In Progress' WHERE t.id = :taskId")
    void startTask(@Param("taskId") Long taskId, @Param("startDate") LocalDateTime startDate);

    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.completionDateTime = :completionDateTime, t.duration = :duration, t.status = 'Completed' WHERE t.id = :taskId")
    void completeTask(@Param("taskId") Long taskId, @Param("completionDateTime") LocalDateTime completionDateTime, @Param("duration") Duration duration);

    // New Query to Fetch Tasks that Need Notification
    @Query(value = "SELECT * FROM taskvantage.tasks WHERE scheduled_start BETWEEN :start AND :end", nativeQuery = true)
    List<Task> findTasksToNotify(@Param("start") String start, @Param("end") String end);
}

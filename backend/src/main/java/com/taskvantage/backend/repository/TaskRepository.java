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
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT new com.taskvantage.backend.dto.TaskSummary(0, 0, 0, 0, " +  // Placeholder values for summary fields
            "t.id, t.title, t.description, t.priority, t.status, " +
            "t.dueDate, t.creationDate, t.lastModifiedDate, t.scheduledStart, t.completionDateTime, t.duration, " +
            "SIZE(t.subtasks)) " +
            "FROM Task t WHERE t.userId = :userId")
    List<TaskSummary> findTaskSummariesByUserId(@Param("userId") Long userId);

    @Query("SELECT new com.taskvantage.backend.dto.TaskSummary(0, 0, 0, 0, " +  // Placeholder values for summary fields
            "t.id, t.title, t.description, t.priority, t.status, " +
            "t.dueDate, t.creationDate, t.lastModifiedDate, t.scheduledStart, t.completionDateTime, t.duration, " +
            "SIZE(t.subtasks)) " +
            "FROM Task t WHERE t.userId = :userId AND t.status != 'Completed'")
    List<TaskSummary> findNonCompletedTaskSummariesByUserId(@Param("userId") Long userId);


    @Modifying
    @Query("UPDATE Task t SET t.status = :status WHERE t.userId = :userId AND t.id = :taskId")
    void updateTaskStatus(@Param("status") String status, @Param("userId") Long userId, @Param("taskId") Long taskId);

    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.completionDateTime = :completionDateTime, t.duration = :duration, t.status = 'Completed' WHERE t.id = :taskId")
    void completeTask(@Param("taskId") Long taskId, @Param("completionDateTime") ZonedDateTime completionDateTime, @Param("duration") Duration duration);

    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND t.scheduledStart >= :startTime AND t.scheduledStart <= :endTime")
    List<Task> findTasksScheduledBetween(@Param("userId") Long userId, @Param("startTime") ZonedDateTime startTime, @Param("endTime") ZonedDateTime endTime);
}
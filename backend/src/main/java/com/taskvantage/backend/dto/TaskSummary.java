package com.taskvantage.backend.dto;

import com.taskvantage.backend.model.TaskPriority;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

public class TaskSummary {

    // Summary of all tasks, useful for dashboard overviews.
    private long totalTasks;
    private long pastDeadlineTasks;
    private long completedTasksThisMonth;
    private long totalTasksThisMonth;

    // Task-specific details
    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private String status;
    private ZonedDateTime dueDate;
    private ZonedDateTime creationDate;
    private ZonedDateTime lastModifiedDate;
    private ZonedDateTime scheduledStart;
    private ZonedDateTime completionDateTime;
    private Duration duration;
    private int totalSubtasks;

    // Provides explanation for why a task is recommended.
    private String recommendationDetails;

    // A list of other task IDs that can be batch-processed with this task.
    private List<Long> batchableWith;

    // Constructor
    public TaskSummary(long totalTasks, long pastDeadlineTasks, long completedTasksThisMonth, long totalTasksThisMonth,
                       Long id, String title, String description, TaskPriority priority, String status,
                       ZonedDateTime dueDate, ZonedDateTime creationDate, ZonedDateTime lastModifiedDate,
                       ZonedDateTime scheduledStart, ZonedDateTime completionDateTime, Duration duration,
                       int totalSubtasks, String recommendationDetails, List<Long> batchableWith) {
        this.totalTasks = totalTasks;
        this.pastDeadlineTasks = pastDeadlineTasks;
        this.completedTasksThisMonth = completedTasksThisMonth;
        this.totalTasksThisMonth = totalTasksThisMonth;
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
        this.scheduledStart = scheduledStart;
        this.completionDateTime = completionDateTime;
        this.duration = duration;
        this.totalSubtasks = totalSubtasks;
        this.recommendationDetails = recommendationDetails;
        this.batchableWith = batchableWith;
    }

    // No-argument constructor
    public TaskSummary() {
    }

    // Getters and setters
    public String getRecommendationDetails() {
        return recommendationDetails;
    }

    public void setRecommendationDetails(String recommendationDetails) {
        this.recommendationDetails = recommendationDetails;
    }

    public List<Long> getBatchableWith() {
        return batchableWith;
    }

    public void setBatchableWith(List<Long> batchableWith) {
        this.batchableWith = batchableWith;
    }

    // Getters and setters for summary data
    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public long getPastDeadlineTasks() {
        return pastDeadlineTasks;
    }

    public void setPastDeadlineTasks(long pastDeadlineTasks) {
        this.pastDeadlineTasks = pastDeadlineTasks;
    }

    public long getCompletedTasksThisMonth() {
        return completedTasksThisMonth;
    }

    public void setCompletedTasksThisMonth(long completedTasksThisMonth) {
        this.completedTasksThisMonth = completedTasksThisMonth;
    }

    public long getTotalTasksThisMonth() {
        return totalTasksThisMonth;
    }

    public void setTotalTasksThisMonth(long totalTasksThisMonth) {
        this.totalTasksThisMonth = totalTasksThisMonth;
    }

    // Getters and setters for detailed task data
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(ZonedDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public ZonedDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(ZonedDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public ZonedDateTime getCompletionDateTime() {
        return completionDateTime;
    }

    public void setCompletionDateTime(ZonedDateTime completionDateTime) {
        this.completionDateTime = completionDateTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getTotalSubtasks() {
        return totalSubtasks;
    }

    public void setTotalSubtasks(int totalSubtasks) {
        this.totalSubtasks = totalSubtasks;
    }
}
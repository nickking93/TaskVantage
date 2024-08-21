package com.taskvantage.backend.dto;

import java.time.LocalDateTime;

public class TaskSummary {
    private long totalTasks;
    private long totalSubtasks;
    private long pastDeadlineTasks;
    private long completedTasksThisMonth;
    private long totalTasksThisMonth;

    private Long id;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDateTime dueDate;
    private LocalDateTime creationDate;
    private LocalDateTime lastModifiedDate;

    // 8-argument constructor
    public TaskSummary(Long id, String title, String description, String priority, String status,
                       LocalDateTime dueDate, LocalDateTime creationDate, LocalDateTime lastModifiedDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    // No-argument constructor
    public TaskSummary() {
    }

    // Getters and setters
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public long getTotalSubtasks() {
        return totalSubtasks;
    }

    public void setTotalSubtasks(long totalSubtasks) {
        this.totalSubtasks = totalSubtasks;
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
}

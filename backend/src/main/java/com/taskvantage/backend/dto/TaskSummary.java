package com.taskvantage.backend.dto;

public class TaskSummary {
    private long totalTasks;
    private long totalSubtasks;
    private long pastDeadlineTasks;
    private long completedTasksThisMonth;
    private long totalTasksThisMonth;

    // Getters and Setters
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

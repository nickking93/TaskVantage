package com.taskvantage.backend.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status = "Pending";

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "actual_start")
    private LocalDateTime startDate;

    @Column(name = "scheduledStart")
    private LocalDateTime scheduledStart;

    @Column(name = "completionDateTime")
    private LocalDateTime completionDateTime;

    @Column(name = "duration")
    private Duration duration;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag")
    private List<String> tags;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private List<Subtask> subtasks;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_attachments", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "attachment")
    private List<String> attachments;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private List<Comment> comments;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_reminders", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "reminder")
    private List<LocalDateTime> reminders;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(name = "notify_before_start")
    private boolean notifyBeforeStart;

    @Column(name = "notification_sent")
    private Boolean notificationSent;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public LocalDateTime getCompletionDateTime() {
        return completionDateTime;
    }

    public void setCompletionDateTime(LocalDateTime completionDateTime) {
        this.completionDateTime = completionDateTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    // New method to calculate the total number of subtasks
    public int getTotalSubtasks() {
        return subtasks != null ? subtasks.size() : 0;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public List<LocalDateTime> getReminders() {
        return reminders;
    }

    public void setReminders(List<LocalDateTime> reminders) {
        this.reminders = reminders;
    }

    public boolean isNotifyBeforeStart() {
        return notifyBeforeStart;
    }

    public void setNotifyBeforeStart(boolean notifyBeforeStart) {
        this.notifyBeforeStart = notifyBeforeStart;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}
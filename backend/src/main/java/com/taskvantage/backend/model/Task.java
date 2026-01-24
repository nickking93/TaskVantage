package com.taskvantage.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_is_recommended", columnList = "is_recommended"),
                @Index(name = "idx_recommendation_score", columnList = "recommendation_score")
        }
)
public class Task {

    public static final String STATUS_COMPLETED = "Completed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Column(nullable = false)
    private String status = "Pending";

    // Store date and time as ZonedDateTime (in UTC)
    @Column(name = "due_date")
    private ZonedDateTime dueDate;

    @Column(name = "creation_date", nullable = false)
    private ZonedDateTime creationDate = ZonedDateTime.now(ZoneOffset.UTC); // Default to UTC

    @Column(name = "last_modified_date")
    private ZonedDateTime lastModifiedDate;

    @Column(name = "actual_start")
    private ZonedDateTime startDate;

    @Column(name = "scheduled_start")
    private ZonedDateTime scheduledStart;

    @Column(name = "completionDateTime")
    private ZonedDateTime completionDateTime;

    @Column(name = "duration")
    private Duration duration;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag")
    private List<String> tags;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private List<Subtask> subtasks = new ArrayList<>();  // Initialize with an empty list

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_attachments", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "attachment")
    private List<String> attachments;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private List<Comment> comments = new ArrayList<>();  // Initialize with an empty list

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_reminders", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "reminder")
    private List<ZonedDateTime> reminders;  // Store reminders as ZonedDateTime

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(name = "notify_before_start")
    private boolean notifyBeforeStart;

    @Column(name = "notification_sent")
    private Boolean notificationSent;

    @Column(name = "google_calendar_event_id")
    private String googleCalendarEventId;

    @Column(name = "is_all_day")
    private Boolean isAllDay = false;

    // Score representing how relevant a task is as a recommendation (0.0 to 1.0).
    @Column(name = "recommendation_score")
    @Min(0) @Max(1)
    private Float recommendationScore;

    // Identifies the method or source used for the recommendation (e.g., CollaborativeFiltering, NLP).
    @Column(name = "recommended_by")
    private String recommendedBy;

    // Indicates whether the task is currently recommended.
    @Column(name = "is_recommended")
    private Boolean isRecommended = false;

    // Timestamp for when this task was last recommended.
    @Column(name = "last_recommended_on")
    private ZonedDateTime lastRecommendedOn;

    @Transient
    private String recommendationReason;

    // Getters and Setters
    public ZonedDateTime getLastRecommendedOn() {
        return lastRecommendedOn;
    }

    public void setLastRecommendedOn(ZonedDateTime lastRecommendedOn) {
        this.lastRecommendedOn = lastRecommendedOn;
    }

    public Boolean getRecommended() {
        return isRecommended;
    }

    public void setRecommended(Boolean recommended) {
        isRecommended = recommended;
    }

    public String getRecommendedBy() {
        return recommendedBy;
    }

    public void setRecommendedBy(String recommendedBy) {
        this.recommendedBy = recommendedBy;
    }

    public Float getRecommendationScore() {
        return recommendationScore;
    }

    public void setRecommendationScore(Float recommendationScore) {
        if (recommendationScore < 0 || recommendationScore > 1) {
            throw new IllegalArgumentException("Recommendation score must be between 0.0 and 1.0.");
        }
        this.recommendationScore = recommendationScore;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public Boolean isAllDay() {
        return isAllDay;
    }

    public void setIsAllDay(Boolean isAllDay) {
        this.isAllDay = isAllDay;
    }

    public String getGoogleCalendarEventId() {
        return googleCalendarEventId;
    }

    public void setGoogleCalendarEventId(String googleCalendarEventId) {
        this.googleCalendarEventId = googleCalendarEventId;
    }

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

    public static boolean isCompletedStatus(String status) {
        return STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(ZonedDateTime dueDate) {
        if (dueDate != null) {
            this.dueDate = dueDate.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.dueDate = null;  // Handle null case explicitly
        }
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate.withZoneSameInstant(ZoneOffset.UTC);
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate.withZoneSameInstant(ZoneOffset.UTC);
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startDate) {
        if (startDate != null) {
            this.startDate = startDate.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.startDate = null;  // Handle null case explicitly
        }
    }

    public ZonedDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(ZonedDateTime scheduledStart) {
        if (scheduledStart != null) {
            this.scheduledStart = scheduledStart.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.scheduledStart = null;  // Handle null case explicitly
        }
    }

    public ZonedDateTime getCompletionDateTime() {
        return completionDateTime;
    }

    public void setCompletionDateTime(ZonedDateTime completionDateTime) {
        if (completionDateTime != null) {
            this.completionDateTime = completionDateTime.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.completionDateTime = null;  // Handle null case explicitly
        }
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

    public List<ZonedDateTime> getReminders() {
        return reminders;
    }

    public void setReminders(List<ZonedDateTime> reminders) {
        this.reminders = reminders;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
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

    public Task(){}

    public Task(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) &&
                Objects.equals(title, task.title) &&
                Objects.equals(description, task.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description);
    }
}

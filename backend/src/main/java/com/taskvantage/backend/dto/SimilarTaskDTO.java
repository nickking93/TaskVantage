package com.taskvantage.backend.dto;

import com.taskvantage.backend.model.Task;

/**
 * DTO for returning similar tasks with their similarity scores.
 */
public class SimilarTaskDTO {
    private Task task;
    private double similarityScore;
    private String reason;

    public SimilarTaskDTO() {}

    public SimilarTaskDTO(Task task, double similarityScore, String reason) {
        this.task = task;
        this.similarityScore = similarityScore;
        this.reason = reason;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

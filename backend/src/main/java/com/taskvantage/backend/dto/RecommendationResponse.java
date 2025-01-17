package com.taskvantage.backend.dto;

import com.taskvantage.backend.model.Task;
import java.util.List;

public class RecommendationResponse {
    private String status;
    private String message;
    private List<Task> recommendations;

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Task> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Task> recommendations) {
        this.recommendations = recommendations;
    }
}
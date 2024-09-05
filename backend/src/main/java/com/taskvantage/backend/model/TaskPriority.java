package com.taskvantage.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskPriority {
    HIGH, MEDIUM, LOW;

    // This method allows case-insensitive deserialization of the enum
    @JsonCreator
    public static TaskPriority fromString(String value) {
        return TaskPriority.valueOf(value.toUpperCase());
    }

    // Optionally, you can customize the serialization if needed
    @JsonValue
    public String toValue() {
        return this.name();
    }
}
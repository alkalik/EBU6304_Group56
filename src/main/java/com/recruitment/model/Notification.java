package com.recruitment.model;

import java.time.LocalDateTime;

public class Notification {
    public enum Type {
        APPLICATION_STATUS_UPDATE,
        POSITION_EXPIRATION,
        WITHDRAWAL_SUCCESS,
        NEW_APPLICATION
    }

    private String id;
    private String userId;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
    private Type type;

    public Notification() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(String id, String userId, String message, Type type) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
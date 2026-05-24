package com.recruitment.model;

import java.time.LocalDateTime;

/**
 * In-app notification delivered to a specific user.
 * <p>
 * Each notification has a {@link Type} for UI categorisation, a human-readable
 * {@code message}, creation {@code timestamp}, and read/unread flag.
 * Persisted via the notification service data store.
 */
public class Notification {

    /**
     * Semantic category used to style or route notifications in the UI.
     */
    public enum Type {
        /** Applicant informed of an application status change. */
        APPLICATION_STATUS_UPDATE,
        /** Job posting has expired or is about to expire. */
        POSITION_EXPIRATION,
        /** Confirmation that an application withdrawal succeeded. */
        WITHDRAWAL_SUCCESS,
        /** Module organiser alerted to a new application submission. */
        NEW_APPLICATION,
        /** Department-wide or broadcast announcement. */
        ANNOUNCEMENT,
        /** General informational message. */
        INFO
    }

    private String id;
    private String userId;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
    private Type type;

    /**
     * Creates an unread notification with the current timestamp.
     */
    public Notification() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    /**
     * Creates a notification with the given content and type.
     *
     * @param id      unique notification identifier
     * @param userId  recipient user identifier
     * @param message body text shown in the notification panel
     * @param type    semantic category for display and filtering
     */
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

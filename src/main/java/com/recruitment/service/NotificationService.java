package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Notification;
import com.recruitment.model.User;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for in-app user notifications.
 * <p>
 * Supports single-user delivery, broadcast to all users, and targeted delivery by role.
 * Notifications are ordered by timestamp (newest first) when queried per user.
 * </p>
 * <p>
 * Data is persisted in {@code data/notifications.json} via {@link JsonUtil}. An in-memory
 * list is loaded at construction and written back on each mutating operation.
 * </p>
 * <p>
 * Broadcast and role-based methods require a {@link UserService} reference; without it,
 * recipient lists are empty and no notifications are created.
 * </p>
 */
public class NotificationService {
    private static final String FILE_NAME = "notifications.json";
    private static final Type LIST_TYPE = new TypeToken<List<Notification>>() {}.getType();

    private List<Notification> notifications;
    private UserService userService;

    /**
     * Loads notifications from JSON without a {@link UserService} (broadcast/role features disabled).
     */
    public NotificationService() {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    /**
     * Loads notifications from JSON and associates a {@link UserService} for broadcast and role targeting.
     *
     * @param userService the user service used to resolve recipient IDs
     */
    public NotificationService(UserService userService) {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        this.userService = userService;
    }

    /**
     * Reloads notification data from the JSON file, discarding unsaved in-memory changes.
     */
    public void reload() {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, notifications);
    }

    /**
     * Creates and persists a notification for a single user.
     *
     * @param userId  the recipient user ID
     * @param message the notification body text
     * @param type    the {@link Notification.Type} category
     * @return the created {@link Notification} with a generated ID and timestamp
     */
    public Notification createNotification(String userId, String message, Notification.Type type) {
        Notification notification = new Notification(
                IDGenerator.generate("NOT"),
                userId,
                message,
                type
        );
        notifications.add(notification);
        save();
        return notification;
    }

    /**
     * Sends the same notification to every registered user.
     * <p>
     * Requires {@link UserService} to be available; does nothing if no users can be resolved.
     * </p>
     *
     * @param message the notification body text
     * @param type    the {@link Notification.Type} category
     */
    public void broadcastNotification(String message, Notification.Type type) {
        List<String> allUserIds = getAllUserIds();
        for (String userId : allUserIds) {
            createNotification(userId, message, type);
        }
    }

    /**
     * Sends the same notification to all users with the given role.
     * <p>
     * Requires {@link UserService} to be available; does nothing if no matching users exist.
     * </p>
     *
     * @param message the notification body text
     * @param type    the {@link Notification.Type} category
     * @param role    the target {@link User.Role}
     */
    public void notifyUsersByRole(String message, Notification.Type type, User.Role role) {
        List<String> userIds = getUserIdsByRole(role);
        for (String userId : userIds) {
            createNotification(userId, message, type);
        }
    }

    private List<String> getAllUserIds() {
        if (userService != null) {
            return userService.getAllUsers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getUserIdsByRole(User.Role role) {
        if (userService != null) {
            return userService.getAllUsers().stream()
                    .filter(user -> user.getRole() == role)
                    .map(User::getId)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Returns all notifications for a user, sorted newest first by timestamp.
     *
     * @param userId the recipient user ID
     * @return a sorted list of notifications (may be empty)
     */
    public List<Notification> getNotificationsByUser(String userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    /**
     * Returns unread notifications for a user, sorted newest first.
     *
     * @param userId the recipient user ID
     * @return a sorted list of unread notifications (may be empty)
     */
    public List<Notification> getUnreadNotificationsByUser(String userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    /**
     * Marks a single notification as read and reloads from disk.
     *
     * @param notificationId the notification ID
     * @return {@code true} if found and marked read; {@code false} if not found
     */
    public boolean markAsRead(String notificationId) {
        Optional<Notification> notification = findById(notificationId);
        if (notification.isPresent()) {
            notification.get().setRead(true);
            save();
            reload();
        return true;
        }
        return false;
    }

    /**
     * Marks all unread notifications for a user as read.
     *
     * @param userId the recipient user ID
     */
    public void markAllAsRead(String userId) {
        notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .forEach(n -> n.setRead(true));
        save();
    }

    /**
     * Removes all read notifications for a user and reloads from disk.
     *
     * @param userId the recipient user ID
     */
    public void clearReadNotifications(String userId) {
        notifications.removeIf(n -> n.getUserId().equals(userId) && n.isRead());
        save();
        reload();
    }

    /**
     * Finds a notification by unique identifier.
     *
     * @param id the notification ID (e.g. {@code NOT-...})
     * @return an {@link Optional} containing the notification if found, or empty otherwise
     */
    public Optional<Notification> findById(String id) {
        return notifications.stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    /**
     * Counts unread notifications for a user.
     *
     * @param userId the recipient user ID
     * @return the number of unread notifications
     */
    public int getUnreadCount(String userId) {
        return (int) notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .count();
    }
}

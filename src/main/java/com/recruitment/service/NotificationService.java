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

public class NotificationService {
    private static final String FILE_NAME = "notifications.json";
    private static final Type LIST_TYPE = new TypeToken<List<Notification>>() {}.getType();

    private List<Notification> notifications;
    private UserService userService;

    public NotificationService() {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    public NotificationService(UserService userService) {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        this.userService = userService;
    }

    public void reload() {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, notifications);
    }

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

    // Broadcast notification to all users (e.g. admin announcements)
    public void broadcastNotification(String message, Notification.Type type) {
        List<String> allUserIds = getAllUserIds();
        for (String userId : allUserIds) {
            createNotification(userId, message, type);
        }
    }

    // Notify users by role
    public void notifyUsersByRole(String message, Notification.Type type, User.Role role) {
        List<String> userIds = getUserIdsByRole(role);
        for (String userId : userIds) {
            createNotification(userId, message, type);
        }
    }

    // Helper methods
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

    public List<Notification> getNotificationsByUser(String userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    public List<Notification> getUnreadNotificationsByUser(String userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

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

    public void markAllAsRead(String userId) {
        notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .forEach(n -> n.setRead(true));
        save();
    }

    public void clearReadNotifications(String userId) {
        notifications.removeIf(n -> n.getUserId().equals(userId) && n.isRead());
        save();
        reload();
    }

    public Optional<Notification> findById(String id) {
        return notifications.stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    public int getUnreadCount(String userId) {
        return (int) notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .count();
    }
}
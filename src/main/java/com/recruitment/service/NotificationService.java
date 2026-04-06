package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Notification;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NotificationService {
    private static final String FILE_NAME = "notifications.json";
    private static final Type LIST_TYPE = new TypeToken<List<Notification>>() {}.getType();

    private List<Notification> notifications;

    public NotificationService() {
        this.notifications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
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
package com.recruitment.service;

import com.recruitment.model.Notification;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class NotificationServiceTest {
    private NotificationService notificationService;

    @Before
    public void setUp() {
        new File("data").mkdirs();
        notificationService = new NotificationService();
    }

    @Test
    public void testCreateNotificationAndUnreadCount() {
        String userId = "USR-notify-" + System.currentTimeMillis();

        Notification notification = notificationService.createNotification(
                userId,
                "A new application has arrived.",
                Notification.Type.NEW_APPLICATION
        );

        assertNotNull(notification);
        assertNotNull(notification.getId());
        assertEquals(userId, notification.getUserId());
        assertEquals(Notification.Type.NEW_APPLICATION, notification.getType());
        assertFalse(notification.isRead());
        assertTrue(notificationService.getUnreadCount(userId) >= 1);
    }

    @Test
    public void testMarkAsRead() {
        String userId = "USR-read-" + System.currentTimeMillis();
        Notification notification = notificationService.createNotification(
                userId,
                "Your status has changed.",
                Notification.Type.APPLICATION_STATUS_UPDATE
        );

        assertTrue(notificationService.markAsRead(notification.getId()));

        Notification updated = notificationService.findById(notification.getId()).orElse(null);
        assertNotNull(updated);
        assertTrue(updated.isRead());
        assertEquals(0, notificationService.getUnreadCount(userId));
    }

    @Test
    public void testClearReadNotificationsOnlyRemovesReadOnes() {
        String userId = "USR-clear-" + System.currentTimeMillis();
        Notification readNotification = notificationService.createNotification(
                userId,
                "Read me",
                Notification.Type.NEW_APPLICATION
        );
        Notification unreadNotification = notificationService.createNotification(
                userId,
                "Keep me",
                Notification.Type.POSITION_EXPIRATION
        );

        assertTrue(notificationService.markAsRead(readNotification.getId()));
        notificationService.clearReadNotifications(userId);

        List<Notification> remaining = notificationService.getNotificationsByUser(userId);
        assertEquals(1, remaining.size());
        assertEquals(unreadNotification.getId(), remaining.get(0).getId());
        assertFalse(remaining.get(0).isRead());
    }

    @Test
    public void testMarkAllAsRead() {
        String userId = "USR-all-read-" + System.currentTimeMillis();
        notificationService.createNotification(userId, "One", Notification.Type.NEW_APPLICATION);
        notificationService.createNotification(userId, "Two", Notification.Type.WITHDRAWAL_SUCCESS);

        notificationService.markAllAsRead(userId);

        assertEquals(0, notificationService.getUnreadCount(userId));
        assertTrue(notificationService.getNotificationsByUser(userId).stream().allMatch(Notification::isRead));
    }
}

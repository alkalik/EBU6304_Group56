package com.recruitment.service;

import com.recruitment.model.Notification;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NotificationService} creation, read state, unread counts, and bulk clear operations.
 */
public class NotificationServiceTest {
    private NotificationService notificationService;

    @Before
    public void setUp() {
        new File("data").mkdirs();
        notificationService = new NotificationService();
    }

    /**
     * Verifies that creating a notification sets fields correctly and increases the user's unread count.
     */
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

    /**
     * Verifies that marking a notification as read updates its state and clears the unread count for that user.
     */
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

    /**
     * Verifies that {@code clearReadNotifications} removes only read notifications for a user.
     */
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

    /**
     * Verifies that {@code markAllAsRead} sets all notifications read and zeroes the unread count.
     */
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

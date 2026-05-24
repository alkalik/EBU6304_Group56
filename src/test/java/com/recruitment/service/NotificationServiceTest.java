package com.recruitment.service;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.recruitment.model.Notification;

public class NotificationServiceTest extends ServiceTestBase {
    private NotificationService notificationService;

    // Initializes the NotificationService and ensures the dynamic storage directory exists before every test run
    @Before
    public void setUp() {
        notificationService = newServiceGraph().notificationService;
    }

    // Verifies that a notification can be successfully created with an unread status, increasing the unread count
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

    // Ensures that an individual notification can be flagged as read, which subsequently drops the user's unread counter
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

    // Validates that cleaning up history deletes only the read alerts while leaving all unread alerts completely untouched
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

    // Assures that a user can perform a bulk update action to instantly mark all of their incoming alerts as read
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

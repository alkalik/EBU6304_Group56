package com.recruitment.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ModelDefaultsTest {
    @Test
    public void testUserDefaults() {
        User user = new User();

        assertNotNull(user.getSkills());
        assertTrue(user.getSkills().isEmpty());
    }

    @Test
    public void testJobDefaults() {
        Job job = new Job();

        assertNotNull(job.getRequiredSkills());
        assertTrue(job.getRequiredSkills().isEmpty());
        assertEquals(Job.Status.OPEN, job.getStatus());
        assertEquals(0, job.getFilledPositions());
    }

    @Test
    public void testApplicationDefaults() {
        Application application = new Application();

        assertEquals(Application.Status.PENDING, application.getStatus());
    }

    @Test
    public void testNotificationDefaults() {
        Notification notification = new Notification();

        assertNotNull(notification.getTimestamp());
        assertFalse(notification.isRead());
    }
}

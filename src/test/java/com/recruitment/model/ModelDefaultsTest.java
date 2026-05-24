package com.recruitment.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for default field values on domain model classes after construction.
 */
public class ModelDefaultsTest {

    /**
     * Verifies that a newly created {@link User} has a non-null, empty skills list.
     */
    @Test
    public void testUserDefaults() {
        User user = new User();

        assertNotNull(user.getSkills());
        assertTrue(user.getSkills().isEmpty());
    }

    /**
     * Verifies that a newly created {@link Job} has an empty required-skills list,
     * {@link Job.Status#OPEN} status, and zero filled positions.
     */
    @Test
    public void testJobDefaults() {
        Job job = new Job();

        assertNotNull(job.getRequiredSkills());
        assertTrue(job.getRequiredSkills().isEmpty());
        assertEquals(Job.Status.OPEN, job.getStatus());
        assertEquals(0, job.getFilledPositions());
    }

    /**
     * Verifies that a newly created {@link Application} defaults to {@link Application.Status#PENDING}.
     */
    @Test
    public void testApplicationDefaults() {
        Application application = new Application();

        assertEquals(Application.Status.PENDING, application.getStatus());
    }

    /**
     * Verifies that a newly created {@link Notification} has a non-null timestamp and is unread.
     */
    @Test
    public void testNotificationDefaults() {
        Notification notification = new Notification();

        assertNotNull(notification.getTimestamp());
        assertFalse(notification.isRead());
    }
}

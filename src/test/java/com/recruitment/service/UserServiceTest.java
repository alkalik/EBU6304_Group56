package com.recruitment.service;

import com.recruitment.model.User;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class UserServiceTest {
    private UserService userService;

    @Before
    public void setUp() {
        // Ensure clean data directory for tests
        new File("data").mkdirs();
        userService = new UserService();
    }

    @Test
    public void testRegisterAndAuthenticate() {
        User user = new User(null, "testuser_" + System.currentTimeMillis(), "pass123",
                User.Role.TA, "Test User", "test@example.com");
        boolean registered = userService.register(user);
        assertTrue("User should be registered successfully", registered);
        assertNotNull("User should have an ID after registration", user.getId());

        User authenticated = userService.authenticate(user.getUsername(), "pass123");
        assertNotNull("Authentication should succeed with correct credentials", authenticated);
        assertEquals(user.getUsername(), authenticated.getUsername());

        User failAuth = userService.authenticate(user.getUsername(), "wrongpass");
        assertNull("Authentication should fail with wrong password", failAuth);
    }

    @Test
    public void testDuplicateRegistration() {
        String username = "dupuser_" + System.currentTimeMillis();
        User user1 = new User(null, username, "pass1", User.Role.TA, "User1", "u1@test.com");
        User user2 = new User(null, username, "pass2", User.Role.MO, "User2", "u2@test.com");

        assertTrue(userService.register(user1));
        assertFalse("Duplicate username should fail", userService.register(user2));
    }

    @Test
    public void testFindByRole() {
        String suffix = "_" + System.currentTimeMillis();
        userService.register(new User(null, "ta" + suffix, "p", User.Role.TA, "TA", "ta@test.com"));
        userService.register(new User(null, "mo" + suffix, "p", User.Role.MO, "MO", "mo@test.com"));

        List<User> tas = userService.findByRole(User.Role.TA);
        assertFalse("Should find at least one TA", tas.isEmpty());
        assertTrue(tas.stream().allMatch(u -> u.getRole() == User.Role.TA));
    }

    @Test
    public void testUpdateUser() {
        User user = new User(null, "upd_" + System.currentTimeMillis(), "pass",
                User.Role.TA, "Original", "orig@test.com");
        userService.register(user);

        user.setName("Updated Name");
        user.setPhone("1234567890");
        assertTrue(userService.updateUser(user));

        User found = userService.findById(user.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Updated Name", found.getName());
        assertEquals("1234567890", found.getPhone());
    }

    @Test
    public void testDeleteUser() {
        User user = new User(null, "del_" + System.currentTimeMillis(), "pass",
                User.Role.TA, "ToDelete", "del@test.com");
        userService.register(user);

        assertTrue(userService.deleteUser(user.getId()));
        assertFalse(userService.findById(user.getId()).isPresent());
    }
}

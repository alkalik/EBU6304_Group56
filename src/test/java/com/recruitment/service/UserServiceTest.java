package com.recruitment.service;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.recruitment.model.User;

public class UserServiceTest {
    private UserService userService;

    // Initializes the UserService instance and ensures the test data storage directory exists before every test run
    @Before
    public void setUp() {
        // Ensure clean data directory for tests
        new File("data").mkdirs();
        userService = new UserService();
    }

    // Verifies successful user registration and checks that authentication passes with valid credentials but fails with invalid ones
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

    // Ensures that the system rejects new registrations that attempt to use an already taken username
    @Test
    public void testDuplicateRegistration() {
        String username = "dupuser_" + System.currentTimeMillis();
        User user1 = new User(null, username, "pass1", User.Role.TA, "User1", "u1@test.com");
        User user2 = new User(null, username, "pass2", User.Role.MO, "User2", "u2@test.com");

        assertTrue(userService.register(user1));
        assertFalse("Duplicate username should fail", userService.register(user2));
    }

    // Verifies that users can be retrieved and filtered properly from the persistent store based on their functional role
    @Test
    public void testFindByRole() {
        String suffix = "_" + System.currentTimeMillis();
        userService.register(new User(null, "ta" + suffix, "p", User.Role.TA, "TA", "ta@test.com"));
        userService.register(new User(null, "mo" + suffix, "p", User.Role.MO, "MO", "mo@test.com"));

        List<User> tas = userService.findByRole(User.Role.TA);
        assertFalse("Should find at least one TA", tas.isEmpty());
        assertTrue(tas.stream().allMatch(u -> u.getRole() == User.Role.TA));
    }

    // Validates that an existing user's profile attributes can be successfully updated and saved in the repository
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

    // Assures that a user record can be deleted by its ID and will no longer be discoverable in subsequent lookups
    @Test
    public void testDeleteUser() {
        User user = new User(null, "del_" + System.currentTimeMillis(), "pass",
                User.Role.TA, "ToDelete", "del@test.com");
        userService.register(user);

        assertTrue(userService.deleteUser(user.getId()));
        assertFalse(userService.findById(user.getId()).isPresent());
    }

    // Checks that the universal search functionality can locate users by matching strings against name, department, or role
    @Test
    public void testSearchUsers() {
        String suffix = "_" + System.currentTimeMillis();

        User ta = new User(null, "search_ta" + suffix, "pass",
                User.Role.TA, "Alice Search", "alice" + suffix + "@test.com");
        ta.setDepartment("Computer Science");
        assertTrue(userService.register(ta));

        User mo = new User(null, "search_mo" + suffix, "pass",
                User.Role.MO, "Bob Review", "bob" + suffix + "@test.com");
        mo.setDepartment("Mathematics");
        assertTrue(userService.register(mo));

        List<User> byName = userService.searchUsers("alice search");
        assertTrue(byName.stream().anyMatch(u -> u.getId().equals(ta.getId())));

        List<User> byDepartment = userService.searchUsers("mathematics");
        assertTrue(byDepartment.stream().anyMatch(u -> u.getId().equals(mo.getId())));

        List<User> byRole = userService.searchUsers("mo");
        assertTrue(byRole.stream().anyMatch(u -> u.getId().equals(mo.getId())));
    }
}
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
        String suffix = String.valueOf(System.currentTimeMillis());
        User user = new User(null, "testuser_" + suffix, "pass123",
                User.Role.TA, "Test User", "test_" + suffix + "@example.com");
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
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "dupuser_" + suffix;
        User user1 = new User(null, username, "pass1", User.Role.TA, "User1", "u1_" + suffix + "@test.com");
        User user2 = new User(null, username, "pass2", User.Role.MO, "User2", "u2_" + suffix + "@test.com");

        assertTrue(userService.register(user1));
        assertFalse("Duplicate username should fail", userService.register(user2));
    }

    @Test
    public void testDuplicateEmailRegistration() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "shared_" + suffix + "@test.com";
        User user1 = new User(null, "user_a_" + suffix, "pass1", User.Role.TA, "A", email);
        User user2 = new User(null, "user_b_" + suffix, "pass2", User.Role.TA, "B", email);

        assertTrue(userService.register(user1));
        assertFalse("Duplicate email should fail", userService.register(user2));
    }

    @Test
    public void testFindByRole() {
        String suffix = "_" + System.currentTimeMillis();
        userService.register(new User(null, "ta" + suffix, "p", User.Role.TA, "TA", "ta" + suffix + "@test.com"));
        userService.register(new User(null, "mo" + suffix, "p", User.Role.MO, "MO", "mo" + suffix + "@test.com"));

        List<User> tas = userService.findByRole(User.Role.TA);
        assertFalse("Should find at least one TA", tas.isEmpty());
        assertTrue(tas.stream().allMatch(u -> u.getRole() == User.Role.TA));
    }

    @Test
    public void testUpdateUser() {
        String suffix = String.valueOf(System.currentTimeMillis());
        User user = new User(null, "upd_" + suffix, "pass",
                User.Role.TA, "Original", "orig_" + suffix + "@test.com");
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
        String suffix = String.valueOf(System.currentTimeMillis());
        User user = new User(null, "del_" + suffix, "pass",
                User.Role.TA, "ToDelete", "del_" + suffix + "@test.com");
        userService.register(user);

        assertTrue(userService.deleteUser(user.getId()));
        assertFalse(userService.findById(user.getId()).isPresent());
    }

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

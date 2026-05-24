package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.User;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;
import com.recruitment.util.PasswordUtil;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for user account management.
 * <p>
 * Handles authentication, registration, profile updates, role-based lookup,
 * keyword search, and deletion. Passwords are stored using {@link PasswordUtil}
 * hashing; legacy plaintext passwords are migrated automatically on load and login.
 * </p>
 * <p>
 * Data is persisted in {@code data/users.json} via {@link JsonUtil}. An in-memory
 * list is loaded at construction and written back on each mutating operation.
 * When the store is empty, default demo users (admin, MOs, TAs) are seeded.
 * </p>
 */
public class UserService {
    private static final String FILE_NAME = "users.json";
    private static final Type LIST_TYPE = new TypeToken<List<User>>() {}.getType();

    private List<User> users;

    /**
     * Loads users from JSON, migrates legacy plaintext passwords, and seeds defaults if empty.
     */
    public UserService() {
        this.users = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        migrateLegacyPlaintextPasswordsIfNeeded();
        seedDefaultUsersIfEmpty();
    }

    /**
     * Reloads user data from the JSON file, discarding unsaved in-memory changes.
     * Re-runs password migration and default-user seeding when applicable.
     */
    public void reload() {
        this.users = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        migrateLegacyPlaintextPasswordsIfNeeded();
        seedDefaultUsersIfEmpty();
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, users);
    }

    /**
     * Authenticates a user by username and password.
     * <p>
     * On successful login, legacy plaintext passwords are upgraded to a hash and saved.
     * </p>
     *
     * @param username the login username (case-sensitive match)
     * @param password the plain-text password to verify
     * @return the matching {@link User} if credentials are valid; {@code null} if not found or password mismatch
     */
    public User authenticate(String username, String password) {
        Optional<User> userOpt = users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
        if (!userOpt.isPresent()) return null;

        User user = userOpt.get();
        String stored = user.getPassword();
        if (PasswordUtil.matches(password, stored)) {
            // Auto-upgrade legacy plaintext passwords on successful login
            if (!PasswordUtil.isHashed(stored)) {
                user.setPassword(PasswordUtil.hashPassword(password));
                save();
            }
            return user;
        }
        return null;
    }

    /**
     * Registers a new user if the username is not already taken.
     * <p>
     * Assigns a new {@code USR-} prefixed ID and hashes the password before persistence.
     * </p>
     *
     * @param user the user to register; must have a unique {@link User#getUsername()}
     * @return {@code true} if registration succeeded; {@code false} if the username already exists
     */
    public boolean register(User user) {
        if (users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()))) {
            return false;
        }
        user.setId(IDGenerator.generate("USR"));
        user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        users.add(user);
        save();
        return true;
    }

    /**
     * Updates an existing user by matching on {@link User#getId()}.
     *
     * @param user the user record with updated fields; ID must match an existing entry
     * @return {@code true} if the user was found and updated; {@code false} if no matching ID exists
     */
    public boolean updateUser(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                save();
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a user by unique identifier.
     *
     * @param id the user ID (e.g. {@code USR-...})
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    public Optional<User> findById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    /**
     * Finds a user by login username.
     *
     * @param username the username to look up
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    /**
     * Returns all users with the given role.
     *
     * @param role the {@link User.Role} to filter by
     * @return a new list of users with that role (may be empty)
     */
    public List<User> findByRole(User.Role role) {
        return users.stream().filter(u -> u.getRole() == role).collect(Collectors.toList());
    }

    /**
     * Searches users by keyword across username, name, email, department, and role name.
     * <p>
     * Matching is case-insensitive and uses substring containment. A {@code null} or
     * blank keyword returns all users.
     * </p>
     *
     * @param keyword the search term; may be {@code null}
     * @return a filtered list of matching users (never {@code null})
     */
    public List<User> searchUsers(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return users.stream()
                .filter(u -> normalizedKeyword.isEmpty()
                        || containsIgnoreCase(u.getUsername(), normalizedKeyword)
                        || containsIgnoreCase(u.getName(), normalizedKeyword)
                        || containsIgnoreCase(u.getEmail(), normalizedKeyword)
                        || containsIgnoreCase(u.getDepartment(), normalizedKeyword)
                        || (u.getRole() != null && u.getRole().name().toLowerCase().contains(normalizedKeyword)))
                .collect(Collectors.toList());
    }

    /**
     * Returns the live in-memory list of all users.
     * <p>
     * Callers should treat the returned list as the service's working copy; mutating it
     * without going through service methods may bypass persistence.
     * </p>
     *
     * @return the internal list of all users (never {@code null})
     */
    public List<User> getAllUsers() {
        return users;
    }

    /**
     * Permanently removes a user by ID.
     *
     * @param id the user ID to delete
     * @return {@code true} if a user was removed; {@code false} if the ID was not found
     */
    public boolean deleteUser(String id) {
        boolean removed = users.removeIf(u -> u.getId().equals(id));
        if (removed) save();
        return removed;
    }

    private void migrateLegacyPlaintextPasswordsIfNeeded() {
        boolean changed = false;
        for (User u : users) {
            String p = u.getPassword();
            if (p != null && !PasswordUtil.isHashed(p)) {
                u.setPassword(PasswordUtil.hashPassword(p));
                changed = true;
            }
        }
        if (changed) save();
    }

    private void seedDefaultUsersIfEmpty() {
        if (users != null && !users.isEmpty()) return;

        User admin = new User(null, "admin", "admin123", User.Role.ADMIN, "System Admin", "admin@bupt.edu.cn");
        admin.setPhone("010-12345678");
        admin.setDepartment("International School");

        User mo1 = new User(null, "mo1", "mo123", User.Role.MO, "Dr. Zhang Wei", "zhangwei@bupt.edu.cn");
        mo1.setPhone("010-87654321");
        mo1.setDepartment("Computer Science");

        User mo2 = new User(null, "mo2", "mo123", User.Role.MO, "Dr. Li Ming", "liming@bupt.edu.cn");
        mo2.setPhone("010-11223344");
        mo2.setDepartment("Electronic Engineering");

        User ta1 = new User(null, "ta1", "ta123", User.Role.TA, "Wang Xiaoming", "wxm@bupt.edu.cn");
        ta1.setPhone("13800138001");
        ta1.setDepartment("Computer Science");
        ta1.setSkills(java.util.Arrays.asList("Java", "Python", "Agile"));

        User ta2 = new User(null, "ta2", "ta123", User.Role.TA, "Chen Lei", "chenlei@bupt.edu.cn");
        ta2.setPhone("13800138002");
        ta2.setDepartment("Computer Science");
        ta2.setSkills(java.util.Arrays.asList("C++", "Data Structures", "Algorithms"));

        register(admin);
        register(mo1);
        register(mo2);
        register(ta1);
        register(ta2);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}

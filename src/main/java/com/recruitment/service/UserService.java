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

public class UserService {
    private static final String FILE_NAME = "users.json";
    private static final Type LIST_TYPE = new TypeToken<List<User>>() {}.getType();

    private List<User> users;

    public UserService() {
        this.users = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        migrateLegacyPlaintextPasswordsIfNeeded();
        seedDefaultUsersIfEmpty();
    }

    public void reload() {
        this.users = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        migrateLegacyPlaintextPasswordsIfNeeded();
        seedDefaultUsersIfEmpty();
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, users);
    }

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

    public Optional<User> findById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    public List<User> findByRole(User.Role role) {
        return users.stream().filter(u -> u.getRole() == role).collect(Collectors.toList());
    }

    public List<User> getAllUsers() {
        return users;
    }

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
}

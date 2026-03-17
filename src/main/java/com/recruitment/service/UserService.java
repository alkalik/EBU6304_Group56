package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.User;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

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
    }

    public void reload() {
        this.users = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, users);
    }

    public User authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    public boolean register(User user) {
        if (users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()))) {
            return false;
        }
        user.setId(IDGenerator.generate("USR"));
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
}

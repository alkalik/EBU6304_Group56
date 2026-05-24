package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a registered user in the recruitment system.
 * <p>
 * Users may be teaching assistants ({@link Role#TA}), module organisers ({@link Role#MO}),
 * or administrators ({@link Role#ADMIN}). TA-specific profile data includes skills,
 * CV file path, and contact details; MO users may have an associated department.
 * Credentials and identifiers are persisted via {@code users.json}.
 */
public class User {

    /**
     * System role that determines which features and views a user may access.
     */
    public enum Role {
        /** Teaching assistant who browses jobs and submits applications. */
        TA,
        /** Module organiser who posts jobs and reviews applications. */
        MO,
        /** System administrator with elevated management capabilities. */
        ADMIN
    }

    private String id;
    private String username;
    private String password;
    private Role role;
    private String name;
    private String email;
    private String phone;
    private List<String> skills;
    private String cvPath;
    private String department;

    /** Creates an empty user with an empty skills list. */
    public User() {
        this.skills = new ArrayList<>();
    }

    /**
     * Creates a user with core identity and contact fields.
     *
     * @param id       unique user identifier
     * @param username login name
     * @param password stored password (plain or hashed, depending on persistence layer)
     * @param role     account role
     * @param name     display name
     * @param email    contact email
     */
    public User(String id, String username, String password, Role role, String name, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.email = email;
        this.skills = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    /**
     * Returns a short display label combining name and role.
     *
     * @return formatted string such as {@code "Jane Doe (TA)"}
     */
    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}

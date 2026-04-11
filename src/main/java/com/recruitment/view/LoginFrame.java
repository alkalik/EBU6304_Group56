package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoginFrame extends JFrame {
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    
    // Login attempt tracking
    private static final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 60 * 1000; // 1 minute
    private static final long ATTEMPT_WINDOW_MS = 60 * 1000; // 1 minute
    
    // Inner class to track login attempts
    private static class LoginAttempt {
        int failureCount;
        long firstFailureTime;
        long lockoutUntil;
        
        LoginAttempt() {
            this.failureCount = 0;
            this.firstFailureTime = 0;
            this.lockoutUntil = 0;
        }
    }

    public LoginFrame(UserService userService, JobService jobService, ApplicationService applicationService, NotificationService notificationService) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        initUI();
    }

    private void initUI() {
        setTitle("TA Recruitment System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 400);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Title
        JLabel titleLabel = new JLabel("TA Recruitment System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(70, 130, 180));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        roleComboBox = new JComboBox<>(new String[]{"TA", "MO", "ADMIN"});
        roleComboBox.setSelectedIndex(0);
        formPanel.add(roleComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton loginBtn = new JButton("Login");
        loginBtn.setPreferredSize(new Dimension(100, 35));
        loginBtn.setFocusPainted(false);
        loginBtn.addActionListener(e -> handleLogin());

        JButton registerBtn = new JButton("Register");
        registerBtn.setPreferredSize(new Dimension(100, 35));
        registerBtn.setFocusPainted(false);
        registerBtn.addActionListener(e -> openRegister());

        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Enter key triggers login
        getRootPane().setDefaultButton(loginBtn);

        setContentPane(mainPanel);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String selectedRole = (String) roleComboBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if user is locked out
        LoginAttempt attempt = loginAttempts.computeIfAbsent(username, k -> new LoginAttempt());
        long currentTime = System.currentTimeMillis();
        
        // Reset failures if outside the attempt window
        if (currentTime - attempt.firstFailureTime > ATTEMPT_WINDOW_MS) {
            attempt.failureCount = 0;
            attempt.firstFailureTime = 0;
            attempt.lockoutUntil = 0;
        }
        
        // Check if currently locked out
        if (attempt.lockoutUntil > currentTime) {
            long remainingSeconds = (attempt.lockoutUntil - currentTime) / 1000;
            JOptionPane.showMessageDialog(this, 
                    "Account locked due to too many failed login attempts.\nPlease try again in " + remainingSeconds + " seconds.",
                    "Account Locked", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userService.authenticate(username, password);
        if (user == null) {
            // Record failed attempt
            attempt.failureCount++;
            if (attempt.firstFailureTime == 0) {
                attempt.firstFailureTime = currentTime;
            }
            
            if (attempt.failureCount >= MAX_ATTEMPTS) {
                // Lock the account
                attempt.lockoutUntil = currentTime + LOCKOUT_DURATION_MS;
                JOptionPane.showMessageDialog(this, 
                        "Invalid username or password.\nToo many failed attempts. Account locked for 1 minute.",
                        "Login Failed - Account Locked", JOptionPane.ERROR_MESSAGE);
            } else {
                int remainingAttempts = MAX_ATTEMPTS - attempt.failureCount;
                JOptionPane.showMessageDialog(this, 
                        "Invalid username or password.\nRemaining attempts: " + remainingAttempts,
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        // Check if user's role matches selected role
        if (!user.getRole().toString().equals(selectedRole)) {
            JOptionPane.showMessageDialog(this, "Your account role does not match the selected role.",
                    "Role Mismatch", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Successful login - reset attempt counter
        attempt.failureCount = 0;
        attempt.firstFailureTime = 0;
        attempt.lockoutUntil = 0;
        
        openDashboard(user);
    }

    public void openDashboard(User user) {
        this.setVisible(false);
        switch (user.getRole()) {
            case TA:
                new TADashboard(user, this, jobService, applicationService, notificationService).setVisible(true);
                break;
            case MO:
                new MODashboard(user, this, jobService, applicationService, notificationService).setVisible(true);
                break;
            case ADMIN:
                new AdminDashboard(user, this, jobService, applicationService, notificationService).setVisible(true);
                break;
        }
    }

    private void openRegister() {
        new RegisterDialog(this, userService).setVisible(true);
    }

    public void showAgain() {
        usernameField.setText("");
        passwordField.setText("");
        roleComboBox.setSelectedIndex(0);
        userService.reload();
        setVisible(true);
    }
}

package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.util.ShadowBorder;

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

    private static final Color PRIMARY      = new Color(0x6C, 0x5C, 0xE7);
    private static final Color DARK_BG      = new Color(0x1E, 0x1E, 0x2E);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color TEXT_PRIMARY  = new Color(0x2D, 0x34, 0x36);
    private static final Color TEXT_SECONDARY = new Color(0x63, 0x6E, 0x72);
    private static final Color BORDER       = new Color(0xDF, 0xE6, 0xE9);

    public LoginFrame(UserService userService, JobService jobService, ApplicationService applicationService, NotificationService notificationService) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        initUI();
    }

    private void initUI() {
        setTitle("TA Recruitment System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(540, 640);
        setLocationRelativeTo(null);
        setResizable(false);

        // Gradient background panel
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0xF4, 0xF5, 0xF8),
                        0, getHeight(), new Color(0xEE, 0xEC, 0xFB));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ===== Top brand area (dark background) =====
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBackground(DARK_BG);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(40, 30, 30, 30));

        JLabel titleLabel = new JLabel("TA Recruitment");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0xCB, 0xC3, 0xF7));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Teaching Assistant Management System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(0xA0, 0xA0, 0xC0));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandPanel.add(titleLabel);
        brandPanel.add(Box.createVerticalStrut(6));
        brandPanel.add(subtitleLabel);
        root.add(brandPanel, BorderLayout.NORTH);

        // ===== Center: white card form with shadow elevation =====
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(0, 36, 30, 36));

        // Shadow card: opaque=false so ShadowBorder shows through; real background
        // painted by ShadowBorder itself
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.card(),
                BorderFactory.createEmptyBorder(30, 30, 25, 30)
        ));

        JLabel signInLabel = new JLabel("Sign In");
        signInLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        signInLabel.setForeground(TEXT_PRIMARY);
        signInLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(signInLabel);
        card.add(Box.createVerticalStrut(20));

        // Username
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(userLabel);
        card.add(Box.createVerticalStrut(6));

        usernameField = new JTextField();
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));

        // Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passLabel.setForeground(TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(24));

        // Login button
        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.addActionListener(e -> handleLogin());
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(16));

        // Register link
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkPanel.setBackground(CARD_BG);
        linkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noAccountLabel.setForeground(TEXT_SECONDARY);

        JButton registerLink = new JButton("Register");
        registerLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        registerLink.setForeground(PRIMARY);
        registerLink.setBackground(CARD_BG);
        registerLink.setBorderPainted(false);
        registerLink.setContentAreaFilled(false);
        registerLink.setFocusPainted(false);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLink.addActionListener(e -> openRegister());

        linkPanel.add(noAccountLabel);
        linkPanel.add(registerLink);
        card.add(linkPanel);

        centerWrapper.add(card);
        root.add(centerWrapper, BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginBtn);
        setContentPane(root);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

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
        userService.reload();
        setVisible(true);
    }
}

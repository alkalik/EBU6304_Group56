package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.UserService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final UserService userService;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame(UserService userService) {
        this.userService = userService;
        initUI();
    }

    private void initUI() {
        setTitle("TA Recruitment System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Title
        JLabel titleLabel = new JLabel("TA Recruitment System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton loginBtn = new JButton("Login");
        loginBtn.setPreferredSize(new Dimension(100, 35));
        loginBtn.addActionListener(e -> handleLogin());

        JButton registerBtn = new JButton("Register");
        registerBtn.setPreferredSize(new Dimension(100, 35));
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

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = userService.authenticate(username, password);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        openDashboard(user);
    }

    public void openDashboard(User user) {
        this.setVisible(false);
        switch (user.getRole()) {
            case TA:
                new TADashboard(user, this).setVisible(true);
                break;
            case MO:
                new MODashboard(user, this).setVisible(true);
                break;
            case ADMIN:
                new AdminDashboard(user, this).setVisible(true);
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

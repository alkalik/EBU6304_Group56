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
        setSize(520, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = UIHelper.createPagePanel();
        mainPanel.add(UIHelper.createHeaderPanel(
                "TA Recruitment System",
                "Sign in to manage recruitment tasks, applications, and job postings."
        ), BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        UIHelper.styleTextComponent(usernameField);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        UIHelper.styleTextComponent(passwordField);
        formPanel.add(passwordField, gbc);

        JPanel formCard = UIHelper.wrapInCard(formPanel);
        mainPanel.add(formCard, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);
        JButton loginBtn = new JButton("Login");
        loginBtn.setPreferredSize(new Dimension(120, 38));
        UIHelper.stylePrimaryButton(loginBtn);
        loginBtn.addActionListener(e -> handleLogin());

        JButton registerBtn = new JButton("Register");
        registerBtn.setPreferredSize(new Dimension(120, 38));
        UIHelper.styleSecondaryButton(registerBtn);
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

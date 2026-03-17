package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.UserService;

import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    private final UserService userService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JTextField nameField;
    private JTextField emailField;
    private JComboBox<User.Role> roleCombo;

    public RegisterDialog(JFrame parent, UserService userService) {
        super(parent, "Register New Account", true);
        this.userService = userService;
        initUI();
    }

    private void initUI() {
        setSize(420, 400);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        usernameField = new JTextField(18);
        mainPanel.add(usernameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(18);
        mainPanel.add(passwordField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        mainPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        confirmField = new JPasswordField(18);
        mainPanel.add(confirmField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        mainPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        nameField = new JTextField(18);
        mainPanel.add(nameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        mainPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        emailField = new JTextField(18);
        mainPanel.add(emailField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        mainPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        roleCombo = new JComboBox<>(User.Role.values());
        mainPanel.add(roleCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton registerBtn = new JButton("Register");
        registerBtn.setPreferredSize(new Dimension(100, 32));
        registerBtn.addActionListener(e -> handleRegister());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(100, 32));
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(registerBtn);
        btnPanel.add(cancelBtn);
        mainPanel.add(btnPanel, gbc);

        setContentPane(mainPanel);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        User.Role role = (User.Role) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = new User(null, username, password, role, name, email);
        if (userService.register(user)) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Username already exists.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

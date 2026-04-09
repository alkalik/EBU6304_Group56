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
        setSize(520, 500);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel mainPanel = UIHelper.createPagePanel();
        mainPanel.add(UIHelper.createHeaderPanel(
                "Create New Account",
                "Register as a TA, Module Organiser, or Admin to access the recruitment system."
        ), BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        usernameField = new JTextField(18);
        UIHelper.styleTextComponent(usernameField);
        formPanel.add(usernameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(18);
        UIHelper.styleTextComponent(passwordField);
        formPanel.add(passwordField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        confirmField = new JPasswordField(18);
        UIHelper.styleTextComponent(confirmField);
        formPanel.add(confirmField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        nameField = new JTextField(18);
        UIHelper.styleTextComponent(nameField);
        formPanel.add(nameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        emailField = new JTextField(18);
        UIHelper.styleTextComponent(emailField);
        formPanel.add(emailField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        roleCombo = new JComboBox<>(User.Role.values());
        UIHelper.styleTextComponent(roleCombo);
        formPanel.add(roleCombo, gbc);

        mainPanel.add(UIHelper.wrapInCard(formPanel), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setOpaque(false);
        JButton registerBtn = new JButton("Register");
        registerBtn.setPreferredSize(new Dimension(120, 36));
        UIHelper.stylePrimaryButton(registerBtn);
        registerBtn.addActionListener(e -> handleRegister());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 36));
        UIHelper.styleSecondaryButton(cancelBtn);
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(registerBtn);
        btnPanel.add(cancelBtn);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(registerBtn);
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

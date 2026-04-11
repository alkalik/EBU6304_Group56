package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.UserService;
import com.recruitment.util.RegistrationRules;
import com.recruitment.util.ShadowBorder;

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

    private static final Color DARK_BG      = new Color(0x1E, 0x1E, 0x2E);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(0x63, 0x6E, 0x72);
    private static final Color DANGER       = new Color(0xE1, 0x70, 0x55);

    public RegisterDialog(JFrame parent, UserService userService) {
        super(parent, "Register New Account", true);
        this.userService = userService;
        initUI();
    }

    private void initUI() {
        setSize(520, 600);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0xF4, 0xF5, 0xF8),
                        0, getHeight(), new Color(0xEE, 0xEC, 0xFB));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ===== Title banner =====
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(DARK_BG);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(24, 30, 20, 30));

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(0xCB, 0xC3, 0xF7));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Join the TA Recruitment platform");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(0xA0, 0xA0, 0xC0));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitleLabel);
        root.add(titlePanel, BorderLayout.NORTH);

        // ===== Form card =====
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

        JPanel card = new JPanel(new GridBagLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.card(),
                BorderFactory.createEmptyBorder(24, 24, 20, 24)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        addFormField(card, gbc, row++, "Username", usernameField = new JTextField(20));
        addFormField(card, gbc, row++, "Password", passwordField = new JPasswordField(20));
        addFormField(card, gbc, row++, "Confirm Password", confirmField = new JPasswordField(20));
        addFormField(card, gbc, row++, "Full Name", nameField = new JTextField(20));
        addFormField(card, gbc, row++, "Email", emailField = new JTextField(20));

        // Role
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel roleLabel = new JLabel("Role");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(TEXT_SECONDARY);
        card.add(roleLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        roleCombo = new JComboBox<>(new User.Role[]{User.Role.TA, User.Role.MO});
        card.add(roleCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(16, 4, 4, 4);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setBackground(CARD_BG);

        JButton registerBtn = new JButton("Register");
        registerBtn.setPreferredSize(new Dimension(120, 36));
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerBtn.addActionListener(e -> handleRegister());

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 36));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setBackground(DANGER);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.addActionListener(e -> dispose());

        btnPanel.add(registerBtn);
        btnPanel.add(cancelBtn);
        card.add(btnPanel, gbc);

        cardWrapper.add(card);
        root.add(cardWrapper, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_SECONDARY);
        panel.add(label, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        User.Role role = (User.Role) roleCombo.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your full name.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String uFmt = RegistrationRules.usernameFormatFailure(usernameField.getText());
        if (uFmt != null) {
            JOptionPane.showMessageDialog(this, uFmt, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String uTaken = RegistrationRules.usernameAvailabilityFailure(username, userService);
        if (uTaken != null) {
            JOptionPane.showMessageDialog(this, uTaken, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String eFmt = RegistrationRules.emailFormatFailure(emailField.getText());
        if (eFmt != null) {
            JOptionPane.showMessageDialog(this, eFmt, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String eUsed = RegistrationRules.emailAvailabilityFailure(email, userService);
        if (eUsed != null) {
            JOptionPane.showMessageDialog(this, eUsed, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordReason = RegistrationRules.passwordPolicyFailure(password);
        if (passwordReason != null) {
            JOptionPane.showMessageDialog(this, passwordReason,
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String confirmReason = RegistrationRules.confirmPasswordFailure(password, confirm);
        if (confirmReason != null) {
            JOptionPane.showMessageDialog(this, confirmReason,
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = new User(null, username, password, role, name, email);
        if (userService.register(user)) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else if (userService.isUsernameTaken(username)) {
            JOptionPane.showMessageDialog(this, "This username is already registered.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        } else if (userService.isEmailRegistered(email)) {
            JOptionPane.showMessageDialog(this, "This email is already registered.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Registration could not be completed. Please try again.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

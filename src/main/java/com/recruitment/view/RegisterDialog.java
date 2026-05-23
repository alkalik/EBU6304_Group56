package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.UserService;
import com.recruitment.util.ShadowBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterDialog extends JDialog {

    private final UserService userService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JTextField nameField;
    private JTextField emailField;
    private JComboBox<User.Role> roleCombo;

    private static final Color C_HEADER  = new Color(0x1A, 0x1A, 0x2E);
    private static final Color C_BG      = new Color(0xF0, 0xF2, 0xF8);
    private static final Color C_SURFACE = Color.WHITE;
    private static final Color C_PRIMARY = new Color(0x5C, 0x6B, 0xE8);
    private static final Color C_ACCENT  = new Color(0x43, 0xC6, 0xAC);
    private static final Color C_DANGER  = new Color(0xE5, 0x53, 0x53);
    private static final Color C_TXT_SEC = new Color(0x66, 0x72, 0x80);
    private static final Font  F_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  F_BODY    = new Font("Segoe UI", Font.PLAIN, 13);

    public RegisterDialog(JFrame parent, UserService userService) {
        super(parent, "Create New Account", true);
        this.userService = userService;
        initUI();
    }

    private void initUI() {
        setSize(500, 600);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        // ── Header ─────────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(C_HEADER);
        header.setBorder(new EmptyBorder(26, 0, 22, 0));

        JLabel titleLbl = new JLabel("Create Account");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLbl.setForeground(new Color(0xCB, 0xC5, 0xFF));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Join the TA Recruitment Platform");
        subLbl.setFont(F_BODY);
        subLbl.setForeground(new Color(0xA0, 0xA8, 0xCC));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(titleLbl);
        header.add(Box.createVerticalStrut(5));
        header.add(subLbl);
        root.add(header, BorderLayout.NORTH);

        // ── Form card ──────────────────────────────────────────────────────
        JPanel cardWrap = new JPanel(new GridBagLayout());
        cardWrap.setBackground(C_BG);
        cardWrap.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.card(),
                new EmptyBorder(22, 28, 18, 28)));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        usernameField = field();
        passwordField = new JPasswordField(20);
        confirmField  = new JPasswordField(20);
        nameField     = field();
        emailField    = field();
        styleComp(passwordField);
        styleComp(confirmField);

        int r = 0;
        r = addField(card, g, r, "Username *",         usernameField);
        r = addField(card, g, r, "Password *",          passwordField);
        r = addField(card, g, r, "Confirm Password *",  confirmField);
        r = addField(card, g, r, "Full Name *",          nameField);
        r = addField(card, g, r, "Email *",              emailField);

        // Role row
        g.gridx = 0; g.gridy = r; g.gridwidth = 1; g.weightx = 0;
        g.insets = new Insets(8, 0, 2, 0);
        JLabel roleLbl = new JLabel("Role *");
        roleLbl.setFont(F_BOLD); roleLbl.setForeground(C_TXT_SEC);
        card.add(roleLbl, g);
        r++;
        g.gridx = 0; g.gridy = r; g.gridwidth = 2; g.weightx = 1.0;
        g.insets = new Insets(2, 0, 10, 0);
        roleCombo = new JComboBox<>(new User.Role[]{User.Role.TA, User.Role.MO});
        roleCombo.setFont(F_BODY);
        card.add(roleCombo, g);
        r++;

        // Separator
        g.gridx = 0; g.gridy = r; g.gridwidth = 2;
        g.insets = new Insets(6, 0, 6, 0);
        card.add(new JSeparator(), g);
        r++;

        // Buttons
        g.gridx = 0; g.gridy = r; g.gridwidth = 2;
        g.insets = new Insets(10, 0, 0, 0);
        g.anchor = GridBagConstraints.CENTER;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setBackground(C_SURFACE);

        JButton regBtn = makeBtn("Register", C_ACCENT);
        regBtn.setPreferredSize(new Dimension(120, 38));
        regBtn.addActionListener(e -> handleRegister());

        JButton cancelBtn = makeBtn("Cancel", C_DANGER);
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        cancelBtn.addActionListener(e -> dispose());

        btnRow.add(regBtn);
        btnRow.add(cancelBtn);
        card.add(btnRow, g);

        cardWrap.add(card, new GridBagConstraints());
        root.add(cardWrap, BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(regBtn);
    }

    private int addField(JPanel p, GridBagConstraints g, int row, String label, JComponent comp) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        g.insets = new Insets(8, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_BOLD); lbl.setForeground(C_TXT_SEC);
        p.add(lbl, g);
        row++;
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weightx = 1.0;
        g.insets = new Insets(2, 0, 2, 0);
        p.add(comp, g);
        return row + 1;
    }

    private JTextField field() {
        JTextField f = new JTextField(20);
        styleComp(f);
        return f;
    }

    private void styleComp(JComponent c) {
        c.setFont(F_BODY);
        c.setPreferredSize(new Dimension(280, 34));
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(F_BOLD);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmField.getPassword());
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        User.Role role  = (User.Role) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty()) {
            warn("All fields marked with * are required.");
            return;
        }
        if (username.length() < 3) {
            warn("Username must be at least 3 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            warn("Passwords do not match.");
            return;
        }
        if (password.length() < 4) {
            warn("Password must be at least 4 characters.");
            return;
        }
        if (!email.contains("@")) {
            warn("Please enter a valid email address.");
            return;
        }

        User user = new User(null, username, password, role, name, email);
        if (userService.register(user)) {
            JOptionPane.showMessageDialog(this,
                    "Registration successful! You can now log in.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            warn("Username already exists. Please choose a different one.");
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }
}

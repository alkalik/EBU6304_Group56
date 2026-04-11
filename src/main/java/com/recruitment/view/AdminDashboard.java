package com.recruitment.view;

import java.awt.*;
import java.util.List;
import java.util.Optional;

import javax.swing.*;
import com.recruitment.util.ShadowBorder;
import javax.swing.table.DefaultTableModel;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.BackupService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;

public class AdminDashboard extends JFrame {
    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    private static final Color PRIMARY      = new Color(0x6C, 0x5C, 0xE7);
    private static final Color DARK_BG      = new Color(0x1E, 0x1E, 0x2E);
    private static final Color TEXT_PRIMARY  = new Color(0x2D, 0x34, 0x36);
    private static final Color TEXT_SECONDARY = new Color(0x63, 0x6E, 0x72);
    private static final Color DANGER       = new Color(0xE1, 0x70, 0x55);
    private static final Color BORDER       = new Color(0xDF, 0xE6, 0xE9);

    public AdminDashboard(User currentUser, LoginFrame loginFrame, JobService jobService, ApplicationService applicationService, NotificationService notificationService) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        initUI();
    }

    private void initUI() {
        setTitle("Admin Dashboard - " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());

        // ===== Welcome header bar =====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DARK_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + "  (Administrator)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(new Color(0xCB, 0xC3, 0xF7));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        JButton logoutHeaderBtn = new JButton("Logout");
        logoutHeaderBtn.setBackground(DANGER);
        logoutHeaderBtn.setForeground(Color.WHITE);
        logoutHeaderBtn.setFocusPainted(false);
        logoutHeaderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutHeaderBtn.addActionListener(e -> logout());
        headerRight.add(logoutHeaderBtn);

        headerPanel.add(headerRight, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // ===== Menu bar =====
        JMenuBar menuBar = new JMenuBar();
        JMenu accountMenu = new JMenu("Account");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        accountMenu.add(logoutItem);
        menuBar.add(accountMenu);

        JMenu dataMenu = new JMenu("Data");
        JMenuItem backupItem = new JMenuItem("Backup Data");
        backupItem.addActionListener(e -> BackupService.backupAllData(currentUser, true));
        dataMenu.add(backupItem);

        JMenuItem restoreItem = new JMenuItem("Restore Data...");
        restoreItem.addActionListener(e -> {
            java.util.List<String> backups = BackupService.listBackups();
            if (backups.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No backups available.", "Restore", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String sel = (String) JOptionPane.showInputDialog(this,
                    "Select backup to restore:", "Restore Backup",
                    JOptionPane.PLAIN_MESSAGE, null,
                    backups.toArray(new String[0]), backups.get(0));
            if (sel != null) {
                BackupService.restoreBackup(currentUser, sel);
            }
        });
        dataMenu.add(restoreItem);
        menuBar.add(dataMenu);

        JMenu notificationMenu = new JMenu("Notifications");
        JMenuItem broadcastItem = new JMenuItem("Broadcast Announcement");
        broadcastItem.addActionListener(e -> broadcastAnnouncement());
        notificationMenu.add(broadcastItem);

        JMenuItem notifyTAItem = new JMenuItem("Notify All TAs");
        notifyTAItem.addActionListener(e -> notifyUsersByRole(User.Role.TA, "TA Notification"));
        notificationMenu.add(notifyTAItem);

        JMenuItem notifyMOItem = new JMenuItem("Notify All MOs");
        notifyMOItem.addActionListener(e -> notifyUsersByRole(User.Role.MO, "MO Notification"));
        notificationMenu.add(notifyMOItem);

        menuBar.add(notificationMenu);
        setJMenuBar(menuBar);

        // ===== Tabbed pane =====
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("  TA Workload  ", createWorkloadPanel());
        tabbedPane.addTab("  All Users  ", createUsersPanel());
        tabbedPane.addTab("  All Jobs  ", createAllJobsPanel());
        tabbedPane.addTab("  All Applications  ", createAllApplicationsPanel());
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        root.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createWorkloadPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel header = new JLabel("TA Workload Overview", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setForeground(TEXT_PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        panel.add(header, BorderLayout.NORTH);

        String[] columns = {"TA ID", "Name", "Email", "Department", "Skills", "Accepted Jobs", "Pending Apps"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDF, 0xE6, 0xE9), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            userService.reload();
            applicationService.reload();
            loadWorkload(model);
        });
        btnPanel.add(refreshBtn);

        JButton detailBtn = new JButton("View TA Details");
        detailBtn.setFocusPainted(false);
        detailBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a TA.");
                return;
            }
            String taId = (String) model.getValueAt(selectedRow, 0);
            showTADetails(taId);
        });
        btnPanel.add(detailBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadWorkload(model);
        return panel;
    }

    private void loadWorkload(DefaultTableModel model) {
        model.setRowCount(0);
        List<User> tas = userService.findByRole(User.Role.TA);
        for (User ta : tas) {
            long accepted = applicationService.getAcceptedCountByApplicant(ta.getId());
            long pending = applicationService.getApplicationsByApplicant(ta.getId()).stream()
                    .filter(a -> a.getStatus() == Application.Status.PENDING).count();
            model.addRow(new Object[]{
                    ta.getId(), ta.getName(), ta.getEmail(),
                    ta.getDepartment() != null ? ta.getDepartment() : "",
                    ta.getSkills() != null ? String.join(", ", ta.getSkills()) : "",
                    accepted, pending
            });
        }
    }

    private void showTADetails(String taId) {
        Optional<User> taOpt = userService.findById(taId);
        if (!taOpt.isPresent()) return;

        User ta = taOpt.get();
        List<Application> apps = applicationService.getApplicationsByApplicant(taId);

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(ta.getName()).append("\n");
        sb.append("Email: ").append(ta.getEmail()).append("\n");
        sb.append("Phone: ").append(ta.getPhone() != null ? ta.getPhone() : "N/A").append("\n");
        sb.append("Department: ").append(ta.getDepartment() != null ? ta.getDepartment() : "N/A").append("\n");
        sb.append("Skills: ").append(ta.getSkills() != null ? String.join(", ", ta.getSkills()) : "N/A").append("\n");
        sb.append("CV: ").append(ta.getCvPath() != null ? ta.getCvPath() : "Not uploaded").append("\n\n");
        sb.append("--- Applications ---\n");

        for (Application app : apps) {
            String jobTitle = jobService.findById(app.getJobId())
                    .map(Job::getTitle).orElse("Unknown");
            sb.append(String.format("  [%s] %s - %s (%s)\n",
                    app.getId(), jobTitle, app.getStatus(), app.getApplyDate()));
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(500, 350));
        JOptionPane.showMessageDialog(this, sp, "TA Details: " + ta.getName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        String[] columns = {"ID", "Username", "Name", "Role", "Email", "Department"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);

        // Search bar with shadow card
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.subtle(),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        JLabel searchIcon = new JLabel("Search:");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchIcon.setForeground(TEXT_SECONDARY);
        searchPanel.add(searchIcon);

        JTextField keywordField = new JTextField(24);
        keywordField.setToolTipText("Search by username, name, role, email, or department");
        searchPanel.add(keywordField);

        JLabel resultLabel = new JLabel();
        resultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultLabel.setForeground(TEXT_SECONDARY);

        JButton searchBtn = new JButton("Search");
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> loadUsers(model, keywordField.getText(), resultLabel));

        JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            keywordField.setText("");
            loadUsers(model, "", resultLabel);
        });

        keywordField.addActionListener(e -> loadUsers(model, keywordField.getText(), resultLabel));

        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        searchPanel.add(resultLabel);
        panel.add(searchPanel, BorderLayout.NORTH);

        JScrollPane userScrollPane = new JScrollPane(table);
        userScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDF, 0xE6, 0xE9), 1));
        userScrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(userScrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            userService.reload();
            loadUsers(model, keywordField.getText(), resultLabel);
        });

        JButton deleteBtn = new JButton("Delete User");
        deleteBtn.setBackground(DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a user.");
                return;
            }
            String userId = (String) model.getValueAt(selectedRow, 0);
            if (userId.equals(currentUser.getId())) {
                JOptionPane.showMessageDialog(this, "Cannot delete yourself.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete this user permanently?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                userService.deleteUser(userId);
                loadUsers(model, keywordField.getText(), resultLabel);
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(deleteBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadUsers(model, "", resultLabel);
        return panel;
    }

    private void loadUsers(DefaultTableModel model, String keyword, JLabel resultLabel) {
        model.setRowCount(0);
        String searchText = keyword == null ? "" : keyword.trim();
        List<User> users = searchText.isEmpty()
                ? userService.getAllUsers()
                : userService.searchUsers(searchText);
        for (User user : users) {
            model.addRow(new Object[]{
                    user.getId(), user.getUsername(), user.getName(),
                    user.getRole(), user.getEmail(),
                    user.getDepartment() != null ? user.getDepartment() : ""
            });
        }
        if (resultLabel != null) {
            resultLabel.setText(searchText.isEmpty()
                    ? "Users: " + users.size()
                    : "Results: " + users.size());
        }
    }

    private JPanel createAllJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        String[] columns = {"ID", "Title", "Module", "Type", "Posted By", "Positions", "Filled", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane jobScrollPane = new JScrollPane(table);
        jobScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDF, 0xE6, 0xE9), 1));
        jobScrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(jobScrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            jobService.reload();
            userService.reload();
            loadAllJobs(model);
        });
        btnPanel.add(refreshBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadAllJobs(model);
        return panel;
    }

    private void loadAllJobs(DefaultTableModel model) {
        model.setRowCount(0);
        for (Job job : jobService.getAllJobs()) {
            String postedByName = userService.findById(job.getPostedBy())
                    .map(User::getName).orElse("Unknown");
            model.addRow(new Object[]{
                    job.getId(), job.getTitle(), job.getModuleName(),
                    job.getJobType(), postedByName,
                    job.getMaxPositions(), job.getFilledPositions(), job.getStatus()
            });
        }
    }

    private JPanel createAllApplicationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        String[] columns = {"App ID", "Job Title", "Applicant", "Apply Date", "Status", "Reviewed By"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane appScrollPane = new JScrollPane(table);
        appScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDF, 0xE6, 0xE9), 1));
        appScrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(appScrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            applicationService.reload();
            jobService.reload();
            userService.reload();
            loadAllApplications(model);
        });
        btnPanel.add(refreshBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadAllApplications(model);
        return panel;
    }

    private void loadAllApplications(DefaultTableModel model) {
        model.setRowCount(0);
        for (Application app : applicationService.getAllApplications()) {
            String jobTitle = jobService.findById(app.getJobId())
                    .map(Job::getTitle).orElse("Unknown");
            String applicantName = userService.findById(app.getApplicantId())
                    .map(User::getName).orElse("Unknown");
            String reviewerName = app.getReviewedBy() != null ?
                    userService.findById(app.getReviewedBy())
                            .map(User::getName).orElse("") : "";
            model.addRow(new Object[]{
                    app.getId(), jobTitle, applicantName,
                    app.getApplyDate(), app.getStatus(), reviewerName
            });
        }
    }

    private void broadcastAnnouncement() {
        String message = JOptionPane.showInputDialog(this, "Enter announcement message:", "Broadcast Announcement", JOptionPane.PLAIN_MESSAGE);
        if (message != null && !message.trim().isEmpty()) {
            notificationService.broadcastNotification(message, com.recruitment.model.Notification.Type.ANNOUNCEMENT);
            JOptionPane.showMessageDialog(this, "Announcement broadcasted to all users!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void notifyUsersByRole(User.Role role, String title) {
        String message = JOptionPane.showInputDialog(this, "Enter notification message for " + role + "s:", title, JOptionPane.PLAIN_MESSAGE);
        if (message != null && !message.trim().isEmpty()) {
            notificationService.notifyUsersByRole(message, com.recruitment.model.Notification.Type.INFO, role);
            JOptionPane.showMessageDialog(this, "Notification sent to all " + role + "s!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void logout() {
        dispose();
        loginFrame.showAgain();
    }
}

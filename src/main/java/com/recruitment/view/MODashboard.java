package com.recruitment.view;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.BackupService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.util.ShadowBorder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MODashboard extends JFrame {
    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    private JTabbedPane tabbedPane;

    private static final Color PRIMARY      = new Color(0x6C, 0x5C, 0xE7);
    private static final Color DARK_BG      = new Color(0x1E, 0x1E, 0x2E);
    private static final Color TEXT_SECONDARY = new Color(0x63, 0x6E, 0x72);
    private static final Color DANGER       = new Color(0xE1, 0x70, 0x55);
    private static final Color SUCCESS      = new Color(0x00, 0xB8, 0x94);
    private static final Color BORDER       = new Color(0xDF, 0xE6, 0xE9);

    public MODashboard(User currentUser, LoginFrame loginFrame, JobService jobService, ApplicationService applicationService, NotificationService notificationService) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        initUI();
    }

    private void initUI() {
        setTitle("Module Organiser Dashboard - " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 660);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());

        // ===== Welcome header bar =====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DARK_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + "  (Module Organiser)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(new Color(0xCB, 0xC3, 0xF7));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        JButton notificationButton = new JButton("Notifications");
        notificationButton.setToolTipText("Notifications");
        notificationButton.setFocusPainted(false);
        notificationButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        notificationButton.setForeground(Color.WHITE);
        notificationButton.setBackground(new Color(0x6C, 0x5C, 0xE7));
        notificationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notificationButton.addActionListener(e -> showNotifications());
        updateNotificationButton(notificationButton);
        headerRight.add(notificationButton);

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
        boolean can = BackupService.canBackup(currentUser.getRole());
        dataMenu.setEnabled(can);
        menuBar.add(dataMenu);
        setJMenuBar(menuBar);

        // ===== Tabbed pane =====
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("  Post New Job  ", createPostJobPanel());
        tabbedPane.addTab("  My Posted Jobs  ", createMyJobsPanel());
        tabbedPane.addTab("  Review Applicants  ", createReviewPanel());
        root.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createPostJobPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.card(),
                BorderFactory.createEmptyBorder(22, 26, 22, 26)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField titleField = new JTextField(25);
        JTextField moduleField = new JTextField(25);
        JComboBox<Job.JobType> typeCombo = new JComboBox<>(Job.JobType.values());
        JTextArea descArea = new JTextArea(4, 25);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JTextField skillsField = new JTextField(25);
        JSpinner positionsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JTextField semesterField = new JTextField("2025-2026 Spring", 25);
        JTextField deadlineField = new JTextField("2026-04-30", 25);

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Job Title:", titleField);
        addFormRow(formPanel, gbc, row++, "Module Name:", moduleField);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel typeLabel = new JLabel("Job Type:");
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        typeLabel.setForeground(TEXT_SECONDARY);
        formPanel.add(typeLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(typeCombo, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(TEXT_SECONDARY);
        formPanel.add(descLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(new JScrollPane(descArea), gbc);
        row++;

        addFormRow(formPanel, gbc, row++, "Required Skills (comma-separated):", skillsField);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel posLabel = new JLabel("Max Positions:");
        posLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        posLabel.setForeground(TEXT_SECONDARY);
        formPanel.add(posLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(positionsSpinner, gbc);
        row++;

        addFormRow(formPanel, gbc, row++, "Semester:", semesterField);
        addFormRow(formPanel, gbc, row++, "Deadline (YYYY-MM-DD):", deadlineField);

        panel.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton postBtn = new JButton("Post Job");
        postBtn.setPreferredSize(new Dimension(130, 38));
        postBtn.setBackground(SUCCESS);
        postBtn.setForeground(Color.WHITE);
        postBtn.setFocusPainted(false);
        postBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        postBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String desc = descArea.getText().trim();

            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Job title is required.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Job job = new Job();
            job.setTitle(title);
            job.setModuleName(moduleField.getText().trim());
            job.setJobType((Job.JobType) typeCombo.getSelectedItem());
            job.setDescription(desc);
            job.setPostedBy(currentUser.getId());
            job.setMaxPositions((int) positionsSpinner.getValue());
            job.setSemester(semesterField.getText().trim());
            job.setDeadline(deadlineField.getText().trim());

            String skillsText = skillsField.getText().trim();
            if (!skillsText.isEmpty()) {
                job.setRequiredSkills(Arrays.stream(skillsText.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()));
            }

            jobService.createJob(job);
            JOptionPane.showMessageDialog(this, "Job posted successfully!");

            titleField.setText("");
            moduleField.setText("");
            descArea.setText("");
            skillsField.setText("");
            positionsSpinner.setValue(1);

            tabbedPane.setComponentAt(1, createMyJobsPanel());
            tabbedPane.setComponentAt(2, createReviewPanel());
        });

        JButton clearBtn = new JButton("Clear");
        clearBtn.setPreferredSize(new Dimension(110, 38));
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            titleField.setText("");
            moduleField.setText("");
            descArea.setText("");
            skillsField.setText("");
            positionsSpinner.setValue(1);
        });

        btnPanel.add(postBtn);
        btnPanel.add(clearBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setPreferredSize(new Dimension(100, 35));
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMyJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        String[] columns = {"ID", "Title", "Module", "Type", "Positions", "Filled", "Status", "Deadline"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane jobsScrollPane = new JScrollPane(table);
        jobsScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDF, 0xE6, 0xE9), 1));
        jobsScrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(jobsScrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            jobService.reload();
            loadMyJobs(model);
        });

        JButton closeBtn = new JButton("Close Job");
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a job.");
                return;
            }
            String jobId = (String) model.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Close this job? No more applications will be accepted.",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                jobService.closeJob(jobId);
                loadMyJobs(model);
            }
        });

        JButton deleteBtn = new JButton("Delete Job");
        deleteBtn.setBackground(DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a job.");
                return;
            }
            String jobId = (String) model.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete this job permanently?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                jobService.deleteJob(jobId);
                loadMyJobs(model);
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(closeBtn);
        btnPanel.add(deleteBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadMyJobs(model);
        return panel;
    }

    private void loadMyJobs(DefaultTableModel model) {
        model.setRowCount(0);
        List<Job> jobs = jobService.getJobsByMO(currentUser.getId());
        for (Job job : jobs) {
            model.addRow(new Object[]{
                    job.getId(), job.getTitle(), job.getModuleName(),
                    job.getJobType(), job.getMaxPositions(), job.getFilledPositions(),
                    job.getStatus(), job.getDeadline()
            });
        }
    }

    private JPanel createReviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel selectLabel = new JLabel("Select Job:");
        selectLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectLabel.setForeground(TEXT_SECONDARY);
        topPanel.add(selectLabel);
        JComboBox<String> jobCombo = new JComboBox<>();
        jobCombo.setPreferredSize(new Dimension(320, 32));
        List<Job> myJobs = jobService.getJobsByMO(currentUser.getId());
        for (Job job : myJobs) {
            jobCombo.addItem(job.getId() + " - " + job.getTitle());
        }
        topPanel.add(jobCombo);
        panel.add(topPanel, BorderLayout.NORTH);

        String[] columns = {"App ID", "Applicant", "Email", "Skills", "Apply Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane appsScrollPane = new JScrollPane(table);
        appsScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDF, 0xE6, 0xE9), 1));
        appsScrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(appsScrollPane, BorderLayout.CENTER);

        jobCombo.addActionListener(e -> {
            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                loadApplicationsForJob(model, jobId);
            }
        });

        if (jobCombo.getItemCount() > 0) {
            String first = jobCombo.getItemAt(0);
            String jobId = first.split(" - ")[0];
            loadApplicationsForJob(model, jobId);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            applicationService.reload();
            userService.reload();
            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                loadApplicationsForJob(model, jobId);
            }
        });

        JButton acceptBtn = new JButton("Accept");
        acceptBtn.setBackground(SUCCESS);
        acceptBtn.setForeground(Color.WHITE);
        acceptBtn.setFocusPainted(false);
        acceptBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        acceptBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select an applicant.");
                return;
            }
            String appId = (String) model.getValueAt(selectedRow, 0);
            boolean accepted = applicationService.acceptApplication(appId, currentUser.getId());
            if (!accepted) {
                JOptionPane.showMessageDialog(this,
                        "Unable to accept this application. It may already be processed or the job is full.",
                        "Accept Failed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                jobService.reload();
                loadApplicationsForJob(model, jobId);
            }
            JOptionPane.showMessageDialog(this, "Applicant accepted!");
        });

        JButton rejectBtn = new JButton("Reject");
        rejectBtn.setBackground(DANGER);
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setFocusPainted(false);
        rejectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rejectBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select an applicant.");
                return;
            }
            String appId = (String) model.getValueAt(selectedRow, 0);
            String note = JOptionPane.showInputDialog(this, "Rejection reason (optional):");
            applicationService.rejectApplication(appId, currentUser.getId(), note != null ? note : "");
            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                loadApplicationsForJob(model, jobId);
            }
            JOptionPane.showMessageDialog(this, "Applicant rejected.");
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(acceptBtn);
        btnPanel.add(rejectBtn);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadApplicationsForJob(DefaultTableModel model, String jobId) {
        model.setRowCount(0);
        List<Application> apps = applicationService.getApplicationsByJob(jobId);
        for (Application app : apps) {
            Optional<User> applicant = userService.findById(app.getApplicantId());
            String name = applicant.map(User::getName).orElse("Unknown");
            String email = applicant.map(User::getEmail).orElse("");
            String skills = applicant.map(u -> String.join(", ", u.getSkills())).orElse("");
            model.addRow(new Object[]{
                    app.getId(), name, email, skills, app.getApplyDate(), app.getStatus()
            });
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(TEXT_SECONDARY);
        panel.add(l, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void updateNotificationButton(JButton button) {
        int unreadCount = notificationService.getUnreadCount(currentUser.getId());
        if (unreadCount > 0) {
            button.setText("Notifications (" + unreadCount + ")");
            button.setForeground(new Color(0xFF, 0x63, 0x48));
        } else {
            button.setText("Notifications");
            button.setForeground(Color.WHITE);
        }
    }

    private void showNotifications() {
        JDialog dialog = new JDialog(this, "Notifications", true);
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> notificationList = new JList<>(listModel);
        notificationList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        notificationList.setFixedCellHeight(28);

        refreshNotificationList(listModel);

        JScrollPane scrollPane = new JScrollPane(notificationList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        JButton markReadBtn = new JButton("Mark as Read");
        markReadBtn.setFocusPainted(false);
        markReadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        markReadBtn.addActionListener(e -> {
            int[] selectedIndices = notificationList.getSelectedIndices();
            List<com.recruitment.model.Notification> currentNotifications = notificationService.getNotificationsByUser(currentUser.getId());
            for (int index : selectedIndices) {
                if (index < currentNotifications.size()) {
                    com.recruitment.model.Notification n = currentNotifications.get(index);
                    notificationService.markAsRead(n.getId());
                }
            }
            refreshNotificationList(listModel);
        });

        JButton clearReadBtn = new JButton("Clear All Read");
        clearReadBtn.setBackground(DANGER);
        clearReadBtn.setForeground(Color.WHITE);
        clearReadBtn.setFocusPainted(false);
        clearReadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearReadBtn.addActionListener(e -> {
            notificationService.clearReadNotifications(currentUser.getId());
            refreshNotificationList(listModel);
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(markReadBtn);
        buttonPanel.add(clearReadBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void refreshNotificationList(DefaultListModel<String> listModel) {
        listModel.clear();
        List<com.recruitment.model.Notification> notifications = notificationService.getNotificationsByUser(currentUser.getId());
        for (com.recruitment.model.Notification n : notifications) {
            String status = n.isRead() ? "[Read]" : "[Unread]";
            listModel.addElement(status + " " + n.getTimestamp().toString() + ": " + n.getMessage());
        }
    }

    private void logout() {
        dispose();
        loginFrame.showAgain();
    }
}

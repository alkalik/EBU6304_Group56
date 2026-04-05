package com.recruitment.view;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.BackupService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TADashboard extends JFrame {
    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    private JTabbedPane tabbedPane;

    public TADashboard(User currentUser, LoginFrame loginFrame) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
        this.notificationService = new NotificationService();
        initUI();
    }

    private void initUI() {
        setTitle("TA Dashboard - " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Menu bar
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

        // Notification bell button
        JButton notificationButton = new JButton("🔔");
        notificationButton.setToolTipText("Notifications");
        notificationButton.setBorderPainted(false);
        notificationButton.setContentAreaFilled(false);
        notificationButton.setFocusPainted(false);
        notificationButton.addActionListener(e -> showNotifications());
        updateNotificationButton(notificationButton);
        menuBar.add(Box.createHorizontalGlue()); // Push to right
        menuBar.add(notificationButton);

        setJMenuBar(menuBar);

        setJMenuBar(menuBar);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("My Profile", createProfilePanel());
        tabbedPane.addTab("Browse Jobs", createBrowseJobsPanel());
        tabbedPane.addTab("My Applications", createMyApplicationsPanel());

        setContentPane(tabbedPane);
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameField = new JTextField(currentUser.getName(), 25);
        JTextField emailField = new JTextField(currentUser.getEmail() != null ? currentUser.getEmail() : "", 25);
        JTextField phoneField = new JTextField(currentUser.getPhone() != null ? currentUser.getPhone() : "", 25);
        JTextField skillsField = new JTextField(
                currentUser.getSkills() != null ? String.join(", ", currentUser.getSkills()) : "", 25);
        JTextField deptField = new JTextField(
                currentUser.getDepartment() != null ? currentUser.getDepartment() : "", 25);
        JLabel cvLabel = new JLabel(currentUser.getCvPath() != null ? currentUser.getCvPath() : "No CV uploaded");

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Name:", nameField);
        addFormRow(formPanel, gbc, row++, "Email:", emailField);
        addFormRow(formPanel, gbc, row++, "Phone:", phoneField);
        addFormRow(formPanel, gbc, row++, "Skills (comma-separated):", skillsField);
        addFormRow(formPanel, gbc, row++, "Department:", deptField);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("CV:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel cvPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cvPanel.add(cvLabel);
        JButton uploadBtn = new JButton("Upload CV");
        uploadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                // Copy file to data/cv directory
                File cvDir = new File("data/cv");
                if (!cvDir.exists()) cvDir.mkdirs();
                File dest = new File(cvDir, currentUser.getId() + "_" + file.getName());
                try {
                    java.nio.file.Files.copy(file.toPath(), dest.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    currentUser.setCvPath(dest.getPath());
                    cvLabel.setText(dest.getName());
                    JOptionPane.showMessageDialog(this, "CV uploaded successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to upload CV: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        cvPanel.add(uploadBtn);
        formPanel.add(cvPanel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save Profile");
        saveBtn.setPreferredSize(new Dimension(120, 35));
        saveBtn.addActionListener(e -> {
            currentUser.setName(nameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());
            currentUser.setDepartment(deptField.getText().trim());
            String skillsText = skillsField.getText().trim();
            if (!skillsText.isEmpty()) {
                currentUser.setSkills(Arrays.stream(skillsText.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()));
            }
            userService.updateUser(currentUser);
            JOptionPane.showMessageDialog(this, "Profile saved successfully.");
            setTitle("TA Dashboard - " + currentUser.getName());
        });
        btnPanel.add(saveBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBrowseJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"ID", "Title", "Module", "Type", "Positions", "Deadline", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            jobService.reload();
            loadJobsTable(model);
        });

        JButton applyBtn = new JButton("Apply for Selected Job");
        applyBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a job to apply.");
                return;
            }
            String jobId = (String) model.getValueAt(selectedRow, 0);
            applyForJob(jobId);
        });

        JButton detailBtn = new JButton("View Details");
        detailBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a job.");
                return;
            }
            String jobId = (String) model.getValueAt(selectedRow, 0);
            showJobDetails(jobId);
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(detailBtn);
        btnPanel.add(applyBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadJobsTable(model);
        return panel;
    }

    private void loadJobsTable(DefaultTableModel model) {
        model.setRowCount(0);
        List<Job> jobs = jobService.getOpenJobs();
        for (Job job : jobs) {
            model.addRow(new Object[]{
                    job.getId(), job.getTitle(), job.getModuleName(),
                    job.getJobType(), job.getMaxPositions() - job.getFilledPositions(),
                    job.getDeadline(), job.getStatus()
            });
        }
    }

    private void showJobDetails(String jobId) {
        Optional<Job> jobOpt = jobService.findById(jobId);
        if (!jobOpt.isPresent()) {
            JOptionPane.showMessageDialog(this, "Job not found.");
            return;
        }
        Job job = jobOpt.get();
        String details = String.format(
                "Title: %s\nModule: %s\nType: %s\nDescription: %s\n\nRequired Skills: %s\n" +
                        "Positions: %d (Filled: %d)\nDeadline: %s\nPosted: %s",
                job.getTitle(), job.getModuleName(), job.getJobType(),
                job.getDescription(), String.join(", ", job.getRequiredSkills()),
                job.getMaxPositions(), job.getFilledPositions(),
                job.getDeadline(), job.getPostDate()
        );
        JTextArea textArea = new JTextArea(details);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(400, 250));
        JOptionPane.showMessageDialog(this, sp, "Job Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void applyForJob(String jobId) {
        Optional<Job> jobOpt = jobService.findById(jobId);
        if (!jobOpt.isPresent()) {
            JOptionPane.showMessageDialog(this, "Job not found.");
            return;
        }

        JTextArea coverLetterArea = new JTextArea(5, 30);
        coverLetterArea.setLineWrap(true);
        coverLetterArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(coverLetterArea);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Cover Letter (optional):"), BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Apply for: " + jobOpt.get().getTitle(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            applicationService.reload();
            Application app = applicationService.apply(jobId, currentUser.getId(),
                    coverLetterArea.getText().trim());
            if (app != null) {
                JOptionPane.showMessageDialog(this, "Application submitted successfully!");
                // Refresh applications tab
                tabbedPane.setComponentAt(2, createMyApplicationsPanel());
            } else {
                JOptionPane.showMessageDialog(this, "You have already applied for this job.",
                        "Duplicate Application", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private JPanel createMyApplicationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"App ID", "Job Title", "Apply Date", "Status", "Review Note"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            applicationService.reload();
            jobService.reload();
            loadApplicationsTable(model);
        });

        JButton withdrawBtn = new JButton("Withdraw Application");
        withdrawBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select an application.");
                return;
            }
            String appId = (String) model.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to withdraw this application?",
                    "Confirm Withdrawal", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                applicationService.withdrawApplication(appId);
                loadApplicationsTable(model);
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(withdrawBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadApplicationsTable(model);
        return panel;
    }

    private void loadApplicationsTable(DefaultTableModel model) {
        model.setRowCount(0);
        List<Application> apps = applicationService.getApplicationsByApplicant(currentUser.getId());
        for (Application app : apps) {
            String jobTitle = jobService.findById(app.getJobId())
                    .map(Job::getTitle).orElse("Unknown");
            model.addRow(new Object[]{
                    app.getId(), jobTitle, app.getApplyDate(),
                    app.getStatus(), app.getReviewNote() != null ? app.getReviewNote() : ""
            });
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void updateNotificationButton(JButton button) {
        int unreadCount = notificationService.getUnreadCount(currentUser.getId());
        if (unreadCount > 0) {
            button.setText("🔔 (" + unreadCount + ")");
            button.setForeground(Color.RED);
        } else {
            button.setText("🔔");
            button.setForeground(Color.BLACK);
        }
    }

    private void showNotifications() {
        JDialog dialog = new JDialog(this, "Notifications", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> notificationList = new JList<>(listModel);
        notificationList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        refreshNotificationList(listModel);

        JScrollPane scrollPane = new JScrollPane(notificationList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton markReadBtn = new JButton("Mark Selected as Read");
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
            updateNotificationButton((JButton) getJMenuBar().getComponent(getJMenuBar().getComponentCount() - 1));
        });

        JButton clearReadBtn = new JButton("Clear All Read");
        clearReadBtn.addActionListener(e -> {
            notificationService.clearReadNotifications(currentUser.getId());
            refreshNotificationList(listModel);
            updateNotificationButton((JButton) getJMenuBar().getComponent(getJMenuBar().getComponentCount() - 1));
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(markReadBtn);
        buttonPanel.add(clearReadBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
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

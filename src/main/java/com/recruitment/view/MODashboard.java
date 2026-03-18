package com.recruitment.view;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.BackupService;
import com.recruitment.service.JobService;
import com.recruitment.service.UserService;

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

    private JTabbedPane tabbedPane;

    public MODashboard(User currentUser, LoginFrame loginFrame) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
        initUI();
    }

    private void initUI() {
        setTitle("Module Organiser Dashboard - " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

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

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Post New Job", createPostJobPanel());
        tabbedPane.addTab("My Posted Jobs", createMyJobsPanel());
        tabbedPane.addTab("Review Applicants", createReviewPanel());

        setContentPane(tabbedPane);
    }

    private JPanel createPostJobPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel formPanel = new JPanel(new GridBagLayout());
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
        formPanel.add(new JLabel("Job Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(typeCombo, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(new JScrollPane(descArea), gbc);
        row++;

        addFormRow(formPanel, gbc, row++, "Required Skills (comma-separated):", skillsField);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Max Positions:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(positionsSpinner, gbc);
        row++;

        addFormRow(formPanel, gbc, row++, "Semester:", semesterField);
        addFormRow(formPanel, gbc, row++, "Deadline (YYYY-MM-DD):", deadlineField);

        panel.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton postBtn = new JButton("Post Job");
        postBtn.setPreferredSize(new Dimension(120, 35));
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

            // Clear fields
            titleField.setText("");
            moduleField.setText("");
            descArea.setText("");
            skillsField.setText("");
            positionsSpinner.setValue(1);

            // Refresh jobs tab
            tabbedPane.setComponentAt(1, createMyJobsPanel());
            tabbedPane.setComponentAt(2, createReviewPanel());
        });

        JButton clearBtn = new JButton("Clear");
        clearBtn.setPreferredSize(new Dimension(100, 35));
        clearBtn.addActionListener(e -> {
            titleField.setText("");
            moduleField.setText("");
            descArea.setText("");
            skillsField.setText("");
            positionsSpinner.setValue(1);
        });

        btnPanel.add(postBtn);
        btnPanel.add(clearBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMyJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"ID", "Title", "Module", "Type", "Positions", "Filled", "Status", "Deadline"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            jobService.reload();
            loadMyJobs(model);
        });

        JButton closeBtn = new JButton("Close Job");
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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Job selector at top
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.add(new JLabel("Select Job:"));
        JComboBox<String> jobCombo = new JComboBox<>();
        jobCombo.setPreferredSize(new Dimension(300, 28));
        List<Job> myJobs = jobService.getJobsByMO(currentUser.getId());
        for (Job job : myJobs) {
            jobCombo.addItem(job.getId() + " - " + job.getTitle());
        }
        topPanel.add(jobCombo);
        panel.add(topPanel, BorderLayout.NORTH);

        // Applications table
        String[] columns = {"App ID", "Applicant", "Email", "Skills", "Apply Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        jobCombo.addActionListener(e -> {
            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                loadApplicationsForJob(model, jobId);
            }
        });

        // Load first job's applications
        if (jobCombo.getItemCount() > 0) {
            String first = jobCombo.getItemAt(0);
            String jobId = first.split(" - ")[0];
            loadApplicationsForJob(model, jobId);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton acceptBtn = new JButton("Accept");
        acceptBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select an applicant.");
                return;
            }
            String appId = (String) model.getValueAt(selectedRow, 0);
            applicationService.acceptApplication(appId, currentUser.getId());

            // Update filled positions
            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                Optional<Job> jobOpt = jobService.findById(jobId);
                if (jobOpt.isPresent()) {
                    Job job = jobOpt.get();
                    job.setFilledPositions(job.getFilledPositions() + 1);
                    jobService.updateJob(job);
                }
                loadApplicationsForJob(model, jobId);
            }
            JOptionPane.showMessageDialog(this, "Applicant accepted!");
        });

        JButton rejectBtn = new JButton("Reject");
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

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            applicationService.reload();
            userService.reload();
            String selected = (String) jobCombo.getSelectedItem();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                loadApplicationsForJob(model, jobId);
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(acceptBtn);
        btnPanel.add(rejectBtn);
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
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void logout() {
        dispose();
        loginFrame.showAgain();
    }
}

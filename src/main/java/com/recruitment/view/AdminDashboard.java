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
import java.util.List;
import java.util.Optional;

public class AdminDashboard extends JFrame {
    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;

    public AdminDashboard(User currentUser, LoginFrame loginFrame) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
        initUI();
    }

    private void initUI() {
        setTitle("Admin Dashboard - " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
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
        menuBar.add(dataMenu);
        setJMenuBar(menuBar);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("TA Workload Overview", createWorkloadPanel());
        tabbedPane.addTab("All Users", createUsersPanel());
        tabbedPane.addTab("All Jobs", createAllJobsPanel());
        tabbedPane.addTab("All Applications", createAllApplicationsPanel());

        setContentPane(tabbedPane);
    }

    private JPanel createWorkloadPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("TA Workload Overview", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        panel.add(header, BorderLayout.NORTH);

        String[] columns = {"TA ID", "Name", "Email", "Department", "Skills", "Accepted Jobs", "Pending Apps"};
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
            userService.reload();
            applicationService.reload();
            loadWorkload(model);
        });
        btnPanel.add(refreshBtn);

        JButton detailBtn = new JButton("View TA Details");
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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"ID", "Username", "Name", "Role", "Email", "Department"};
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
            userService.reload();
            loadUsers(model);
        });

        JButton deleteBtn = new JButton("Delete User");
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
                loadUsers(model);
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(deleteBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadUsers(model);
        return panel;
    }

    private void loadUsers(DefaultTableModel model) {
        model.setRowCount(0);
        for (User user : userService.getAllUsers()) {
            model.addRow(new Object[]{
                    user.getId(), user.getUsername(), user.getName(),
                    user.getRole(), user.getEmail(),
                    user.getDepartment() != null ? user.getDepartment() : ""
            });
        }
    }

    private JPanel createAllJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"ID", "Title", "Module", "Type", "Posted By", "Positions", "Filled", "Status"};
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
            userService.reload();
            loadAllJobs(model);
        });
        btnPanel.add(refreshBtn);
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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"App ID", "Job Title", "Applicant", "Apply Date", "Status", "Reviewed By"};
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
            applicationService.reload();
            jobService.reload();
            userService.reload();
            loadAllApplications(model);
        });
        btnPanel.add(refreshBtn);
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

    private void logout() {
        dispose();
        loginFrame.showAgain();
    }
}

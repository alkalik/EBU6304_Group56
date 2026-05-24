package com.recruitment.view;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.util.ShadowBorder;
import com.recruitment.util.UiText;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main application window for Teaching Assistant ({@link User.Role#TA}) users.
 * <p>
 * Provides three tabs:
 * </p>
 * <ul>
 *   <li><b>My Profile</b> – edit personal details, skills, password, and upload a CV</li>
 *   <li><b>Browse Jobs</b> – search and apply for open TA positions</li>
 *   <li><b>My Applications</b> – view application status and withdraw pending applications</li>
 * </ul>
 * <p>
 * The header includes a notifications panel and logout. UI styling reuses shared
 * widget factories from {@link AdminDashboard}.
 * </p>
 */
public class TADashboard extends JFrame {

    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    // Shared palette
    private static final Color C_BG      = AdminDashboard.C_BG;
    private static final Color C_SURFACE = AdminDashboard.C_SURFACE;
    private static final Color C_PRIMARY = AdminDashboard.C_PRIMARY;
    private static final Color C_ACCENT  = AdminDashboard.C_ACCENT;
    private static final Color C_DANGER  = AdminDashboard.C_DANGER;
    private static final Color C_HDR     = AdminDashboard.C_HDR;
    private static final Color C_TXT_PRI = AdminDashboard.C_TXT_PRI;
    private static final Color C_TXT_SEC = AdminDashboard.C_TXT_SEC;
    private static final Font  F_BODY    = AdminDashboard.F_BODY;
    private static final Font  F_BOLD    = AdminDashboard.F_BOLD;
    private static final Font  F_SMALL   = AdminDashboard.F_SMALL;
    private static final Font  F_H2      = AdminDashboard.F_H2;

    /**
     * Creates the TA dashboard for the given authenticated user.
     *
     * @param currentUser           the logged-in teaching assistant
     * @param loginFrame            the login frame to return to on logout
     * @param jobService            service for browsing and querying job listings
     * @param applicationService    service for submitting and managing applications
     * @param notificationService   service for reading and updating notifications
     */
    public TADashboard(User currentUser, LoginFrame loginFrame, JobService jobService,
                       ApplicationService applicationService, NotificationService notificationService) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        initUI();
    }

    private void initUI() {
        setTitle("Teaching Assistant – " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 740);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_BOLD);
        tabs.addTab("  My Profile  ",      createProfilePanel());
        tabs.addTab("  Browse Jobs  ",     createBrowsePanel());
        tabs.addTab("  My Applications  ", createAppsPanel());
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(C_HDR); h.setBorder(new EmptyBorder(16, 28, 16, 28));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); left.setOpaque(false);
        JLabel icon = new JLabel("🎓  "); icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18)); icon.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel title = new JLabel("Teaching Assistant"); title.setFont(new Font("Segoe UI", Font.BOLD, 18)); title.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel sep = new JLabel("  |  "); sep.setForeground(new Color(0x55, 0x55, 0x88));
        JLabel name = new JLabel(currentUser.getName()); name.setFont(new Font("Segoe UI", Font.PLAIN, 14)); name.setForeground(new Color(0xA0, 0xA8, 0xCC));
        left.add(icon); left.add(title); left.add(sep); left.add(name);
        h.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); right.setOpaque(false);
        JButton nb = AdminDashboard.pill("Notifications", C_PRIMARY);
        nb.addActionListener(e -> { notificationService.reload(); showNotifications(); updateNotifBtn(nb); });
        updateNotifBtn(nb); right.add(nb);
        JButton lo = AdminDashboard.pill("Logout", C_DANGER); lo.addActionListener(e -> logout()); right.add(lo);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    // ── Tab 1 – My Profile ─────────────────────────────────────────────────
    private JPanel createProfilePanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(C_BG); outer.setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(24, 30, 20, 30)));

        JLabel secLbl = new JLabel("Edit Profile");
        secLbl.setFont(F_H2); secLbl.setForeground(C_TXT_PRI);

        GridBagConstraints g = gbcBase();
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.insets = new Insets(0, 0, 18, 0);
        card.add(secLbl, g);

        JTextField nameF   = tf(currentUser.getName(), 26);
        JTextField emailF  = tf(currentUser.getEmail() != null ? currentUser.getEmail() : "", 26);
        JTextField phoneF  = tf(currentUser.getPhone() != null ? currentUser.getPhone() : "", 26);
        JTextField deptF   = tf(currentUser.getDepartment() != null ? currentUser.getDepartment() : "", 26);
        JTextField skillsF = tf(currentUser.getSkills() != null ? String.join(", ", currentUser.getSkills()) : "", 26);
        skillsF.putClientProperty("JTextField.placeholderText", "e.g. Java, Python, Machine Learning");
        JPasswordField pwF = new JPasswordField(26); pwF.setFont(F_BODY);

        int row = 1;
        row = addField(card, g, row, "Full Name *", nameF);
        row = addField(card, g, row, "Email", emailF);
        row = addField(card, g, row, "Phone", phoneF);
        row = addField(card, g, row, "Department", deptF);
        row = addField(card, g, row, "Skills (comma-separated)", skillsF);
        row = addField(card, g, row, "New Password (leave blank to keep)", pwF);

        // CV row
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0; g.insets = new Insets(8, 0, 2, 8);
        JLabel cvLbl = new JLabel("CV File"); cvLbl.setFont(F_BOLD); cvLbl.setForeground(C_TXT_SEC); card.add(cvLbl, g);
        row++;
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weightx = 1.0; g.insets = new Insets(4, 0, 10, 0);
        JPanel cvRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); cvRow.setBackground(C_SURFACE);
        String cvInit = currentUser.getCvPath() != null ? new File(currentUser.getCvPath()).getName() : "Not uploaded";
        JLabel cvPathLbl = new JLabel(cvInit); cvPathLbl.setFont(F_SMALL); cvPathLbl.setForeground(C_TXT_SEC);
        JButton uploadBtn = AdminDashboard.pill("Upload CV", C_ACCENT);
        uploadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Documents", "pdf", "doc", "docx"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentUser.setCvPath(fc.getSelectedFile().getAbsolutePath());
                cvPathLbl.setText(fc.getSelectedFile().getName());
                userService.updateUser(currentUser); info("CV saved.");
            }
        });
        cvRow.add(uploadBtn); cvRow.add(cvPathLbl); card.add(cvRow, g);
        outer.add(card, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12)); btns.setBackground(C_BG);
        JButton save = AdminDashboard.pill("Save Changes", C_ACCENT); save.setPreferredSize(new Dimension(140, 36));
        save.addActionListener(e -> {
            String nm = nameF.getText().trim(); if (nm.isEmpty()) { warn("Name cannot be empty."); return; }
            currentUser.setName(nm); currentUser.setEmail(emailF.getText().trim());
            currentUser.setPhone(phoneF.getText().trim()); currentUser.setDepartment(deptF.getText().trim());
            String sk = skillsF.getText().trim();
            if (!sk.isEmpty()) currentUser.setSkills(java.util.Arrays.stream(sk.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            String pw = new String(pwF.getPassword());
            if (!pw.isEmpty()) { if (pw.length() < 4) { warn("Password must be at least 4 characters."); return; } currentUser.setPassword(pw); }
            userService.updateUser(currentUser); info("Profile updated!");
        });
        btns.add(save);
        outer.add(btns, BorderLayout.SOUTH);
        return outer;
    }

    // ── Tab 2 – Browse Jobs ────────────────────────────────────────────────
    private JPanel createBrowsePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setBackground(C_SURFACE);
        searchBar.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(10, 16, 10, 16)));
        JTextField kwF = new JTextField(28); kwF.setFont(F_BODY);
        kwF.putClientProperty("JTextField.placeholderText", "Search by title, module or skill...");
        JLabel resLbl = new JLabel(); resLbl.setFont(F_SMALL); resLbl.setForeground(C_TXT_SEC);
        JButton sb = AdminDashboard.pill("Search", C_PRIMARY); JButton cb = AdminDashboard.ghost("Clear");
        searchBar.add(new JLabel(UiText.symbolText("🔍", "", true))); searchBar.add(kwF); searchBar.add(sb); searchBar.add(cb); searchBar.add(resLbl);
        panel.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Module", "Type", "Required Skills", "Open Positions", "Deadline"};
        DefaultTableModel model = AdminDashboard.noEdit(cols);
        JTable table = AdminDashboard.styledTable(model);
        table.getColumnModel().getColumn(4).setPreferredWidth(220);
        panel.add(AdminDashboard.wrapInCard("Open Positions", AdminDashboard.wrapScroll(table)), BorderLayout.CENTER);

        Runnable load = () -> loadJobs(model, kwF.getText(), resLbl);
        sb.addActionListener(e -> load.run()); cb.addActionListener(e -> { kwF.setText(""); load.run(); }); kwF.addActionListener(e -> load.run());

        JPanel btns = btnRow();
        JButton refresh = AdminDashboard.ghost(UiText.symbolText("↻", "Refresh", false)); refresh.addActionListener(e -> { jobService.reload(); load.run(); });
        JButton apply = AdminDashboard.pill("Apply for This Job", C_ACCENT);
        apply.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select a job."); return; }
            String jobId = (String) model.getValueAt(r, 0);
            Optional<Job> jobOpt = jobService.findById(jobId);
            if (!jobOpt.isPresent()) { warn("Job not found."); return; }
            if (jobOpt.get().getStatus() != Job.Status.OPEN) { warn("This job is no longer open for applications."); return; }
            boolean dup = applicationService.getApplicationsByApplicant(currentUser.getId()).stream().anyMatch(a -> a.getJobId().equals(jobId));
            if (dup) { warn("You have already applied for this position."); return; }
            applicationService.apply(jobId, currentUser.getId(), "");
            info("Application submitted successfully!");
            load.run();
        });
        btns.add(refresh); btns.add(apply);
        panel.add(btns, BorderLayout.SOUTH);
        load.run();
        return panel;
    }

    private void loadJobs(DefaultTableModel model, String kw, JLabel resLbl) {
        model.setRowCount(0);
        String k = kw == null ? "" : kw.trim().toLowerCase();
        List<Job> jobs = jobService.getOpenJobs();
        if (!k.isEmpty()) jobs = jobs.stream().filter(j ->
                (j.getTitle() != null && j.getTitle().toLowerCase().contains(k)) ||
                (j.getModuleName() != null && j.getModuleName().toLowerCase().contains(k)) ||
                (j.getRequiredSkills() != null && j.getRequiredSkills().stream().anyMatch(s -> s.toLowerCase().contains(k))))
                .collect(Collectors.toList());
        jobs.forEach(j -> model.addRow(new Object[]{j.getId(), j.getTitle(), j.getModuleName(), j.getJobType(),
                j.getRequiredSkills() != null ? String.join(", ", j.getRequiredSkills()) : "",
                j.getMaxPositions() - j.getFilledPositions(), j.getDeadline()}));
        if (resLbl != null) resLbl.setText(k.isEmpty() ? "  " + jobs.size() + " open jobs" : "  " + jobs.size() + " results");
    }

    // ── Tab 3 – My Applications ────────────────────────────────────────────
    private JPanel createAppsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));

        String[] cols = {"App ID", "Job Title", "Module", "Apply Date", "Status", "Review Note", "Withdrawn At"};
        DefaultTableModel model = AdminDashboard.noEdit(cols);
        JTable table = AdminDashboard.styledTable(model);
        table.getColumnModel().getColumn(4).setCellRenderer(AdminDashboard.statusBadgeRenderer());
        table.getColumnModel().getColumn(5).setPreferredWidth(200);
        panel.add(AdminDashboard.wrapInCard("My Applications", AdminDashboard.wrapScroll(table)), BorderLayout.CENTER);

        JPanel btns = btnRow();
        JButton refresh = AdminDashboard.ghost(UiText.symbolText("↻", "Refresh", false)); refresh.addActionListener(e -> { applicationService.reload(); loadMyApps(model); });
        JButton withdraw = AdminDashboard.pill("Withdraw", C_DANGER);
        withdraw.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select an application."); return; }
            String appId = (String) model.getValueAt(r, 0);
            applicationService.reload();
            Optional<Application> appOpt = applicationService.findById(appId);
            if (!appOpt.isPresent()) { warn("Application not found."); return; }
            Application app = appOpt.get();
            if (app.getStatus() != Application.Status.PENDING) {
                warn("<html>Only <b>PENDING</b> applications can be withdrawn.<br>Current status: <b>" + app.getStatus() + "</b></html>"); return;
            }
            if (!confirm("<html><b>Withdraw this application?</b><br><br>Once withdrawn, this cannot be undone.</html>")) return;
            if (!confirm("Final confirmation: withdraw this application?")) return;
            applicationService.withdrawApplication(appId);
            applicationService.reload(); loadMyApps(model);
            info("Application withdrawn.");
        });
        btns.add(refresh); btns.add(withdraw);
        panel.add(btns, BorderLayout.SOUTH);
        loadMyApps(model);
        return panel;
    }

    private void loadMyApps(DefaultTableModel model) {
        model.setRowCount(0);
        applicationService.getApplicationsByApplicant(currentUser.getId()).forEach(app -> {
            String jt  = jobService.findById(app.getJobId()).map(Job::getTitle).orElse("Unknown");
            String mod = jobService.findById(app.getJobId()).map(Job::getModuleName).orElse("");
            model.addRow(new Object[]{app.getId(), jt, mod, app.getApplyDate(), app.getStatus(),
                    app.getReviewNote() != null ? app.getReviewNote() : "",
                    app.getWithdrawnAt() != null ? app.getWithdrawnAt() : ""});
        });
    }

    // ── Notifications ──────────────────────────────────────────────────────
    private void updateNotifBtn(JButton btn) {
        int n = notificationService.getUnreadCount(currentUser.getId());
        btn.setText(n > 0 ? "Notifications (" + n + ")" : "Notifications");
        btn.setForeground(n > 0 ? new Color(0xFF, 0xCC, 0x00) : Color.WHITE);
    }

    private void showNotifications() {
        JDialog dlg = new JDialog(this, "Notifications", true);
        dlg.setSize(540, 420); dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setBackground(C_BG); p.setBorder(new EmptyBorder(12, 16, 12, 16));
        DefaultListModel<String> lm = new DefaultListModel<>();
        JList<String> list = new JList<>(lm); list.setFont(F_BODY); list.setFixedCellHeight(30);
        refreshNL(lm);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel bRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6)); bRow.setBackground(C_BG);
        JButton ref = AdminDashboard.pill("Refresh", C_PRIMARY); ref.addActionListener(e -> { notificationService.reload(); refreshNL(lm); });
        JButton mark = AdminDashboard.ghost("Mark as Read"); mark.addActionListener(e -> {
            int[] idx = list.getSelectedIndices();
            List<com.recruitment.model.Notification> all = notificationService.getNotificationsByUser(currentUser.getId());
            for (int i : idx) if (i < all.size()) notificationService.markAsRead(all.get(i).getId());
            refreshNL(lm);
        });
        JButton clr = AdminDashboard.pill("Clear Read", C_DANGER); clr.addActionListener(e -> { notificationService.clearReadNotifications(currentUser.getId()); refreshNL(lm); });
        JButton cls = AdminDashboard.ghost("Close"); cls.addActionListener(e -> dlg.dispose());
        bRow.add(ref); bRow.add(mark); bRow.add(clr); bRow.add(cls);
        p.add(bRow, BorderLayout.SOUTH); dlg.setContentPane(p); dlg.setVisible(true);
    }

    private void refreshNL(DefaultListModel<String> lm) {
        lm.clear();
        notificationService.getNotificationsByUser(currentUser.getId()).forEach(n ->
                lm.addElement((n.isRead() ? "[Read]   " : "[Unread] ") + n.getTimestamp() + ": " + n.getMessage()));
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void logout() { dispose(); loginFrame.showAgain(); }
    private void warn(String m) { JOptionPane.showMessageDialog(this, m, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void info(String m) { JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE); }
    private boolean confirm(String m) { return JOptionPane.showConfirmDialog(this, m, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }

    // ── Widget helpers ─────────────────────────────────────────────────────
    private JPanel btnRow() { JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8)); p.setBackground(C_BG); return p; }
    private JTextField tf(String d, int c) { JTextField f = new JTextField(d, c); f.setFont(F_BODY); return f; }
    private GridBagConstraints gbcBase() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 6, 7, 6); g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST; return g;
    }
    private int addField(JPanel p, GridBagConstraints g, int row, String label, JComponent c) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0; g.insets = new Insets(8, 0, 2, 8);
        JLabel l = new JLabel(label); l.setFont(F_BOLD); l.setForeground(C_TXT_SEC); p.add(l, g);
        g.gridx = 1; g.gridwidth = 1; g.weightx = 1.0; g.insets = new Insets(4, 0, 4, 0); p.add(c, g);
        return row + 1;
    }
}

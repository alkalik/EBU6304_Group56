package com.recruitment.view;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.service.AIAnalysisService;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.BackupService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.util.AppConfig;
import com.recruitment.util.ShadowBorder;
import com.recruitment.util.UiText;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main application window for Module Organiser ({@link User.Role#MO}) users.
 * <p>
 * Provides four tabs:
 * </p>
 * <ul>
 *   <li><b>Post Job</b> – create new TA job openings for modules</li>
 *   <li><b>My Jobs</b> – manage posted jobs (close or delete)</li>
 *   <li><b>Review Applicants</b> – view CVs and accept or reject applications</li>
 *   <li><b>AI Skill Analysis</b> – run DeepSeek-powered skill-gap analysis on applicants</li>
 * </ul>
 * <p>
 * The menu bar also exposes data backup and restore actions. UI styling reuses shared
 * widget factories from {@link AdminDashboard}.
 * </p>
 */
public class MODashboard extends JFrame {

    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;
    private final AIAnalysisService aiAnalysisService;

    private JTabbedPane tabbedPane;

    // Reuse palette constants from AdminDashboard
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
    private static final Font  F_H1      = AdminDashboard.F_H1;
    private static final Font  F_H2      = AdminDashboard.F_H2;

    /**
     * Creates the Module Organiser dashboard for the given authenticated user.
     *
     * @param currentUser           the logged-in module organiser
     * @param loginFrame            the login frame to return to on logout
     * @param jobService            service for posting and managing jobs
     * @param applicationService    service for reviewing applicant submissions
     * @param notificationService   service for reading and updating notifications
     */
    public MODashboard(User currentUser, LoginFrame loginFrame, JobService jobService,
                       ApplicationService applicationService, NotificationService notificationService) {
        this.currentUser = currentUser;
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        this.aiAnalysisService = new AIAnalysisService();
        initUI();
    }

    private void initUI() {
        setTitle("Module Organiser – " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JMenuBar mb = new JMenuBar(); mb.setBackground(C_SURFACE);
        JMenu acct = new JMenu("Account");
        JMenuItem li = new JMenuItem("Logout"); li.addActionListener(e -> logout()); acct.add(li); mb.add(acct);
        JMenu data = new JMenu("Data");
        data.add(menuItem("Backup Data",   e -> BackupService.backupAllData(currentUser, true)));
        data.add(menuItem("Restore Data…", e -> doRestore()));
        mb.add(data);
        setJMenuBar(mb);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(F_BOLD);
        tabbedPane.addTab("  Post Job  ",         createPostJobPanel());
        tabbedPane.addTab("  My Jobs  ",           createMyJobsPanel());
        tabbedPane.addTab("  Review Applicants  ", createReviewPanel());
        tabbedPane.addTab("  AI Skill Analysis  ", createAIPanel());
        root.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JMenuItem menuItem(String t, java.awt.event.ActionListener l) {
        JMenuItem m = new JMenuItem(t); m.addActionListener(l); return m;
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(C_HDR); h.setBorder(new EmptyBorder(16, 28, 16, 28));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); left.setOpaque(false);
        JLabel icon = new JLabel("📋  "); icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18)); icon.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel title = new JLabel("Module Organiser"); title.setFont(new Font("Segoe UI", Font.BOLD, 18)); title.setForeground(new Color(0xCB, 0xC5, 0xFF));
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

    // ── Tab 1 – Post Job ───────────────────────────────────────────────────
    private JPanel createPostJobPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(C_BG); outer.setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(24, 30, 20, 30)));
        GridBagConstraints g = gbc();

        JTextField titleF  = tf(26); JTextField moduleF = tf(26);
        JComboBox<Job.JobType> typeC = new JComboBox<>(Job.JobType.values()); typeC.setFont(F_BODY);
        JTextArea descA = new JTextArea(4, 26); descA.setFont(F_BODY); descA.setLineWrap(true); descA.setWrapStyleWord(true);
        JTextField skillsF = tf(26); skillsF.putClientProperty("JTextField.placeholderText", "e.g. Java, Python, SQL");
        JSpinner posSpin = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JTextField semF  = tf("2025-2026 Spring", 26);
        JTextField deadF = tf("2026-06-30", 26);

        int row = 0;
        row = fr(card, g, row, "Job Title *", titleF);
        row = fr(card, g, row, "Module Name *", moduleF);
        row = cr(card, g, row, "Job Type", typeC);
        row = tar(card, g, row, "Description", descA);
        row = fr(card, g, row, "Required Skills (comma-separated)", skillsF);
        row = sr(card, g, row, "Max Positions", posSpin);
        row = fr(card, g, row, "Semester", semF);
        row = fr(card, g, row, "Deadline (YYYY-MM-DD)", deadF);

        // Section label
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.insets = new Insets(0, 0, 14, 0);
        JLabel secLbl = new JLabel("Post a New Job Opening");
        secLbl.setFont(F_H2); secLbl.setForeground(C_TXT_PRI);
        card.add(secLbl, g);

        outer.add(card, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12)); btns.setBackground(C_BG);
        JButton post = AdminDashboard.pill("Post Job", C_ACCENT); post.setPreferredSize(new Dimension(130, 36));
        post.addActionListener(e -> {
            String t = titleF.getText().trim(); if (t.isEmpty()) { warn("Job title is required."); return; }
            Job job = new Job(); job.setTitle(t); job.setModuleName(moduleF.getText().trim());
            job.setJobType((Job.JobType) typeC.getSelectedItem()); job.setDescription(descA.getText().trim());
            job.setPostedBy(currentUser.getId()); job.setMaxPositions((int) posSpin.getValue());
            job.setSemester(semF.getText().trim()); job.setDeadline(deadF.getText().trim());
            String sk = skillsF.getText().trim();
            if (!sk.isEmpty()) job.setRequiredSkills(Arrays.stream(sk.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            jobService.createJob(job);
            info("Job posted successfully!");
            titleF.setText(""); moduleF.setText(""); descA.setText(""); skillsF.setText(""); posSpin.setValue(1);
            refreshTabs();
        });
        JButton clear = AdminDashboard.ghost("Clear");
        clear.addActionListener(e -> { titleF.setText(""); moduleF.setText(""); descA.setText(""); skillsF.setText(""); posSpin.setValue(1); });
        btns.add(post); btns.add(clear);
        outer.add(btns, BorderLayout.SOUTH);
        return outer;
    }

    // ── Tab 2 – My Jobs ────────────────────────────────────────────────────
    private JPanel createMyJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));
        String[] cols = {"ID", "Title", "Module", "Type", "Max Positions", "Filled", "Status", "Deadline"};
        DefaultTableModel model = AdminDashboard.noEdit(cols);
        JTable table = AdminDashboard.styledTable(model);
        panel.add(AdminDashboard.wrapInCard("My Posted Jobs", AdminDashboard.wrapScroll(table)), BorderLayout.CENTER);
        JPanel btns = btnRow();
        JButton refresh = AdminDashboard.ghost(UiText.symbolText("↻", "Refresh", false));
        refresh.addActionListener(e -> { jobService.reload(); loadMyJobs(model); });
        JButton closeJ = AdminDashboard.ghost("Close Job");
        closeJ.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select a job."); return; }
            if (confirm("Close this job? No more applications will be accepted.")) {
                jobService.closeJob((String) model.getValueAt(r, 0)); loadMyJobs(model);
            }
        });
        JButton del = AdminDashboard.pill("Delete", C_DANGER);
        del.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select a job."); return; }
            if (confirm("Permanently delete this job?")) {
                jobService.deleteJob((String) model.getValueAt(r, 0)); loadMyJobs(model);
            }
        });
        btns.add(refresh); btns.add(closeJ); btns.add(del);
        panel.add(btns, BorderLayout.SOUTH);
        loadMyJobs(model);
        return panel;
    }

    private void loadMyJobs(DefaultTableModel model) {
        model.setRowCount(0);
        jobService.getJobsByMO(currentUser.getId()).forEach(j -> model.addRow(new Object[]{
                j.getId(), j.getTitle(), j.getModuleName(), j.getJobType(),
                j.getMaxPositions(), j.getFilledPositions(), j.getStatus(), j.getDeadline()}));
    }

    // ── Tab 3 – Review Applicants ──────────────────────────────────────────
    private JPanel createReviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setBackground(C_SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(10, 16, 10, 16)));
        topBar.add(lbl("Job:")); 
        JComboBox<String> jobCombo = new JComboBox<>(); jobCombo.setFont(F_BODY); jobCombo.setPreferredSize(new Dimension(380, 32));
        jobService.getJobsByMO(currentUser.getId()).forEach(j -> jobCombo.addItem(j.getId() + " – " + j.getTitle()));
        topBar.add(jobCombo);
        JLabel countLbl = new JLabel(); countLbl.setFont(F_SMALL); countLbl.setForeground(C_TXT_SEC);
        topBar.add(countLbl);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"App ID", "Name", "Email", "Skills", "Apply Date", "Status", "CV"};
        DefaultTableModel model = AdminDashboard.noEdit(cols);
        JTable table = AdminDashboard.styledTable(model);
        table.getColumnModel().getColumn(5).setCellRenderer(AdminDashboard.statusBadgeRenderer());
        table.getColumnModel().getColumn(6).setMaxWidth(60);
        panel.add(AdminDashboard.wrapInCard("Applicants", AdminDashboard.wrapScroll(table)), BorderLayout.CENTER);

        jobCombo.addActionListener(e -> { String sel = (String) jobCombo.getSelectedItem(); if (sel != null) { loadApplicants(model, sel.split(" – ")[0]); countLbl.setText("  " + model.getRowCount() + " applicants"); }});
        if (jobCombo.getItemCount() > 0) { loadApplicants(model, jobCombo.getItemAt(0).split(" – ")[0]); countLbl.setText("  " + model.getRowCount() + " applicants"); }

        JPanel btns = btnRow();
        JButton refresh = AdminDashboard.ghost(UiText.symbolText("↻", "Refresh", false));
        refresh.addActionListener(e -> {
            applicationService.reload(); userService.reload();
            String sel = (String) jobCombo.getSelectedItem();
            if (sel != null) { loadApplicants(model, sel.split(" – ")[0]); countLbl.setText("  " + model.getRowCount() + " applicants"); }
        });
        JButton viewCV = AdminDashboard.pill("View CV", C_PRIMARY);
        viewCV.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select an applicant."); return; }
            applicationService.reload();
            applicationService.findById((String) model.getValueAt(r, 0)).ifPresent(app ->
                    userService.findById(app.getApplicantId()).ifPresent(this::showCVDialog));
        });
        JButton accept = AdminDashboard.pill(UiText.symbolText("✔", "Accept", false, Color.WHITE), C_ACCENT);
        accept.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select an applicant."); return; }
            if (!applicationService.acceptApplication((String) model.getValueAt(r, 0), currentUser.getId())) {
                warn("Cannot accept – already processed or position full."); return;
            }
            String sel = (String) jobCombo.getSelectedItem();
            if (sel != null) { jobService.reload(); loadApplicants(model, sel.split(" – ")[0]); }
            info("Applicant accepted!");
        });
        JButton reject = AdminDashboard.pill(UiText.symbolText("✘", "Reject", false, Color.WHITE), C_DANGER);
        reject.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select an applicant."); return; }
            String note = JOptionPane.showInputDialog(this, "Rejection reason (optional):", "Reject", JOptionPane.PLAIN_MESSAGE);
            applicationService.rejectApplication((String) model.getValueAt(r, 0), currentUser.getId(), note != null ? note : "");
            String sel = (String) jobCombo.getSelectedItem();
            if (sel != null) loadApplicants(model, sel.split(" – ")[0]);
            info("Applicant rejected.");
        });
        btns.add(refresh); btns.add(viewCV); btns.add(accept); btns.add(reject);
        panel.add(btns, BorderLayout.SOUTH);
        return panel;
    }

    private void loadApplicants(DefaultTableModel model, String jobId) {
        model.setRowCount(0);
        applicationService.getApplicationsByJob(jobId).forEach(app -> {
            Optional<User> u = userService.findById(app.getApplicantId());
            model.addRow(new Object[]{app.getId(),
                    u.map(User::getName).orElse("Unknown"),
                    u.map(User::getEmail).orElse(""),
                    u.map(us -> String.join(", ", us.getSkills())).orElse(""),
                    app.getApplyDate(), app.getStatus(),
                    u.map(us -> us.getCvPath() != null ? "Yes" : "No").orElse("No")});
        });
    }

    private void showCVDialog(User ta) {
        JDialog dlg = new JDialog(this, "CV – " + ta.getName(), true);
        dlg.setSize(600, 460); dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        // Dark header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(C_HDR); hdr.setBorder(new EmptyBorder(18, 24, 18, 24));
        JPanel hl = new JPanel(); hl.setOpaque(false); hl.setLayout(new BoxLayout(hl, BoxLayout.Y_AXIS));
        JLabel nm = new JLabel(ta.getName()); nm.setFont(new Font("Segoe UI", Font.BOLD, 18)); nm.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel em = new JLabel(ta.getEmail() != null ? ta.getEmail() : ""); em.setFont(F_BODY); em.setForeground(new Color(0xA0, 0xA8, 0xCC));
        hl.add(nm); hl.add(em); hdr.add(hl, BorderLayout.WEST); root.add(hdr, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14)); body.setBackground(C_BG); body.setBorder(new EmptyBorder(16, 22, 16, 22));

        // Profile info card
        JPanel info = new JPanel(new GridLayout(0, 2, 8, 8)); info.setBackground(C_SURFACE);
        info.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(14, 18, 14, 18)));
        addIR(info, "Phone",      ta.getPhone() != null ? ta.getPhone() : "—");
        addIR(info, "Department", ta.getDepartment() != null ? ta.getDepartment() : "—");
        addIR(info, "Skills",     ta.getSkills() != null ? String.join(", ", ta.getSkills()) : "—");
        body.add(info, BorderLayout.NORTH);

        // CV file card
        JPanel cvCard = new JPanel(new BorderLayout(8, 8)); cvCard.setBackground(C_SURFACE);
        cvCard.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(14, 18, 14, 18)));
        JLabel cvTitle = new JLabel("CV File"); cvTitle.setFont(F_H2); cvTitle.setForeground(C_TXT_PRI);
        cvCard.add(cvTitle, BorderLayout.NORTH);

        String cvPath = ta.getCvPath();
        if (cvPath == null || cvPath.isEmpty()) {
            JLabel noCV = new JLabel("No CV uploaded.", SwingConstants.CENTER);
            noCV.setFont(F_BODY); noCV.setForeground(C_TXT_SEC); cvCard.add(noCV, BorderLayout.CENTER);
        } else {
            File f = new File(cvPath);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)); row.setBackground(C_SURFACE);
            JLabel path = new JLabel(cvPath); path.setFont(F_SMALL); path.setForeground(C_TXT_SEC);
            row.add(path);
            if (f.exists()) {
                JButton open = AdminDashboard.pill("Open File", C_PRIMARY);
                open.addActionListener(ev -> { try { Desktop.getDesktop().open(f); } catch (IOException ex) { warn(ex.getMessage()); } });
                row.add(open);
            } else {
                JLabel miss = new JLabel("  File not found"); miss.setFont(F_SMALL); miss.setForeground(C_DANGER); row.add(miss);
            }
            cvCard.add(row, BorderLayout.CENTER);
        }
        body.add(cvCard, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10)); bot.setBackground(C_BG);
        JButton cls = AdminDashboard.ghost("Close"); cls.addActionListener(e -> dlg.dispose()); bot.add(cls);
        root.add(bot, BorderLayout.SOUTH);
        dlg.setContentPane(root); dlg.setVisible(true);
    }

    // ── Tab 4 – AI Skill Analysis ──────────────────────────────────────────
    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setBackground(C_SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(10, 16, 10, 16)));
        topBar.add(lbl("Job:"));
        JComboBox<String> jobCombo = new JComboBox<>(); jobCombo.setFont(F_BODY); jobCombo.setPreferredSize(new Dimension(360, 32));
        jobService.getJobsByMO(currentUser.getId()).forEach(j -> jobCombo.addItem(j.getId() + " – " + j.getTitle()));
        topBar.add(jobCombo);
        final String analyseBtnLabel = UiText.symbolText("✦", "Analyse", false, Color.WHITE);
        JButton analyseBtn = AdminDashboard.pill(analyseBtnLabel, C_PRIMARY);
        topBar.add(analyseBtn);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"#", "Name", "Email", "Match %", "Matched Skills", "Missing (Importance)"};
        DefaultTableModel model = AdminDashboard.noEdit(cols);
        JTable table = AdminDashboard.styledTable(model);
        table.getColumnModel().getColumn(3).setCellRenderer(matchPctBadge());
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        table.getColumnModel().getColumn(5).setPreferredWidth(260);
        panel.add(AdminDashboard.wrapInCard("Candidate Skill Match", AdminDashboard.wrapScroll(table)), BorderLayout.CENTER);

        final List<AIAnalysisService.CandidateAnalysis>[] lastResult = new List[]{null};
        final Job[] selJob = new Job[]{null};

        analyseBtn.addActionListener(e -> {
            String sel = (String) jobCombo.getSelectedItem();
            if (sel == null) { warn("Please select a job."); return; }
            String jobId = sel.split(" – ")[0];
            Optional<Job> jobOpt = jobService.findById(jobId);
            if (!jobOpt.isPresent()) { warn("Job not found."); return; }
            selJob[0] = jobOpt.get();
            applicationService.reload(); userService.reload();
            List<Application> apps = applicationService.getApplicationsByJob(jobId);
            if (apps.isEmpty()) { info("No applicants for this job yet."); model.setRowCount(0); return; }

            analyseBtn.setEnabled(false); analyseBtn.setText("Analysing…");
            Job jSnap = selJob[0]; List<Application> aSnap = apps;
            new Thread(() -> {
                List<AIAnalysisService.CandidateAnalysis> results = aiAnalysisService.analyzeJobApplicants(jSnap, aSnap, userService);
                SwingUtilities.invokeLater(() -> {
                    lastResult[0] = results;
                    fillTable(model, results);
                    analyseBtn.setEnabled(true); analyseBtn.setText(analyseBtnLabel);
                    if (results.stream().anyMatch(r -> r.aiComment != null))
                        info("Analysis complete! DeepSeek AI comments added for top 3 candidates.");
                });
            }, "ai-analyse").start();
        });

        JPanel btns = btnRow();
        JButton refreshJobs = AdminDashboard.ghost(UiText.symbolText("↻", "Refresh", false));
        refreshJobs.addActionListener(e -> {
            jobCombo.removeAllItems(); jobService.reload();
            jobService.getJobsByMO(currentUser.getId()).forEach(j -> jobCombo.addItem(j.getId() + " – " + j.getTitle()));
        });
        JButton sortBtn = AdminDashboard.ghost("Sort by Match");
        sortBtn.addActionListener(e -> {
            if (lastResult[0] == null) { warn("Run analysis first."); return; }
            lastResult[0].sort((a, b) -> Double.compare(b.matchPercent, a.matchPercent));
            fillTable(model, lastResult[0]);
        });
        JButton detailBtn = AdminDashboard.pill("Details", C_PRIMARY);
        detailBtn.addActionListener(e -> {
            if (lastResult[0] == null) { warn("Run analysis first."); return; }
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select a candidate."); return; }
            showAnalysisDetail(lastResult[0].get(r), selJob[0]);
        });
        JButton exportBtn = AdminDashboard.pill("Export TXT", C_ACCENT);
        exportBtn.addActionListener(e -> {
            if (lastResult[0] == null || selJob[0] == null) { warn("Run analysis first."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("ai_analysis_" + selJob[0].getId() + ".txt"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
                try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                    fw.write(aiAnalysisService.exportAnalysisToText(selJob[0], lastResult[0]));
                    info("Exported: " + fc.getSelectedFile().getAbsolutePath());
                } catch (IOException ex) { warn("Export failed: " + ex.getMessage()); }
        });
        btns.add(refreshJobs); btns.add(sortBtn); btns.add(detailBtn); btns.add(exportBtn);
        panel.add(btns, BorderLayout.SOUTH);
        return panel;
    }

    private void fillTable(DefaultTableModel model, List<AIAnalysisService.CandidateAnalysis> results) {
        model.setRowCount(0);
        int rank = 1;
        for (AIAnalysisService.CandidateAnalysis r : results) {
            String missing = r.missingSkills.isEmpty() ? "None" :
                    r.missingSkills.stream().map(m -> m.skill + " [" + m.importance.getLabel() + "]").collect(Collectors.joining(", "));
            model.addRow(new Object[]{rank++, r.ta.getName(),
                    r.ta.getEmail() != null ? r.ta.getEmail() : "",
                    String.format("%.1f%%", r.matchPercent),
                    r.matchedSkills.isEmpty() ? "None" : String.join(", ", r.matchedSkills),
                    missing});
        }
    }

    private void showAnalysisDetail(AIAnalysisService.CandidateAnalysis r, Job job) {
        JDialog dlg = new JDialog(this, "Analysis – " + r.ta.getName(), true);
        dlg.setSize(600, 560); dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(C_HDR); hdr.setBorder(new EmptyBorder(16, 24, 16, 24));
        JPanel hl = new JPanel(); hl.setOpaque(false); hl.setLayout(new BoxLayout(hl, BoxLayout.Y_AXIS));
        JLabel nm = new JLabel(r.ta.getName()); nm.setFont(new Font("Segoe UI", Font.BOLD, 17)); nm.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel pos = new JLabel(job != null ? "Position: " + job.getTitle() : ""); pos.setFont(F_BODY); pos.setForeground(new Color(0xA0, 0xA8, 0xCC));
        hl.add(nm); hl.add(pos); hdr.add(hl, BorderLayout.WEST);
        // Match badge
        JLabel badge = new JLabel(String.format("%.0f%%", r.matchPercent), SwingConstants.CENTER);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 26));
        Color matchColor = r.matchPercent >= 80 ? AdminDashboard.C_BALANCED
                : r.matchPercent >= 50 ? AdminDashboard.C_WARNING : AdminDashboard.C_DANGER;
        badge.setForeground(matchColor);
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); badgePanel.setOpaque(false);
        badgePanel.add(badge); hdr.add(badgePanel, BorderLayout.EAST);
        root.add(hdr, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12)); body.setBackground(C_BG); body.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Skills cards side by side
        JPanel skillsRow = new JPanel(new GridLayout(1, 2, 12, 0)); skillsRow.setBackground(C_BG);
        skillsRow.add(skillListCard(UiText.symbolText("✔", "Matched Skills", false, AdminDashboard.C_BALANCED),
                r.matchedSkills.isEmpty() ? List.of("(none)") : r.matchedSkills, AdminDashboard.C_BALANCED));
        List<String> missingLines = r.missingSkills.isEmpty() ? List.of("(none)") :
                r.missingSkills.stream().map(m -> m.skill + "  [" + m.importance.getLabel() + "]").collect(Collectors.toList());
        skillsRow.add(skillListCard(UiText.symbolText("✘", "Missing Skills", false, C_DANGER), missingLines, C_DANGER));
        body.add(skillsRow, BorderLayout.NORTH);

        // AI comment area
        JTextArea aiArea = new JTextArea();
        aiArea.setEditable(false); aiArea.setFont(F_BODY); aiArea.setLineWrap(true); aiArea.setWrapStyleWord(true);
        aiArea.setBackground(new Color(0xF6, 0xF7, 0xFF)); aiArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        if (r.aiComment != null && !r.aiComment.isEmpty()) {
            aiArea.setText(r.aiComment);
        } else if (AppConfig.isDeepSeekEnabled()) {
            aiArea.setText("DeepSeek AI analysis (streaming)…\n");
            String jobReq = job != null && job.getRequiredSkills() != null ? String.join(", ", job.getRequiredSkills()) : "None";
            String taSkills = r.ta.getSkills() != null ? String.join(", ", r.ta.getSkills()) : "None";
            String missing = r.missingSkills.stream().map(m -> m.skill + "(" + m.importance.getLabel() + ")").collect(Collectors.joining(", "));
            String jTitle = job != null ? job.getTitle() : "Unknown";
            new Thread(() ->
                    new com.recruitment.util.DeepSeekClient().streamSkillGapAnalysis(
                            jTitle, jobReq, r.ta.getName(), taSkills, r.matchPercent,
                            missing.isEmpty() ? "None" : missing,
                            token -> SwingUtilities.invokeLater(() -> { aiArea.append(token); aiArea.setCaretPosition(aiArea.getDocument().getLength()); }),
                            () -> {},
                            err -> SwingUtilities.invokeLater(() -> aiArea.append("\n[AI unavailable: " + err + "]\n"))
                    ), "ds-detail").start();
        } else {
            aiArea.setText("(DeepSeek AI not enabled)");
        }

        JPanel aiCard = AdminDashboard.wrapInCard("DeepSeek AI In-depth Analysis", new JScrollPane(aiArea));
        body.add(aiCard, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10)); bot.setBackground(C_BG);
        JButton cls = AdminDashboard.ghost("Close"); cls.addActionListener(e -> dlg.dispose()); bot.add(cls);
        root.add(bot, BorderLayout.SOUTH);
        dlg.setContentPane(root); dlg.setVisible(true);
    }

    private JPanel skillListCard(String title, List<String> items, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6)); card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(12, 14, 12, 14)));
        JLabel tl = new JLabel(title); tl.setFont(F_BOLD);
        if (!title.startsWith("<html>")) tl.setForeground(accent);
        card.add(tl, BorderLayout.NORTH);
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(C_SURFACE);
        items.forEach(it -> {
            JLabel l = new JLabel("  " + it); l.setFont(F_BODY); l.setForeground(C_TXT_PRI);
            l.setBorder(new EmptyBorder(2, 0, 2, 0)); list.add(l);
        });
        card.add(list, BorderLayout.CENTER); return card;
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
    private void refreshTabs() {
        if (tabbedPane == null) return;
        tabbedPane.setComponentAt(1, createMyJobsPanel());
        tabbedPane.setComponentAt(2, createReviewPanel());
        tabbedPane.setComponentAt(3, createAIPanel());
    }

    private void doRestore() {
        List<String> bk = BackupService.listBackups();
        if (bk.isEmpty()) { info("No backups available."); return; }
        String sel = (String) JOptionPane.showInputDialog(this, "Select backup:", "Restore",
                JOptionPane.PLAIN_MESSAGE, null, bk.toArray(new String[0]), bk.get(0));
        if (sel != null) BackupService.restoreBackup(currentUser, sel);
    }

    private void logout() { dispose(); loginFrame.showAgain(); }
    private void warn(String m) { JOptionPane.showMessageDialog(this, m, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void info(String m) { JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE); }
    private boolean confirm(String m) { return JOptionPane.showConfirmDialog(this, m, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }

    // ── Widget helpers ─────────────────────────────────────────────────────
    private JPanel btnRow() { JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8)); p.setBackground(C_BG); return p; }
    private JTextField tf(int c) { JTextField f = new JTextField(c); f.setFont(F_BODY); return f; }
    private JTextField tf(String d, int c) { JTextField f = tf(c); f.setText(d); return f; }
    private JLabel lbl(String t) { JLabel l = new JLabel(t); l.setFont(F_BODY); l.setForeground(C_TXT_SEC); return l; }
    private void addIR(JPanel p, String k, String v) {
        JLabel kl = new JLabel(k); kl.setFont(F_BOLD); kl.setForeground(C_TXT_SEC);
        JLabel vl = new JLabel(v); vl.setFont(F_BODY); vl.setForeground(C_TXT_PRI);
        p.add(kl); p.add(vl);
    }
    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 6, 7, 6); g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST; return g;
    }
    private int fr(JPanel p, GridBagConstraints g, int row, String label, JComponent c) {
        g.gridx = 0; g.gridy = row + 1; g.gridwidth = 1; g.weightx = 0;
        g.insets = new Insets(6, 0, 2, 8);
        JLabel l = new JLabel(label); l.setFont(F_BOLD); l.setForeground(C_TXT_SEC); p.add(l, g);
        g.gridx = 1; g.gridwidth = 1; g.weightx = 1.0; g.insets = new Insets(6, 0, 2, 0); p.add(c, g);
        return row + 1;
    }
    private int cr(JPanel p, GridBagConstraints g, int row, String label, JComboBox<?> c) { return fr(p, g, row, label, c); }
    private int sr(JPanel p, GridBagConstraints g, int row, String label, JSpinner s) { return fr(p, g, row, label, s); }
    private int tar(JPanel p, GridBagConstraints g, int row, String label, JTextArea a) {
        g.gridx = 0; g.gridy = row + 1; g.gridwidth = 1; g.weightx = 0; g.insets = new Insets(6, 0, 2, 8);
        JLabel l = new JLabel(label); l.setFont(F_BOLD); l.setForeground(C_TXT_SEC); p.add(l, g);
        g.gridx = 1; g.gridwidth = 1; g.weightx = 1.0; g.insets = new Insets(6, 0, 2, 0); p.add(new JScrollPane(a), g);
        return row + 1;
    }

    private static TableCellRenderer matchPctBadge() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER); l.setFont(F_BOLD); l.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel && v != null) try {
                    double p = Double.parseDouble(v.toString().replace("%", "").trim());
                    l.setForeground(p >= 80 ? AdminDashboard.C_BALANCED : p >= 50 ? AdminDashboard.C_WARNING : C_DANGER);
                } catch (NumberFormatException ignored) {}
                return l;
            }
        };
    }
}

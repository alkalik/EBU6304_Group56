package com.recruitment.view;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Optional;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

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

public class AdminDashboard extends JFrame {

    private final User currentUser;
    private final LoginFrame loginFrame;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;
    private final AIAnalysisService aiAnalysisService;

    // ── Palette ────────────────────────────────────────────────────────────
    static final Color C_BG       = new Color(0xF0, 0xF2, 0xF8);
    static final Color C_SURFACE  = Color.WHITE;
    static final Color C_PRIMARY  = new Color(0x5C, 0x6B, 0xE8);
    static final Color C_ACCENT   = new Color(0x43, 0xC6, 0xAC);
    static final Color C_DANGER   = new Color(0xE5, 0x53, 0x53);
    static final Color C_WARNING  = new Color(0xFF, 0x99, 0x00);
    static final Color C_HDR      = new Color(0x1A, 0x1A, 0x2E);
    static final Color C_TXT_PRI  = new Color(0x22, 0x22, 0x33);
    static final Color C_TXT_SEC  = new Color(0x66, 0x72, 0x80);
    static final Color C_OVERLOAD = new Color(0xE5, 0x53, 0x53);
    static final Color C_BALANCED = new Color(0x00, 0x96, 0x60);
    static final Color C_AVAIL    = new Color(0x5C, 0x6B, 0xE8);
    static final Font  F_BODY     = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font  F_BOLD     = new Font("Segoe UI", Font.BOLD, 13);
    static final Font  F_SMALL    = new Font("Segoe UI", Font.PLAIN, 12);
    static final Font  F_H1       = new Font("Segoe UI", Font.BOLD, 18);
    static final Font  F_H2       = new Font("Segoe UI", Font.BOLD, 15);

    public AdminDashboard(User currentUser, LoginFrame loginFrame, JobService jobService,
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
        setTitle("Administrator – " + currentUser.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setBackground(C_BG);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JMenuBar mb = new JMenuBar();
        mb.setBackground(C_SURFACE);
        JMenu acctM = new JMenu("Account");
        JMenuItem logoutItem = new JMenuItem("Logout"); logoutItem.addActionListener(e -> logout()); acctM.add(logoutItem); mb.add(acctM);
        JMenu dataM = new JMenu("Data");
        JMenuItem backup = new JMenuItem("Backup Data"); backup.addActionListener(e -> BackupService.backupAllData(currentUser, true)); dataM.add(backup);
        JMenuItem restore = new JMenuItem("Restore Data..."); restore.addActionListener(e -> doRestore()); dataM.add(restore); mb.add(dataM);
        JMenu notifM = new JMenu("Notifications");
        JMenuItem bc = new JMenuItem("Broadcast Announcement"); bc.addActionListener(e -> broadcastAnnouncement()); notifM.add(bc);
        JMenuItem nta = new JMenuItem("Notify All TAs"); nta.addActionListener(e -> notifyByRole(User.Role.TA)); notifM.add(nta);
        JMenuItem nmo = new JMenuItem("Notify All MOs"); nmo.addActionListener(e -> notifyByRole(User.Role.MO)); notifM.add(nmo);
        mb.add(notifM);
        setJMenuBar(mb);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_BOLD);
        tabs.setBackground(C_BG);
        tabs.addTab("  AI Workload Balance  ", createWorkloadPanel());
        tabs.addTab("  All Users  ",           createUsersPanel());
        tabs.addTab("  All Jobs  ",            createAllJobsPanel());
        tabs.addTab("  All Applications  ",    createAllApplicationsPanel());
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(C_HDR);
        h.setBorder(new EmptyBorder(16, 28, 16, 28));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel("⚙  ");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        icon.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel title = new JLabel("Administrator Panel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel sep = new JLabel("  |  ");
        sep.setForeground(new Color(0x55, 0x55, 0x88));
        JLabel name = new JLabel(currentUser.getName());
        name.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        name.setForeground(new Color(0xA0, 0xA8, 0xCC));
        left.add(icon); left.add(title); left.add(sep); left.add(name);
        h.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton lo = pill("Logout", C_DANGER);
        lo.addActionListener(e -> logout());
        right.add(lo);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    // ── Tab 1 – AI Workload Balance ─────────────────────────────────────────
    private JPanel createWorkloadPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(20, 26, 16, 26));

        // Stats row at top
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(0, 0, 16, 0));
        JLabel statTotal  = statCard("Total TAs",   "—", C_PRIMARY);
        JLabel statOver   = statCard("Overloaded",  "—", C_OVERLOAD);
        JLabel statBal    = statCard("Balanced",    "—", C_BALANCED);
        JLabel statAvail  = statCard("Available",   "—", C_AVAIL);
        wrapStatCard(statsRow, "Total TAs",   statTotal,  C_PRIMARY);
        wrapStatCard(statsRow, "Overloaded",  statOver,   C_OVERLOAD);
        wrapStatCard(statsRow, "Balanced",    statBal,    C_BALANCED);
        wrapStatCard(statsRow, "Available",   statAvail,  C_AVAIL);
        panel.add(statsRow, BorderLayout.NORTH);

        // Main split: workload table (top) + suggestions table (bottom)
        String[] wCols = {"TA Name", "Email", "Department", "Skills", "Accepted", "Pending", "Score", "Status"};
        DefaultTableModel workloadModel = noEdit(wCols);
        JTable workloadTable = styledTable(workloadModel);
        workloadTable.getColumnModel().getColumn(7).setCellRenderer(statusBadgeRenderer());
        workloadTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        workloadTable.getColumnModel().getColumn(6).setPreferredWidth(60);

        String[] sCols = {"#", "Suggestion", "From TA", "To TA", "Position", "Match %", "Status"};
        DefaultTableModel suggestModel = noEdit(sCols);
        JTable suggestTable = styledTable(suggestModel);
        suggestTable.getColumnModel().getColumn(1).setPreferredWidth(380);
        suggestTable.getColumnModel().getColumn(5).setCellRenderer(matchPctRenderer());
        suggestTable.getColumnModel().getColumn(6).setCellRenderer(adoptedRenderer());

        JPanel topCard = wrapInCard("TA Workload Overview", wrapScroll(workloadTable));
        JPanel botCard = wrapInCard("AI Rebalancing Suggestions", wrapScroll(suggestTable));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topCard, botCard);
        split.setDividerLocation(300);
        split.setResizeWeight(0.5);
        split.setBorder(new EmptyBorder(0, 0, 0, 0));
        split.setBackground(C_BG);
        split.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);

        // Summary + AI area
        JTextArea summaryArea = new JTextArea(4, 60);
        summaryArea.setEditable(false);
        summaryArea.setFont(F_BODY);
        summaryArea.setBackground(new Color(0xF6, 0xF7, 0xFF));
        summaryArea.setForeground(C_TXT_PRI);
        summaryArea.setBorder(new EmptyBorder(10, 14, 10, 14));
        summaryArea.setText("Click 'Run AI Analysis' to evaluate workload distribution...");
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createLineBorder(new Color(0xD0, 0xD4, 0xF0)));
        summaryScroll.setPreferredSize(new Dimension(0, 100));

        final AIAnalysisService.WorkloadAnalysisResult[] lastResult = {null};

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btns.setBackground(C_BG);

        JButton refreshBtn = ghost(UiText.symbolText("↻", "Refresh", false));
        refreshBtn.addActionListener(e -> {
            userService.reload(); applicationService.reload();
            loadWorkloadTable(workloadModel, null, statTotal, statOver, statBal, statAvail);
        });

        final String aiBtnLabel = UiText.symbolText("✦", "Run AI Analysis", false, Color.WHITE);
        JButton aiBtn = pill(aiBtnLabel, C_PRIMARY);
        aiBtn.setPreferredSize(new Dimension(180, 36));
        aiBtn.addActionListener(e -> {
            aiBtn.setEnabled(false); aiBtn.setText("Analysing...");
            userService.reload(); applicationService.reload(); jobService.reload();
            List<User> tas = userService.findByRole(User.Role.TA);
            new Thread(() -> {
                AIAnalysisService.WorkloadAnalysisResult result =
                        aiAnalysisService.analyzeWorkload(tas, applicationService, jobService);
                SwingUtilities.invokeLater(() -> {
                    lastResult[0] = result;
                    loadWorkloadTable(workloadModel, result.workloads, statTotal, statOver, statBal, statAvail);
                    loadSuggestions(suggestModel, result.suggestions);
                    summaryArea.setText(result.summary);
                    aiBtn.setEnabled(true); aiBtn.setText(aiBtnLabel);
                });
                if (AppConfig.isDeepSeekEnabled()) {
                    StringBuilder wd = new StringBuilder();
                    wd.append(String.format("Average score: %.1f\n", result.avgWorkload));
                    result.workloads.forEach(w -> wd.append(String.format(
                            "- %s: accepted=%d, pending=%d, score=%.0f, status=%s\n",
                            w.ta.getName(), w.acceptedJobs, w.pendingApps, w.workloadScore, w.status)));
                    if (!result.suggestions.isEmpty()) {
                        wd.append("\nSuggestions:\n");
                        result.suggestions.forEach(s -> wd.append("- ").append(s.description).append("\n"));
                    }
                    SwingUtilities.invokeLater(() -> summaryArea.append(
                            "\n\n─── DeepSeek AI Recommendations (streaming) ───\n"));
                    new com.recruitment.util.DeepSeekClient().streamWorkloadBalance(
                            wd.toString(),
                            token -> SwingUtilities.invokeLater(() -> {
                                summaryArea.append(token);
                                summaryArea.setCaretPosition(summaryArea.getDocument().getLength());
                            }),
                            () -> SwingUtilities.invokeLater(() -> summaryArea.append("\n")),
                            err -> SwingUtilities.invokeLater(() ->
                                    summaryArea.append("\n[AI unavailable: " + err + "]\n"))
                    );
                }
            }, "ds-workload").start();
        });

        JButton detailBtn = ghost(UiText.symbolText("👤", "TA Details", true));
        detailBtn.addActionListener(e -> {
            int r = workloadTable.getSelectedRow();
            if (r < 0) { warn("Please select a TA first."); return; }
            String name = (String) workloadModel.getValueAt(r, 0);
            userService.findByRole(User.Role.TA).stream()
                    .filter(u -> u.getName().equals(name)).findFirst()
                    .ifPresent(u -> showTADetails(u.getId()));
        });

        JButton adoptBtn = pill(UiText.symbolText("✔", "Adopt Suggestion", false, Color.WHITE), C_ACCENT);
        adoptBtn.addActionListener(e -> {
            if (lastResult[0] == null) { warn("Please run AI analysis first."); return; }
            int r = suggestTable.getSelectedRow();
            if (r < 0) { warn("Please select a suggestion to adopt."); return; }
            AIAnalysisService.WorkloadSuggestion sug = lastResult[0].suggestions.get(r);
            if (sug.adopted) { info("This suggestion has already been adopted."); return; }
            if (!confirm("<html><b>Adopt this suggestion?</b><br><br>"
                    + "<span style='color:#555'>" + sug.description + "</span></html>")) return;
            if (!confirm("Please confirm again to finalize this workload rebalancing.")) return;
            sug.adopted = true;
            loadSuggestions(suggestModel, lastResult[0].suggestions);
            String fromId = findTAIdByName(sug.fromTA), toId = findTAIdByName(sug.toTA);
            if (!fromId.isEmpty()) notificationService.createNotification(fromId,
                    "Workload adjustment: admin has scheduled rebalancing for '" + sug.jobTitle + "'.",
                    com.recruitment.model.Notification.Type.INFO);
            if (!toId.isEmpty()) notificationService.createNotification(toId,
                    "Workload adjustment: admin suggests you take position '" + sug.jobTitle + "'.",
                    com.recruitment.model.Notification.Type.INFO);
            info("Suggestion adopted. TAs have been notified.");
        });

        JButton exportBtn = ghost(UiText.symbolText("⬇", "Export Report", false));
        exportBtn.addActionListener(e -> {
            if (lastResult[0] == null) { warn("Please run analysis first."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File("workload_report.txt"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
                try (java.io.FileWriter fw = new java.io.FileWriter(fc.getSelectedFile())) {
                    fw.write(buildWorkloadReport(lastResult[0]));
                    info("Exported: " + fc.getSelectedFile().getAbsolutePath());
                } catch (java.io.IOException ex) { warn("Export failed: " + ex.getMessage()); }
        });

        btns.add(refreshBtn); btns.add(aiBtn); btns.add(detailBtn);
        btns.add(Box.createHorizontalStrut(10));
        btns.add(adoptBtn); btns.add(exportBtn);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(C_BG);
        south.add(summaryScroll, BorderLayout.CENTER);
        south.add(btns, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        loadWorkloadTable(workloadModel, null, statTotal, statOver, statBal, statAvail);
        return panel;
    }

    // Stat card helpers
    private JLabel statCard(String label, String val, Color accent) {
        JLabel l = new JLabel(val, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 28));
        l.setForeground(accent);
        return l;
    }

    private void wrapStatCard(JPanel parent, String title, JLabel valLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_SURFACE); g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(accent); g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel tl = new JLabel(title, SwingConstants.CENTER);
        tl.setFont(F_SMALL); tl.setForeground(C_TXT_SEC);
        card.add(tl, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        parent.add(card);
    }

    private void loadWorkloadTable(DefaultTableModel model, List<AIAnalysisService.TAWorkload> workloads,
                                   JLabel statTotal, JLabel statOver, JLabel statBal, JLabel statAvail) {
        model.setRowCount(0);
        if (workloads != null) {
            workloads.forEach(w -> model.addRow(new Object[]{
                    w.ta.getName(), w.ta.getEmail(),
                    w.ta.getDepartment() != null ? w.ta.getDepartment() : "",
                    w.ta.getSkills() != null ? String.join(", ", w.ta.getSkills()) : "",
                    w.acceptedJobs, w.pendingApps, String.format("%.0f", w.workloadScore), w.status}));
            long over = workloads.stream().filter(w -> "Overloaded".equals(w.status)).count();
            long bal  = workloads.stream().filter(w -> "Balanced".equals(w.status)).count();
            long av   = workloads.stream().filter(w -> "Available".equals(w.status)).count();
            statTotal.setText(String.valueOf(workloads.size()));
            statOver.setText(String.valueOf(over));
            statBal.setText(String.valueOf(bal));
            statAvail.setText(String.valueOf(av));
        } else {
            List<User> tas = userService.findByRole(User.Role.TA);
            tas.forEach(ta -> {
                long acc = applicationService.getAcceptedCountByApplicant(ta.getId());
                long pen = applicationService.getApplicationsByApplicant(ta.getId()).stream()
                        .filter(a -> a.getStatus() == Application.Status.PENDING).count();
                model.addRow(new Object[]{ta.getName(), ta.getEmail(),
                        ta.getDepartment() != null ? ta.getDepartment() : "",
                        ta.getSkills() != null ? String.join(", ", ta.getSkills()) : "",
                        acc, pen, String.format("%.0f", acc * 3.0 + pen), "—"});
            });
            statTotal.setText(String.valueOf(tas.size()));
            statOver.setText("0"); statBal.setText(String.valueOf(tas.size())); statAvail.setText("0");
        }
    }

    private void loadSuggestions(DefaultTableModel model, List<AIAnalysisService.WorkloadSuggestion> suggestions) {
        model.setRowCount(0);
        int i = 1;
        for (AIAnalysisService.WorkloadSuggestion s : suggestions)
            model.addRow(new Object[]{i++, s.description, s.fromTA, s.toTA, s.jobTitle,
                    String.format("%.0f%%", s.matchScore), s.adopted ? "Adopted" : "Pending"});
    }

    private String findTAIdByName(String name) {
        return userService.findByRole(User.Role.TA).stream()
                .filter(u -> u.getName().equals(name)).map(User::getId).findFirst().orElse("");
    }

    private String buildWorkloadReport(AIAnalysisService.WorkloadAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("==============================================\n");
        sb.append("  AI Workload Balance Report\n");
        sb.append("==============================================\n");
        sb.append("Generated: ").append(java.time.LocalDateTime.now().toString().replace("T", " ")).append("\n\n");
        sb.append(result.summary).append("\n\n");
        sb.append(String.format("%-22s %-10s %-10s %-8s %-12s\n", "Name", "Accepted", "Pending", "Score", "Status"));
        sb.append("----------------------------------------------\n");
        result.workloads.forEach(w -> sb.append(String.format("%-22s %-10d %-10d %-8.0f %-12s\n",
                w.ta.getName(), w.acceptedJobs, w.pendingApps, w.workloadScore, w.status)));
        sb.append("\n--- Suggestions ---\n");
        int i = 1;
        for (AIAnalysisService.WorkloadSuggestion s : result.suggestions)
            sb.append(i++).append(". ").append(s.description).append("\n   → ").append(s.adopted ? "Adopted" : "Pending").append("\n\n");
        return sb.toString();
    }

    private void showTADetails(String taId) {
        Optional<User> taOpt = userService.findById(taId);
        if (!taOpt.isPresent()) return;
        User ta = taOpt.get();

        JDialog dlg = new JDialog(this, "TA Profile – " + ta.getName(), true);
        dlg.setSize(560, 480); dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        // Header strip
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(C_HDR); hdr.setBorder(new EmptyBorder(18, 24, 18, 24));
        JLabel nm = new JLabel(ta.getName());
        nm.setFont(new Font("Segoe UI", Font.BOLD, 18)); nm.setForeground(new Color(0xCB, 0xC5, 0xFF));
        JLabel role = new JLabel("Teaching Assistant");
        role.setFont(F_BODY); role.setForeground(new Color(0xA0, 0xA8, 0xCC));
        JPanel hl = new JPanel(); hl.setOpaque(false); hl.setLayout(new BoxLayout(hl, BoxLayout.Y_AXIS));
        hl.add(nm); hl.add(role); hdr.add(hl, BorderLayout.WEST); root.add(hdr, BorderLayout.NORTH);

        // Info grid
        JPanel body = new JPanel(new BorderLayout(0, 12)); body.setBackground(C_BG); body.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel info = new JPanel(new GridLayout(0, 2, 8, 8)); info.setBackground(C_SURFACE);
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE4, 0xE8, 0xF4), 1),
                new EmptyBorder(14, 18, 14, 18)));
        addInfoRow(info, "Email",      ta.getEmail() != null ? ta.getEmail() : "—");
        addInfoRow(info, "Phone",      ta.getPhone() != null ? ta.getPhone() : "—");
        addInfoRow(info, "Department", ta.getDepartment() != null ? ta.getDepartment() : "—");
        addInfoRow(info, "Skills",     ta.getSkills() != null ? String.join(", ", ta.getSkills()) : "—");
        addInfoRow(info, "CV",         ta.getCvPath() != null ? ta.getCvPath() : "Not uploaded");
        body.add(info, BorderLayout.NORTH);

        // Applications
        String[] ac = {"App ID", "Position", "Apply Date", "Status"};
        DefaultTableModel am = noEdit(ac);
        JTable at = styledTable(am);
        at.setRowHeight(26);
        at.getColumnModel().getColumn(3).setCellRenderer(statusBadgeRenderer());
        applicationService.getApplicationsByApplicant(taId).forEach(app -> {
            String jt = jobService.findById(app.getJobId()).map(Job::getTitle).orElse("Unknown");
            am.addRow(new Object[]{app.getId(), jt, app.getApplyDate(), app.getStatus()});
        });
        JPanel appsCard = wrapInShadowCard("Applications", wrapScroll(at));
        body.add(appsCard, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10)); bot.setBackground(C_BG);
        JButton cls = ghost("Close"); cls.addActionListener(e -> dlg.dispose()); bot.add(cls);
        root.add(bot, BorderLayout.SOUTH);
        dlg.setContentPane(root); dlg.setVisible(true);
    }

    private void addInfoRow(JPanel p, String k, String v) {
        JLabel kl = new JLabel(k); kl.setFont(F_BOLD); kl.setForeground(C_TXT_SEC);
        JLabel vl = new JLabel(v); vl.setFont(F_BODY); vl.setForeground(C_TXT_PRI);
        p.add(kl); p.add(vl);
    }

    // ── Tab 2 – All Users ──────────────────────────────────────────────────
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setBackground(C_SURFACE);
        searchBar.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(10, 16, 10, 16)));
        JTextField kwF = new JTextField(28); kwF.setFont(F_BODY);
        kwF.putClientProperty("JTextField.placeholderText", "Search by name, username or role...");
        JLabel resLbl = new JLabel(); resLbl.setFont(F_SMALL); resLbl.setForeground(C_TXT_SEC);
        JButton sb = pill("Search", C_PRIMARY); JButton cb = ghost("Clear");
        searchBar.add(new JLabel(UiText.symbolText("🔍", "", true))); searchBar.add(kwF); searchBar.add(sb); searchBar.add(cb); searchBar.add(resLbl);
        panel.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Username", "Name", "Role", "Email", "Department"};
        DefaultTableModel model = noEdit(cols);
        JTable table = styledTable(model);
        panel.add(wrapInCard("Users", wrapScroll(table)), BorderLayout.CENTER);

        Runnable doSearch = () -> loadUsers(model, kwF.getText(), resLbl);
        sb.addActionListener(e -> doSearch.run());
        cb.addActionListener(e -> { kwF.setText(""); doSearch.run(); });
        kwF.addActionListener(e -> doSearch.run());

        JPanel btns = btnRow();
        JButton refresh = ghost(UiText.symbolText("↻", "Refresh", false)); refresh.addActionListener(e -> { userService.reload(); doSearch.run(); });
        JButton del = pill("Delete User", C_DANGER); del.addActionListener(e -> {
            int r = table.getSelectedRow(); if (r < 0) { warn("Please select a user."); return; }
            String uid = (String) model.getValueAt(r, 0);
            if (uid.equals(currentUser.getId())) { warn("Cannot delete the currently logged-in user."); return; }
            if (confirm("Permanently delete this user?")) { userService.deleteUser(uid); doSearch.run(); }
        });
        btns.add(refresh); btns.add(del);
        panel.add(btns, BorderLayout.SOUTH);
        loadUsers(model, "", resLbl);
        return panel;
    }

    private void loadUsers(DefaultTableModel model, String kw, JLabel resLbl) {
        model.setRowCount(0);
        String k = kw == null ? "" : kw.trim();
        List<User> users = k.isEmpty() ? userService.getAllUsers() : userService.searchUsers(k);
        users.forEach(u -> model.addRow(new Object[]{u.getId(), u.getUsername(), u.getName(),
                u.getRole(), u.getEmail(), u.getDepartment() != null ? u.getDepartment() : ""}));
        if (resLbl != null) resLbl.setText(k.isEmpty() ? "  Total: " + users.size() : "  Found: " + users.size());
    }

    // ── Tab 3 – All Jobs ───────────────────────────────────────────────────
    private JPanel createAllJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));
        String[] cols = {"ID", "Title", "Module", "Type", "Posted By", "Positions", "Filled", "Status"};
        DefaultTableModel model = noEdit(cols);
        JTable table = styledTable(model);
        panel.add(wrapInCard("All Jobs", wrapScroll(table)), BorderLayout.CENTER);
        JPanel btns = btnRow();
        JButton refresh = ghost(UiText.symbolText("↻", "Refresh", false));
        refresh.addActionListener(e -> { jobService.reload(); userService.reload(); loadAllJobs(model); });
        btns.add(refresh); panel.add(btns, BorderLayout.SOUTH);
        loadAllJobs(model); return panel;
    }

    private void loadAllJobs(DefaultTableModel model) {
        model.setRowCount(0);
        jobService.getAllJobs().forEach(j -> {
            String poster = userService.findById(j.getPostedBy()).map(User::getName).orElse("Unknown");
            model.addRow(new Object[]{j.getId(), j.getTitle(), j.getModuleName(), j.getJobType(),
                    poster, j.getMaxPositions(), j.getFilledPositions(), j.getStatus()});
        });
    }

    // ── Tab 4 – All Applications ───────────────────────────────────────────
    private JPanel createAllApplicationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG); panel.setBorder(new EmptyBorder(18, 26, 16, 26));
        String[] cols = {"App ID", "Job Title", "Applicant", "Apply Date", "Status", "Reviewed By", "Withdrawn At"};
        DefaultTableModel model = noEdit(cols);
        JTable table = styledTable(model);
        table.getColumnModel().getColumn(4).setCellRenderer(statusBadgeRenderer());
        panel.add(wrapInCard("All Applications", wrapScroll(table)), BorderLayout.CENTER);
        JPanel btns = btnRow();
        JButton refresh = ghost(UiText.symbolText("↻", "Refresh", false));
        refresh.addActionListener(e -> { applicationService.reload(); jobService.reload(); userService.reload(); loadAllApps(model); });
        btns.add(refresh); panel.add(btns, BorderLayout.SOUTH);
        loadAllApps(model); return panel;
    }

    private void loadAllApps(DefaultTableModel model) {
        model.setRowCount(0);
        applicationService.getAllApplications().forEach(app -> {
            String jt = jobService.findById(app.getJobId()).map(Job::getTitle).orElse("Unknown");
            String name = userService.findById(app.getApplicantId()).map(User::getName).orElse("Unknown");
            String rev = app.getReviewedBy() != null ? userService.findById(app.getReviewedBy()).map(User::getName).orElse("") : "";
            model.addRow(new Object[]{app.getId(), jt, name, app.getApplyDate(), app.getStatus(), rev,
                    app.getWithdrawnAt() != null ? app.getWithdrawnAt() : ""});
        });
    }

    // ── Notifications ──────────────────────────────────────────────────────
    private void broadcastAnnouncement() {
        String msg = JOptionPane.showInputDialog(this, "Announcement message:", "Broadcast", JOptionPane.PLAIN_MESSAGE);
        if (msg != null && !msg.trim().isEmpty()) {
            notificationService.broadcastNotification(msg, com.recruitment.model.Notification.Type.ANNOUNCEMENT);
            info("Announcement sent to all users!");
        }
    }

    private void notifyByRole(User.Role role) {
        String msg = JOptionPane.showInputDialog(this, "Message for all " + role + "s:", "Notify " + role, JOptionPane.PLAIN_MESSAGE);
        if (msg != null && !msg.trim().isEmpty()) {
            notificationService.notifyUsersByRole(msg, com.recruitment.model.Notification.Type.INFO, role);
            info("Notification sent to all " + role + "s!");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────
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

    // ── Widget factory ─────────────────────────────────────────────────────
    static JButton pill(String t, Color bg) {
        JButton b = new JButton(t); b.setFont(F_BOLD); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(Math.max(120, b.getPreferredSize().width + 18), 34)); return b;
    }

    static JButton ghost(String t) {
        JButton b = new JButton(t); b.setFont(F_BODY);
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }

    private JPanel btnRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8)); p.setBackground(C_BG); return p;
    }

    static JPanel wrapInCard(String title, Component content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE4, 0xE8, 0xF4), 1),
                new EmptyBorder(12, 14, 12, 14)));
        if (title != null && !title.isEmpty()) {
            JLabel tl = new JLabel(title); tl.setFont(F_H2); tl.setForeground(C_TXT_PRI);
            tl.setBorder(new EmptyBorder(0, 2, 8, 0));
            card.add(tl, BorderLayout.NORTH);
        }
        card.add(content, BorderLayout.CENTER); return card;
    }

    static JPanel wrapInShadowCard(String title, Component content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(ShadowBorder.card(), new EmptyBorder(12, 14, 12, 14)));
        if (title != null && !title.isEmpty()) {
            JLabel tl = new JLabel(title); tl.setFont(F_H2); tl.setForeground(C_TXT_PRI);
            tl.setBorder(new EmptyBorder(0, 2, 8, 0));
            card.add(tl, BorderLayout.NORTH);
        }
        card.add(content, BorderLayout.CENTER); return card;
    }

    static DefaultTableModel noEdit(String[] c) {
        return new DefaultTableModel(c, 0) { @Override public boolean isCellEditable(int r, int col) { return false; } };
    }

    static JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m); t.setFont(F_BODY); t.setRowHeight(32);
        t.setGridColor(new Color(0xEA, 0xED, 0xF5));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(new Color(0xE3, 0xE6, 0xFF));
        t.setSelectionForeground(C_TXT_PRI);
        t.getTableHeader().setFont(F_BOLD);
        t.getTableHeader().setBackground(new Color(0xF0, 0xF1, 0xFA));
        t.getTableHeader().setForeground(C_TXT_SEC);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0xD0, 0xD4, 0xF0)));
        t.getTableHeader().setReorderingAllowed(false);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setShowVerticalLines(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, v, sel, foc, row, col);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xF9, 0xFE));
                return l;
            }
        });
        return t;
    }

    static JScrollPane wrapScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xE0, 0xE4, 0xF0)));
        sp.getViewport().setBackground(Color.WHITE); return sp;
    }

    static TableCellRenderer statusBadgeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER); l.setFont(F_BOLD);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel && v != null) switch (v.toString()) {
                    case "ACCEPTED":   l.setForeground(new Color(0x00, 0x96, 0x60)); break;
                    case "REJECTED":   l.setForeground(C_DANGER); break;
                    case "WITHDRAWN":  l.setForeground(new Color(0x99, 0x99, 0xAA)); break;
                    case "PENDING":    l.setForeground(new Color(0xDD, 0x88, 0x00)); break;
                    case "Overloaded": l.setForeground(C_OVERLOAD); break;
                    case "Balanced":   l.setForeground(C_BALANCED); break;
                    case "Available":  l.setForeground(C_AVAIL); break;
                    default: l.setForeground(C_TXT_PRI);
                }
                return l;
            }
        };
    }

    private static TableCellRenderer matchPctRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER); l.setFont(F_BOLD);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel && v != null) try {
                    double p = Double.parseDouble(v.toString().replace("%", "").trim());
                    l.setForeground(p >= 70 ? C_BALANCED : p >= 40 ? C_WARNING : C_DANGER);
                } catch (NumberFormatException ignored) {}
                return l;
            }
        };
    }

    private static TableCellRenderer adoptedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER); l.setFont(F_BOLD);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel && v != null)
                    l.setForeground("Adopted".equals(v.toString()) ? C_BALANCED : C_WARNING);
                return l;
            }
        };
    }

    static Font F_H2_static() { return F_H2; }
}

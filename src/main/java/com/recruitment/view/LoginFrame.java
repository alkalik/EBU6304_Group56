package com.recruitment.view;

import com.recruitment.model.User;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.util.RegistrationRules;
import com.recruitment.util.ShadowBorder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginFrame extends JFrame {
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    private JTextField usernameField;
    private JPasswordField passwordField;

    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmField;
    private JTextField regNameField;
    private JTextField regEmailField;
    private JComboBox<User.Role> regRoleCombo;

    private JLabel passwordRuleStatusLabel;
    private JLabel passwordRuleReasonLabel;
    private JLabel confirmStatusLabel;
    private JLabel confirmReasonLabel;
    private JLabel regUsernameStatusLabel;
    private JLabel regUsernameReasonLabel;
    private JLabel regEmailStatusLabel;
    private JLabel regEmailReasonLabel;

    private JButton loginSubmitButton;
    private JButton registerSubmitButton;
    private JButton registerNextButton;
    private JButton registerWizardBackButton;

    private CardLayout registerWizardLayout;
    private JPanel registerWizardCardPanel;
    private JPanel registerProgressLine;
    private JLabel registerProgressCaption;
    private int registerWizardDisplayedStep = 1;
    private int registerContentMaxWidth = 232;

    private CardLayout authCardLayout;
    private JPanel authCardPanel;
    private JToggleButton signInTabBtn;
    private JToggleButton createAccountTabBtn;

    private JPanel showcasePanel;
    private Timer animationTimer;
    private float animationPhase = 0f;
    private boolean compactMode;

    private static final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 60 * 1000;
    private static final long ATTEMPT_WINDOW_MS = 60 * 1000;

    private static class LoginAttempt {
        int failureCount;
        long firstFailureTime;
        long lockoutUntil;
    }

    private enum RegistrationStatusKind {
        USERNAME, PASSWORD_POLICY, CONFIRM, EMAIL
    }

    private static final Color PRIMARY = new Color(0x6C, 0x5C, 0xE7);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(0x2D, 0x34, 0x36);
    private static final Color TEXT_SECONDARY = new Color(0x63, 0x6E, 0x72);
    private static final Color SUCCESS = new Color(0x22, 0x8B, 0x22);
    private static final Color ERROR = new Color(0xCC, 0x33, 0x33);
    /** Fixed label column so the field column stays aligned when validation text changes. */
    private static final int REGISTER_LABEL_COLUMN_WIDTH = 92;
    /**
     * Space between Full name and Email: strut + Email row top inset ≈ same rhythm as
     * “field → validation strip (bottom 10) → next field” on step 1.
     */
    private static final int REGISTER_NAME_EMAIL_STRUT = 28;

    public LoginFrame(UserService userService, JobService jobService, ApplicationService applicationService, NotificationService notificationService) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        initUI();
    }

    private void initUI() {
        setTitle("TA Recruitment System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1140, Math.max(560, screen.width - 80));
        int height = Math.min(760, Math.max(680, screen.height - 100));
        compactMode = width < 980;

        setSize(width, height);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0xF4, 0xF5, 0xF8),
                        0, getHeight(), new Color(0xEE, 0xEC, 0xFB));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        if (compactMode) {
            root.add(createAuthContainer(true), BorderLayout.CENTER);
        } else {
            JPanel content = new JPanel(new GridBagLayout());
            content.setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;

            gbc.gridx = 0;
            gbc.weightx = 0.56;
            gbc.insets = new Insets(0, 0, 0, 14);
            showcasePanel = createShowcasePanel();
            content.add(showcasePanel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.44;
            gbc.insets = new Insets(0, 14, 0, 0);
            content.add(createAuthContainer(false), gbc);

            root.add(content, BorderLayout.CENTER);
            startShowcaseAnimation();
        }

        setContentPane(root);
    }

    private JPanel createShowcasePanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int cx = (int) (w * 0.62);
                int cy = (int) (h * 0.30);

                float p1 = (float) Math.sin(animationPhase) * 16f;
                float p2 = (float) Math.cos(animationPhase * 0.85f) * 18f;
                float p3 = (float) Math.sin(animationPhase * 1.25f) * 14f;

                g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 35));
                g2.fillOval(cx - 170 + (int) p1, cy - 110 + (int) p2, 240, 240);
                g2.setColor(new Color(0xCB, 0xC3, 0xF7, 56));
                g2.fillOval(cx - 10 + (int) p2, cy - 30 + (int) p3, 180, 180);
                g2.setColor(new Color(0xEE, 0xEC, 0xFB, 90));
                g2.fillOval(cx + 95 + (int) p3, cy - 130 + (int) p1, 130, 130);

                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 45));
                int lineY = (int) (h * 0.75 + Math.sin(animationPhase * 0.9f) * 6);
                g2.drawLine(36, lineY, w - 40, lineY);
                g2.dispose();
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.card(),
                BorderFactory.createEmptyBorder(42, 38, 34, 38)
        ));

        JLabel badge = new JLabel("Enterprise Hiring Workflow");
        badge.setOpaque(true);
        badge.setBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 34));
        badge.setForeground(PRIMARY);
        badge.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(badge);
        panel.add(Box.createVerticalStrut(18));

        JLabel title = new JLabel("<html>Recruitment that feels<br/>fast, structured and clear.</html>");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));

        JLabel desc = new JLabel("<html><div style='width:440px;'>Manage TA hiring with role-based workflows, instant application visibility, and streamlined decisions for MOs and Admins.</div></html>");
        desc.setForeground(TEXT_SECONDARY);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(28));

        panel.add(createFeatureTile("Centralized Data", "Users, jobs, and applications stay synchronized in one desktop workspace."));
        panel.add(Box.createVerticalStrut(12));
        panel.add(createFeatureTile("Role-aware Access", "TA, MO, and Admin each get focused tools with isolated permissions."));
        panel.add(Box.createVerticalStrut(12));
        panel.add(createFeatureTile("Fast Decisions", "Review status and workload quickly, then accept or reject in a single flow."));

        panel.add(Box.createVerticalGlue());
        JLabel footer = new JLabel("BUPT International School");
        footer.setForeground(TEXT_SECONDARY);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(footer);

        return panel;
    }

    private JPanel createFeatureTile(String title, String text) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setOpaque(true);
        tile.setBackground(new Color(255, 255, 255, 186));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 60), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        tile.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tile.add(titleLabel);
        tile.add(Box.createVerticalStrut(6));

        JLabel textLabel = new JLabel("<html><div style='width:410px;'>" + text + "</div></html>");
        textLabel.setForeground(TEXT_SECONDARY);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tile.add(textLabel);

        return tile;
    }

    private JPanel createAuthContainer(boolean compact) {
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createCompoundBorder(
                ShadowBorder.subtle(),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        int wrapperWidth = compact ? 480 : 390;
        wrapper.setPreferredSize(new Dimension(wrapperWidth, 620));
        wrapper.setMaximumSize(new Dimension(wrapperWidth, 620));

        JLabel logo = new JLabel("TA Recruitment");
        logo.setForeground(TEXT_PRIMARY);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(logo);
        wrapper.add(Box.createVerticalStrut(8));

        int tipWidth = compact ? 430 : 340;
        JLabel tip = new JLabel("<html><div style='width:" + tipWidth + "px;'>Sign in or create an account to continue.</div></html>");
        tip.setForeground(TEXT_SECONDARY);
        tip.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(tip);
        wrapper.add(Box.createVerticalStrut(18));

        JPanel segmented = createSegmentedTabs();
        segmented.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(segmented);
        wrapper.add(Box.createVerticalStrut(16));

        authCardLayout = new CardLayout();
        authCardPanel = new JPanel(authCardLayout);
        authCardPanel.setOpaque(false);
        authCardPanel.add(createLoginCard(compact), "login");
        authCardPanel.add(createRegisterCard(compact), "register");
        authCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        authCardPanel.setPreferredSize(new Dimension(wrapperWidth, 470));
        wrapper.add(authCardPanel);

        styleTabButtons(signInTabBtn, createAccountTabBtn);
        if (loginSubmitButton != null) {
            getRootPane().setDefaultButton(loginSubmitButton);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        shell.add(wrapper, gbc);

        return shell;
    }

    private JPanel createSegmentedTabs() {
        JPanel segmented = new JPanel(new GridLayout(1, 2, 8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF4, 0xF5, 0xF8));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 70));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        segmented.setOpaque(false);
        segmented.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        segmented.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        signInTabBtn = createTabButton("Sign In", true);
        createAccountTabBtn = createTabButton("Create Account", false);

        ButtonGroup tabs = new ButtonGroup();
        tabs.add(signInTabBtn);
        tabs.add(createAccountTabBtn);

        signInTabBtn.addActionListener(e -> switchToLoginTab());
        createAccountTabBtn.addActionListener(e -> switchToRegisterTab());

        segmented.add(signInTabBtn);
        segmented.add(createAccountTabBtn);
        return segmented;
    }

    private JToggleButton createTabButton(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        return btn;
    }

    private void styleTabButtons(AbstractButton selected, AbstractButton other) {
        selected.setBackground(PRIMARY);
        selected.setForeground(Color.WHITE);
        selected.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        other.setBackground(new Color(255, 255, 255, 0));
        other.setForeground(TEXT_SECONDARY);
        other.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }

    private JPanel createLoginCard(boolean compact) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        JLabel signInLabel = new JLabel("Sign In");
        signInLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        signInLabel.setForeground(TEXT_PRIMARY);
        signInLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(signInLabel);
        card.add(Box.createVerticalStrut(16));

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(userLabel);
        card.add(Box.createVerticalStrut(6));

        usernameField = new JTextField();
        styleInputField(usernameField);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passLabel.setForeground(TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField();
        styleInputField(passwordField);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(24));

        loginSubmitButton = new JButton("Login");
        stylePrimaryButton(loginSubmitButton, 44);
        loginSubmitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginSubmitButton.addActionListener(e -> handleLogin());
        card.add(loginSubmitButton);

        card.add(Box.createVerticalStrut(10));
        JLabel helper = new JLabel("Forgot password? Contact admin to reset.");
        helper.setForeground(TEXT_SECONDARY);
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(helper);

        card.add(Box.createVerticalStrut(10));
        JLabel registerLink = new JLabel(
                "<html><span style='color:#636E72;'>Don't have an account? </span><span style='color:#6C5CE7;'><u>Register</u></span></html>");
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        registerLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchToRegisterTab();
            }
        });
        card.add(registerLink);

        int cardWidth = compact ? 460 : 390;
        card.setPreferredSize(new Dimension(cardWidth, 360));
        return card;
    }

    private JPanel createRegisterCard(boolean compact) {
        registerContentMaxWidth = compact ? 300 : 232;

        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);

        container.add(buildRegisterProgressHeader(), BorderLayout.NORTH);

        registerWizardLayout = new CardLayout();
        registerWizardCardPanel = new JPanel(registerWizardLayout);
        registerWizardCardPanel.setOpaque(false);
        registerWizardCardPanel.add(buildRegisterStep1Panel(registerContentMaxWidth), "step1");
        registerWizardCardPanel.add(buildRegisterStep2Panel(registerContentMaxWidth), "step2");

        JScrollPane scroll = new JScrollPane(registerWizardCardPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        container.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        JLabel signInLink = new JLabel(
                "<html><span style='color:#636E72;'>Already have an account? </span><span style='color:#6C5CE7;'><u>Sign In</u></span></html>");
        signInLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signInLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signInLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        signInLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchToLoginTab();
            }
        });
        south.add(signInLink);
        container.add(south, BorderLayout.SOUTH);

        attachRegisterValidationListeners();
        refreshRegisterValidationStatus();
        updateRegisterWizardUI(1);

        int width = compact ? 470 : 390;
        container.setPreferredSize(new Dimension(width, 470));
        return container;
    }

    private JPanel buildRegisterProgressHeader() {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);

        registerProgressLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = Math.max(2, getHeight());
                int arc = h;
                g2.setColor(new Color(0xE4, 0xE2, 0xEE));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                float frac = registerWizardDisplayedStep <= 1 ? 0.5f : 1f;
                int fw = Math.max(h, Math.round(w * frac));
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, fw, h, arc, arc);
                g2.dispose();
            }
        };
        registerProgressLine.setOpaque(false);
        int lineH = 3;
        registerProgressLine.setMinimumSize(new Dimension(40, lineH));
        registerProgressLine.setPreferredSize(new Dimension(200, lineH));
        registerProgressLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, lineH));
        registerProgressLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(registerProgressLine);
        wrap.add(Box.createVerticalStrut(8));

        registerProgressCaption = new JLabel(" ");
        registerProgressCaption.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        registerProgressCaption.setForeground(TEXT_SECONDARY);
        registerProgressCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(registerProgressCaption);

        return wrap;
    }

    private JPanel buildRegisterStep1Panel(int fieldMax) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(4, 6, 10, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.insets = new Insets(0, 4, 8, 4);
        JLabel title = new JLabel("Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(title, gbc);

        gbc.gridwidth = 1;
        regUsernameField = new JTextField();
        addRegisterField(form, gbc, row++, "Username", regUsernameField, fieldMax, 0);
        addRegistrationStatusRow(form, gbc, row++, RegistrationStatusKind.USERNAME, fieldMax);
        regPasswordField = new JPasswordField();
        addRegisterField(form, gbc, row++, "Password", regPasswordField, fieldMax, 0);
        addRegistrationStatusRow(form, gbc, row++, RegistrationStatusKind.PASSWORD_POLICY, fieldMax);
        regConfirmField = new JPasswordField();
        addRegisterField(form, gbc, row++, "Confirm", regConfirmField, fieldMax, 0);
        addRegistrationStatusRow(form, gbc, row++, RegistrationStatusKind.CONFIRM, fieldMax);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(14, 4, 6, 4);
        gbc.fill = GridBagConstraints.NONE;
        registerNextButton = new JButton("Continue");
        stylePrimaryButton(registerNextButton, 40);
        registerNextButton.setPreferredSize(new Dimension(fieldMax, 40));
        registerNextButton.addActionListener(e -> {
            if (validateRegisterStep1ForNext()) {
                updateRegisterWizardUI(2);
            }
        });
        form.add(registerNextButton, gbc);

        gbc.gridy = row + 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        form.add(Box.createVerticalGlue(), gbc);

        return form;
    }

    private JPanel buildRegisterStep2Panel(int fieldMax) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(4, 6, 10, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.insets = new Insets(0, 4, 8, 4);
        JLabel title = new JLabel("Your profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(title, gbc);

        gbc.gridwidth = 1;
        regNameField = new JTextField();
        addRegisterField(form, gbc, row++, "Full name", regNameField, fieldMax, 0);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.anchor = GridBagConstraints.WEST;
        form.add(Box.createVerticalStrut(REGISTER_NAME_EMAIL_STRUT), gbc);

        regEmailField = new JTextField();
        addRegisterField(form, gbc, row++, "Email", regEmailField, fieldMax, 10);
        addRegistrationStatusRow(form, gbc, row++, RegistrationStatusKind.EMAIL, fieldMax);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 4, 2, 4);
        JLabel roleLabel = new JLabel("Role");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(TEXT_SECONDARY);
        int rlh = roleLabel.getPreferredSize().height;
        Dimension rslot = new Dimension(REGISTER_LABEL_COLUMN_WIDTH, rlh);
        roleLabel.setPreferredSize(rslot);
        roleLabel.setMinimumSize(rslot);
        roleLabel.setMaximumSize(rslot);
        form.add(roleLabel, gbc);

        gbc.gridx = 1;
        regRoleCombo = new JComboBox<>(new User.Role[]{User.Role.TA, User.Role.MO});
        regRoleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        regRoleCombo.setPreferredSize(new Dimension(fieldMax, 42));
        regRoleCombo.setMaximumSize(new Dimension(fieldMax, 42));
        form.add(regRoleCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(18, 4, 6, 4);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        registerWizardBackButton = new JButton("Back");
        styleSecondaryButton(registerWizardBackButton, 40);
        registerWizardBackButton.setPreferredSize(new Dimension(100, 40));
        registerWizardBackButton.addActionListener(e -> updateRegisterWizardUI(1));

        registerSubmitButton = new JButton("Register");
        stylePrimaryButton(registerSubmitButton, 40);
        registerSubmitButton.setPreferredSize(new Dimension(Math.max(120, fieldMax - 110), 40));
        registerSubmitButton.addActionListener(e -> handleRegisterInline());

        btnRow.add(registerWizardBackButton);
        btnRow.add(registerSubmitButton);
        form.add(btnRow, gbc);

        gbc.gridy = row + 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        form.add(Box.createVerticalGlue(), gbc);

        return form;
    }

    private void updateRegisterWizardUI(int step) {
        if (registerWizardLayout == null || registerProgressLine == null) {
            return;
        }
        registerWizardDisplayedStep = step;
        registerProgressLine.repaint();
        if (step == 1) {
            registerWizardLayout.show(registerWizardCardPanel, "step1");
            registerProgressCaption.setText("Step 1 of 2 — Choose username and password");
            if (registerNextButton != null) {
                getRootPane().setDefaultButton(registerNextButton);
            }
        } else {
            registerWizardLayout.show(registerWizardCardPanel, "step2");
            registerProgressCaption.setText("Step 2 of 2 — Name, email and role");
            refreshRegisterValidationStatus();
            if (registerSubmitButton != null) {
                getRootPane().setDefaultButton(registerSubmitButton);
            }
        }
    }

    private boolean validateRegisterStep1ForNext() {
        refreshRegisterValidationStatus();
        String username = regUsernameField.getText().trim();
        String uFmt = RegistrationRules.usernameFormatFailure(regUsernameField.getText());
        if (uFmt != null) {
            JOptionPane.showMessageDialog(this, uFmt, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        String uTaken = RegistrationRules.usernameAvailabilityFailure(username, userService);
        if (uTaken != null) {
            JOptionPane.showMessageDialog(this, uTaken, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        String password = new String(regPasswordField.getPassword()).trim();
        if (RegistrationRules.passwordPolicyFailure(password) != null) {
            JOptionPane.showMessageDialog(this, RegistrationRules.passwordPolicyFailure(password),
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        String confirm = new String(regConfirmField.getPassword()).trim();
        if (RegistrationRules.confirmPasswordFailure(password, confirm) != null) {
            JOptionPane.showMessageDialog(this, RegistrationRules.confirmPasswordFailure(password, confirm),
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void addRegisterField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field, int fieldMaxWidth, int insetTop) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(insetTop, 4, 2, 4);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_SECONDARY);
        int lh = label.getPreferredSize().height;
        Dimension labelSlot = new Dimension(REGISTER_LABEL_COLUMN_WIDTH, lh);
        label.setPreferredSize(labelSlot);
        label.setMinimumSize(labelSlot);
        label.setMaximumSize(labelSlot);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(insetTop, 4, 2, 4);
        if (field instanceof JTextField) {
            styleRegisterTextField((JTextField) field, fieldMaxWidth);
        }
        panel.add(field, gbc);
    }

    private void addRegistrationStatusRow(JPanel panel, GridBagConstraints gbc, int row, RegistrationStatusKind kind, int maxWrapWidth) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(1, 4, 10, 4);

        final int rowWidth = maxWrapWidth;
        final int iconSlot = 22;
        JPanel statusPanel = new JPanel(new BorderLayout(6, 0)) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(rowWidth, 22);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int h = Math.max(24, Math.min(120, d.height));
                return new Dimension(rowWidth, h);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(rowWidth, 200);
            }
        };
        statusPanel.setOpaque(false);

        JLabel iconLabel = new JLabel("❌");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(iconSlot, 18));
        iconLabel.setMinimumSize(new Dimension(iconSlot, 18));
        iconLabel.setMaximumSize(new Dimension(iconSlot, 18));

        JLabel reasonLabel = new JLabel();
        reasonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        reasonLabel.setForeground(ERROR);
        reasonLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(iconLabel, BorderLayout.NORTH);
        statusPanel.add(iconWrap, BorderLayout.WEST);
        statusPanel.add(reasonLabel, BorderLayout.CENTER);

        switch (kind) {
            case USERNAME:
                regUsernameStatusLabel = iconLabel;
                regUsernameReasonLabel = reasonLabel;
                break;
            case PASSWORD_POLICY:
                passwordRuleStatusLabel = iconLabel;
                passwordRuleReasonLabel = reasonLabel;
                break;
            case CONFIRM:
                confirmStatusLabel = iconLabel;
                confirmReasonLabel = reasonLabel;
                break;
            case EMAIL:
                regEmailStatusLabel = iconLabel;
                regEmailReasonLabel = reasonLabel;
                break;
        }

        panel.add(statusPanel, gbc);
    }

    private void styleRegisterTextField(JTextField field, int maxWidth) {
        field.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 90), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(CARD_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setCaretColor(PRIMARY);
        field.setPreferredSize(new Dimension(maxWidth, 42));
        field.setMaximumSize(new Dimension(maxWidth, 42));
        field.setMinimumSize(new Dimension(Math.min(160, maxWidth), 36));
    }

    private void styleSecondaryButton(JButton button, int height) {
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(CARD_BG);
        button.setForeground(TEXT_SECONDARY);
        button.setBorder(new javax.swing.border.LineBorder(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 100), 1, true));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, height));
    }

    private void styleInputField(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 90), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(CARD_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setCaretColor(PRIMARY);
    }

    private void stylePrimaryButton(JButton button, int height) {
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        button.setFocusPainted(false);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new javax.swing.border.LineBorder(PRIMARY, 1, true));
    }

    private void attachRegisterValidationListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshRegisterValidationStatus(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refreshRegisterValidationStatus(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshRegisterValidationStatus(); }
        };
        regUsernameField.getDocument().addDocumentListener(listener);
        regPasswordField.getDocument().addDocumentListener(listener);
        regConfirmField.getDocument().addDocumentListener(listener);
        regEmailField.getDocument().addDocumentListener(listener);
    }

    private void refreshRegisterValidationStatus() {
        if (passwordRuleStatusLabel == null || confirmStatusLabel == null
                || regUsernameStatusLabel == null || regEmailStatusLabel == null) {
            return;
        }

        String usernameRaw = regUsernameField.getText();
        String usernameTrimmed = usernameRaw.trim();
        String usernameFormatReason = RegistrationRules.usernameFormatFailure(usernameRaw);
        String usernameReason = usernameFormatReason != null
                ? usernameFormatReason
                : RegistrationRules.usernameAvailabilityFailure(usernameTrimmed, userService);
        boolean usernameOk = usernameReason == null;
        regUsernameStatusLabel.setText(usernameOk ? "✅" : "❌");
        regUsernameStatusLabel.setForeground(usernameOk ? SUCCESS : ERROR);
        regUsernameReasonLabel.setForeground(usernameOk ? SUCCESS : ERROR);
        regUsernameReasonLabel.setText(usernameOk ? "Username is available." : registrationReasonHtml(usernameReason));

        String password = new String(regPasswordField.getPassword()).trim();
        String confirm = new String(regConfirmField.getPassword()).trim();

        String passwordReason = RegistrationRules.passwordPolicyFailure(password);
        boolean passwordOk = passwordReason == null;
        passwordRuleStatusLabel.setText(passwordOk ? "✅" : "❌");
        passwordRuleStatusLabel.setForeground(passwordOk ? SUCCESS : ERROR);
        passwordRuleReasonLabel.setForeground(passwordOk ? SUCCESS : ERROR);
        passwordRuleReasonLabel.setText(passwordOk ? "Password meets all rules." : registrationReasonHtml(passwordReason));

        String confirmReason = RegistrationRules.confirmPasswordFailure(password, confirm);
        boolean confirmOk = confirmReason == null;
        confirmStatusLabel.setText(confirmOk ? "✅" : "❌");
        confirmStatusLabel.setForeground(confirmOk ? SUCCESS : ERROR);
        confirmReasonLabel.setForeground(confirmOk ? SUCCESS : ERROR);
        confirmReasonLabel.setText(confirmOk ? "Passwords match." : registrationReasonHtml(confirmReason));

        String emailRaw = regEmailField.getText();
        String emailTrimmed = emailRaw.trim();
        String emailFormatReason = RegistrationRules.emailFormatFailure(emailRaw);
        String emailReason = emailFormatReason != null
                ? emailFormatReason
                : RegistrationRules.emailAvailabilityFailure(emailTrimmed, userService);
        boolean emailOk = emailReason == null;
        regEmailStatusLabel.setText(emailOk ? "✅" : "❌");
        regEmailStatusLabel.setForeground(emailOk ? SUCCESS : ERROR);
        regEmailReasonLabel.setForeground(emailOk ? SUCCESS : ERROR);
        regEmailReasonLabel.setText(emailOk ? "Email looks good." : registrationReasonHtml(emailReason));
    }

    private String registrationReasonHtml(String plain) {
        int w = Math.max(120, registerContentMaxWidth - 28);
        String safe = plain == null ? "" : plain.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:" + w + "px'>" + safe + "</body></html>";
    }

    private void startShowcaseAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) return;
        animationTimer = new Timer(40, e -> {
            animationPhase += 0.045f;
            if (animationPhase > 1000f) animationPhase = 0f;
            if (showcasePanel != null && showcasePanel.isShowing()) {
                showcasePanel.repaint();
            }
        });
        animationTimer.start();
    }

    private void switchToRegisterTab() {
        createAccountTabBtn.setSelected(true);
        signInTabBtn.setSelected(false);
        styleTabButtons(createAccountTabBtn, signInTabBtn);
        authCardLayout.show(authCardPanel, "register");
        updateRegisterWizardUI(1);
    }

    private void switchToLoginTab() {
        signInTabBtn.setSelected(true);
        createAccountTabBtn.setSelected(false);
        styleTabButtons(signInTabBtn, createAccountTabBtn);
        authCardLayout.show(authCardPanel, "login");
        if (loginSubmitButton != null) {
            getRootPane().setDefaultButton(loginSubmitButton);
        }
    }

    private void handleRegisterInline() {
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword()).trim();
        String confirm = new String(regConfirmField.getPassword()).trim();
        String name = regNameField.getText().trim();
        String email = regEmailField.getText().trim();
        User.Role role = (User.Role) regRoleCombo.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your full name.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String uFmt = RegistrationRules.usernameFormatFailure(regUsernameField.getText());
        if (uFmt != null) {
            JOptionPane.showMessageDialog(this, uFmt, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String uTaken = RegistrationRules.usernameAvailabilityFailure(username, userService);
        if (uTaken != null) {
            JOptionPane.showMessageDialog(this, uTaken, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String eFmt = RegistrationRules.emailFormatFailure(regEmailField.getText());
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
            regUsernameField.setText("");
            regPasswordField.setText("");
            regConfirmField.setText("");
            regNameField.setText("");
            regEmailField.setText("");
            regRoleCombo.setSelectedItem(User.Role.TA);
            refreshRegisterValidationStatus();
            switchToLoginTab();
        } else {
            if (userService.isUsernameTaken(username)) {
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

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LoginAttempt attempt = loginAttempts.computeIfAbsent(username, k -> new LoginAttempt());
        long currentTime = System.currentTimeMillis();

        if (currentTime - attempt.firstFailureTime > ATTEMPT_WINDOW_MS) {
            attempt.failureCount = 0;
            attempt.firstFailureTime = 0;
            attempt.lockoutUntil = 0;
        }

        if (attempt.lockoutUntil > currentTime) {
            long remainingSeconds = (attempt.lockoutUntil - currentTime) / 1000;
            JOptionPane.showMessageDialog(this,
                    "Account locked due to too many failed login attempts.\nPlease try again in " + remainingSeconds + " seconds.",
                    "Account Locked", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userService.authenticate(username, password);
        if (user == null) {
            attempt.failureCount++;
            if (attempt.firstFailureTime == 0) {
                attempt.firstFailureTime = currentTime;
            }

            if (attempt.failureCount >= MAX_ATTEMPTS) {
                attempt.lockoutUntil = currentTime + LOCKOUT_DURATION_MS;
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.\nToo many failed attempts. Account locked for 1 minute.",
                        "Login Failed - Account Locked", JOptionPane.ERROR_MESSAGE);
            } else {
                int remainingAttempts = MAX_ATTEMPTS - attempt.failureCount;
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.\nRemaining attempts: " + remainingAttempts,
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        attempt.failureCount = 0;
        attempt.firstFailureTime = 0;
        attempt.lockoutUntil = 0;

        openDashboard(user);
    }

    public void openDashboard(User user) {
        this.setVisible(false);
        switch (user.getRole()) {
            case TA:
                new TADashboard(user, this, jobService, applicationService, notificationService).setVisible(true);
                break;
            case MO:
                new MODashboard(user, this, jobService, applicationService, notificationService).setVisible(true);
                break;
            case ADMIN:
                new AdminDashboard(user, this, jobService, applicationService, notificationService).setVisible(true);
                break;
        }
    }

    public void showAgain() {
        usernameField.setText("");
        passwordField.setText("");
        userService.reload();
        setVisible(true);
    }
}

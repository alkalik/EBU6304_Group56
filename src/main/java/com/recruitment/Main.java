package com.recruitment;

import com.formdev.flatlaf.FlatLightLaf;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.view.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        try {
            Locale.setDefault(Locale.ENGLISH);
            JComponent.setDefaultLocale(Locale.ENGLISH);
            FlatLightLaf.setup();

            // ================================================================
            // SHAPE: rounded corners (Calm UI 2026 — pillowy feel)
            // ================================================================
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("CheckBox.arc", 5);
            UIManager.put("ProgressBar.arc", 999);

            // ================================================================
            // FOCUS indicators — subtle outer glow + inner ring
            // ================================================================
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 1);
            UIManager.put("Button.innerFocusWidth", 1);

            // ================================================================
            // TYPOGRAPHY
            // ================================================================
            Font baseFont   = new Font("Segoe UI", Font.PLAIN, 14);
            Font headerFont = new Font("Segoe UI", Font.BOLD, 14);
            UIManager.put("Label.font", baseFont);
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));
            UIManager.put("TextField.font", baseFont);
            UIManager.put("TextArea.font", baseFont);
            UIManager.put("Table.font", baseFont);
            UIManager.put("TableHeader.font", headerFont);
            UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.BOLD, 14));
            UIManager.put("ComboBox.font", baseFont);
            UIManager.put("Menu.font", baseFont);
            UIManager.put("MenuItem.font", baseFont);

            // ================================================================
            // COLOR PALETTE
            // ================================================================
            // Primary accent: Soft Purple
            Color primary        = new Color(0x6C, 0x5C, 0xE7);
            // Hover = lighter/brighter for glow-up feel
            Color primaryHover   = new Color(0x7C, 0x6E, 0xF0);
            // Pressed = visibly darker for "depressed" feel
            Color primaryPressed = new Color(0x48, 0x3D, 0xB0);
            Color primaryFocus   = new Color(0x9D, 0x93, 0xF5);

            // Semantic
            Color success = new Color(0x00, 0xB8, 0x94);
            Color danger  = new Color(0xE1, 0x70, 0x55);

            // Neutrals
            Color bgContent     = new Color(0xF4, 0xF5, 0xF8);
            Color bgCard        = Color.WHITE;
            Color textPrimary   = new Color(0x2D, 0x34, 0x36);
            Color textSecondary = new Color(0x63, 0x6E, 0x72);
            Color border        = new Color(0xDF, 0xE6, 0xE9);
            Color tableAlt      = new Color(0xF8, 0xF7, 0xFF);
            Color tableSel      = new Color(0xE8, 0xE4, 0xF8);
            // Title bar: rich dark navy (not pure black — easier on eyes)
            Color darkBar       = new Color(0x2D, 0x2A, 0x4A);

            // ================================================================
            // BUTTONS — micro-interactivity: glow hover, depress pressed
            // ================================================================
            UIManager.put("Button.background", primary);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.hoverBackground", primaryHover);
            UIManager.put("Button.pressedBackground", primaryPressed);
            UIManager.put("Button.borderColor", primary);
            UIManager.put("Button.hoverBorderColor", primaryHover);
            UIManager.put("Button.focusedBorderColor", primaryFocus);
            UIManager.put("Button.default.background", primary);
            UIManager.put("Button.default.foreground", Color.WHITE);
            UIManager.put("Button.default.hoverBackground", primaryHover);
            UIManager.put("Button.default.pressedBackground", primaryPressed);
            UIManager.put("Button.default.borderColor", primary);
            UIManager.put("Button.default.focusedBorderColor", primaryFocus);

            // ================================================================
            // PANELS & TABBED PANE
            // ================================================================
            UIManager.put("Panel.background", bgContent);
            UIManager.put("TabbedPane.background", bgContent);
            UIManager.put("TabbedPane.contentAreaColor", bgCard);
            UIManager.put("TabbedPane.tabArc", 10);
            UIManager.put("TabbedPane.selectedBackground", primary);
            UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
            UIManager.put("TabbedPane.hoverColor", tableSel);
            UIManager.put("TabbedPane.tabInsets", new Insets(11, 26, 11, 26));
            UIManager.put("TabbedPane.tabHeight", 48);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
            // Slightly taller rows for better readability at full screen
            UIManager.put("Table.rowHeight", 38);
            // Larger table header for visual weight
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));

            // ================================================================
            // TEXT FIELDS
            // ================================================================
            UIManager.put("TextField.background", bgCard);
            UIManager.put("TextField.foreground", textPrimary);
            UIManager.put("TextField.borderColor", border);
            UIManager.put("TextField.focusedBorderColor", primary);
            UIManager.put("PasswordField.background", bgCard);
            UIManager.put("PasswordField.foreground", textPrimary);
            UIManager.put("PasswordField.borderColor", border);
            UIManager.put("PasswordField.focusedBorderColor", primary);
            UIManager.put("TextArea.background", bgCard);
            UIManager.put("ComboBox.background", bgCard);
            UIManager.put("ComboBox.borderColor", border);
            UIManager.put("ComboBox.focusedBorderColor", primary);
            UIManager.put("Spinner.borderColor", border);
            UIManager.put("Spinner.focusedBorderColor", primary);

            // ================================================================
            // TABLES — clean alternating rows, no horizontal grid lines
            // ================================================================
            UIManager.put("Table.background", bgCard);
            UIManager.put("Table.foreground", textPrimary);
            UIManager.put("Table.gridColor", border);
            UIManager.put("Table.selectionBackground", tableSel);
            UIManager.put("Table.selectionForeground", textPrimary);
            UIManager.put("Table.alternateRowColor", tableAlt);
            UIManager.put("Table.rowHeight", 36);
            UIManager.put("Table.showHorizontalLines", false);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
            UIManager.put("Table.cellMargins", new Insets(4, 8, 4, 8));
            UIManager.put("TableHeader.background", new Color(0xEE, 0xEC, 0xFB));
            UIManager.put("TableHeader.foreground", new Color(0x4A, 0x40, 0xA0));
            UIManager.put("TableHeader.separatorColor", border);
            UIManager.put("TableHeader.bottomSeparatorColor", primary);
            UIManager.put("TableHeader.height", 40);

            // ================================================================
            // MENUS
            // ================================================================
            UIManager.put("MenuBar.background", bgContent);
            UIManager.put("MenuBar.borderColor", border);
            UIManager.put("Menu.background", bgContent);
            UIManager.put("Menu.foreground", textPrimary);
            UIManager.put("MenuItem.background", bgCard);
            UIManager.put("MenuItem.foreground", textPrimary);
            UIManager.put("MenuItem.selectionBackground", tableSel);
            UIManager.put("MenuItem.selectionForeground", textPrimary);

            // ================================================================
            // WINDOW TITLE BAR — fix visibility: dark navy, contrasting text
            // ================================================================
            UIManager.put("TitlePane.unifiedBackground", false);
            UIManager.put("TitlePane.background", darkBar);
            // Active window: soft lavender so text is readable on dark bg
            UIManager.put("TitlePane.foreground", new Color(0xE0, 0xE0, 0xEE));
            // Inactive window: more muted
            UIManager.put("TitlePane.inactiveForeground", new Color(0x88, 0x88, 0xAA));
            UIManager.put("TitlePane.inactiveBackground", new Color(0x22, 0x20, 0x38));
            // Close/minimize/maximize button icons inherit foreground — already contrasting
            UIManager.put("TitlePane.buttonHoverBackground", new Color(0x45, 0x40, 0x70));
            UIManager.put("TitlePane.buttonPressedBackground", primary);
            // Ensure close button retains red-ish destructive color on hover
            UIManager.put("TitlePane.closeHoverBackground", new Color(0xC0, 0x39, 0x2B));
            UIManager.put("TitlePane.closePressedBackground", new Color(0x96, 0x28, 0x1F));
            UIManager.put("TitlePane.closeHoverForeground", Color.WHITE);
            UIManager.put("TitlePane.closePressedForeground", Color.WHITE);

            // ================================================================
            // SCROLL BARS — floating pill style
            // ================================================================
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.trackInsets", new Insets(4, 4, 4, 4));
            UIManager.put("ScrollBar.track", new Color(0xF0, 0xF0, 0xF8));
            UIManager.put("ScrollBar.thumb", new Color(0xC0, 0xBA, 0xE8));
            UIManager.put("ScrollBar.hoverThumbColor", primary);

            // ================================================================
            // TOOLTIPS
            // ================================================================
            UIManager.put("ToolTip.background", darkBar);
            UIManager.put("ToolTip.foreground", new Color(0xE0, 0xE0, 0xEE));
            UIManager.put("ToolTip.borderColor", primary);

            // ================================================================
            // OPTION PANE — force English button text (Times New Roman has no CJK glyphs)
            // ================================================================
            UIManager.put("OptionPane.yesButtonText", "Yes");
            UIManager.put("OptionPane.noButtonText", "No");
            UIManager.put("OptionPane.cancelButtonText", "Cancel");
            UIManager.put("OptionPane.okButtonText", "OK");

        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // fallback to default
            }
        }

        SwingUtilities.invokeLater(() -> {
            UserService userService = new UserService();
            JobService jobService = new JobService();
            ApplicationService applicationService = new ApplicationService();
            NotificationService notificationService = new NotificationService(userService);

            // Set up service dependencies to avoid circular dependency
            jobService.setApplicationService(applicationService);
            jobService.setNotificationService(notificationService);
            applicationService.setJobService(jobService);
            applicationService.setNotificationService(notificationService);

            LoginFrame loginFrame = new LoginFrame(userService, jobService, applicationService, notificationService);
            loginFrame.setVisible(true);

            // Check for expired jobs daily
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    jobService.checkExpiredJobs();
                }
            }, 0, 24 * 60 * 60 * 1000); // Check every 24 hours
        });
    }
}

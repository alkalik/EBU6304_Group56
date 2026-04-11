package com.recruitment;

import com.formdev.flatlaf.FlatLightLaf;
import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;
import com.recruitment.view.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        // Apply FlatLaf modern UI theme and rounded corner style
        try {
            FlatLightLaf.setup();
            // Modern color scheme
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 5);
            UIManager.put("ScrollBar.width", 16);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.trackArc", 8);
            UIManager.put("TabbedPane.tabArc", 8);
            UIManager.put("TabbedPane.selectedBackground", new Color(70, 130, 180));
            UIManager.put("TabbedPane.tabInsets", new Insets(6, 12, 6, 12));
            UIManager.put("Table.gridColor", new Color(220, 220, 220));
            UIManager.put("Table.selectionBackground", new Color(173, 216, 230));
            UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("TextArea.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 14));

            // Enhanced modern styling
            UIManager.put("Button.background", new Color(70, 130, 180));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.hoverBackground", new Color(100, 149, 237));
            UIManager.put("Button.pressedBackground", new Color(25, 25, 112));
            UIManager.put("Button.borderColor", new Color(70, 130, 180));
            UIManager.put("Button.hoverBorderColor", new Color(100, 149, 237));
            UIManager.put("Button.focusedBorderColor", new Color(100, 149, 237));

            // Panel and background colors
            UIManager.put("Panel.background", new Color(248, 249, 250));
            UIManager.put("TabbedPane.background", new Color(248, 249, 250));
            UIManager.put("TabbedPane.contentAreaColor", new Color(255, 255, 255));

            // Text field styling
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.borderColor", new Color(200, 200, 200));
            UIManager.put("TextField.focusedBorderColor", new Color(70, 130, 180));
            UIManager.put("PasswordField.background", Color.WHITE);
            UIManager.put("PasswordField.borderColor", new Color(200, 200, 200));
            UIManager.put("PasswordField.focusedBorderColor", new Color(70, 130, 180));

            // Table styling
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.alternateRowColor", new Color(248, 249, 250));
            UIManager.put("TableHeader.background", new Color(240, 240, 240));
            UIManager.put("TableHeader.foreground", new Color(70, 70, 70));

            // Menu styling
            UIManager.put("MenuBar.background", new Color(248, 249, 250));
            UIManager.put("Menu.background", new Color(248, 249, 250));
            UIManager.put("MenuItem.background", new Color(248, 249, 250));
            UIManager.put("MenuItem.selectionBackground", new Color(173, 216, 230));

            // Window styling
            UIManager.put("TitlePane.background", new Color(70, 130, 180));
            UIManager.put("TitlePane.foreground", Color.WHITE);
            UIManager.put("TitlePane.buttonHoverBackground", new Color(100, 149, 237));
            UIManager.put("TitlePane.buttonPressedBackground", new Color(25, 25, 112));

            // Force English button labels
            UIManager.put("OptionPane.okButtonText", "OK");
            UIManager.put("OptionPane.cancelButtonText", "Cancel");
            UIManager.put("OptionPane.yesButtonText", "Yes");
            UIManager.put("OptionPane.noButtonText", "No");
            UIManager.put("OptionPane.buttonFont", new Font("SansSerif", Font.PLAIN, 14));

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

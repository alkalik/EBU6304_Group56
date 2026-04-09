package com.recruitment;

import com.formdev.flatlaf.FlatLightLaf;
import com.recruitment.service.JobService;
import com.recruitment.service.UserService;
import com.recruitment.view.LoginFrame;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        // Apply FlatLaf modern UI theme and rounded corner style
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("TabbedPane.tabHeight", 36);
            UIManager.put("TabbedPane.selectedBackground", UIManager.getColor("Panel.background"));
            UIManager.put("Table.showHorizontalLines", false);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("Focus.width", 1);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // fallback to default
            }
        }

        SwingUtilities.invokeLater(() -> {
            UserService userService = new UserService();
            LoginFrame loginFrame = new LoginFrame(userService);
            loginFrame.setVisible(true);

            // Check for expired jobs daily
            JobService jobService = new JobService();
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

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
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 5);
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

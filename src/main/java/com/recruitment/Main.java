package com.recruitment;

import com.formdev.flatlaf.FlatLightLaf;
import com.recruitment.service.UserService;
import com.recruitment.view.LoginFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set modern look and feel
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
        });
    }
}

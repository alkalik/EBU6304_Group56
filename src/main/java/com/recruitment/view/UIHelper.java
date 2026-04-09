package com.recruitment.view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class UIHelper {
    private static final Color PRIMARY = new Color(46, 94, 170);
    private static final Color PANEL = new Color(248, 250, 252);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color BORDER = new Color(220, 226, 235);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private UIHelper() {
    }

    public static JPanel createPagePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        panel.setBackground(Color.WHITE);
        return panel;
    }

    public static JPanel wrapInCard(JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(PANEL);
        panel.setBorder(createCardBorder());
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public static JPanel createHeaderPanel(String title, String subtitle) {
        JPanel headerPanel = new JPanel(new BorderLayout(8, 6));
        headerPanel.setBackground(PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(HEADER_TEXT);
        titleLabel.setFont(TITLE_FONT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(230, 236, 245));
        subtitleLabel.setFont(SUBTITLE_FONT);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        return headerPanel;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(221, 235, 255));
        table.setSelectionForeground(new Color(33, 37, 41));
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setBackground(new Color(236, 241, 248));
        header.setForeground(new Color(49, 58, 74));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
    }

    public static void stylePrimaryButton(AbstractButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY.darker()),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
    }

    public static void styleTextComponent(JComponent component) {
        component.setPreferredSize(new Dimension(component.getPreferredSize().width, 32));
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    public static JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        return scrollPane;
    }

    public static JPanel createSectionHeader(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(96, 103, 112));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(subtitleLabel, BorderLayout.CENTER);
        return panel;
    }
}

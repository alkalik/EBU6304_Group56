package com.recruitment.util;

import java.awt.Color;

/**
 * Builds HTML snippets for Swing labels and buttons that mix Segoe UI text with
 * symbol or emoji glyphs.
 * <p>
 * On Windows, plain {@code JLabel} text often fails to render Unicode icons;
 * wrapping symbols in {@code Segoe UI Symbol} or {@code Segoe UI Emoji} font spans
 * avoids empty placeholder boxes.
 */
public final class UiText {

    private static final String SYMBOL_FONT = "Segoe UI Symbol";
    private static final String EMOJI_FONT  = "Segoe UI Emoji";
    private static final String TEXT_FONT   = "Segoe UI";

    /** Prevents instantiation. */
    private UiText() {}

    /**
     * Builds HTML for a symbol (or emoji) optionally followed by a text label.
     *
     * @param symbol glyph or emoji character(s) shown in the icon font
     * @param label  trailing label text; may be empty
     * @param emoji  {@code true} to use {@code Segoe UI Emoji}; {@code false} for {@code Segoe UI Symbol}
     * @return HTML string suitable for {@code JLabel.setText} or {@code JButton.setText}
     */
    public static String symbolText(String symbol, String label, boolean emoji) {
        return symbolText(symbol, label, emoji, null);
    }

    /**
     * Builds coloured HTML for a symbol (or emoji) optionally followed by a text label.
     *
     * @param symbol    glyph or emoji character(s) shown in the icon font
     * @param label     trailing label text; may be empty
     * @param emoji     {@code true} to use emoji font; {@code false} for symbol font
     * @param textColor optional foreground colour applied to both spans; {@code null} for default
     * @return HTML string suitable for Swing HTML-capable components
     */
    public static String symbolText(String symbol, String label, boolean emoji, Color textColor) {
        String symFont = emoji ? EMOJI_FONT : SYMBOL_FONT;
        String color = colorStyle(textColor);
        StringBuilder html = new StringBuilder("<html><span style='font-family:")
                .append(symFont).append(";font-size:13px").append(color).append("'>")
                .append(symbol).append("</span>");
        if (label != null && !label.isEmpty()) {
            html.append("&nbsp;<span style='font-family:").append(TEXT_FONT)
                .append(";font-size:13px").append(color).append("'>")
                .append(label).append("</span>");
        }
        return html.append("</html>").toString();
    }

    /**
     * Returns an inline CSS colour declaration for HTML spans, or empty if no colour.
     */
    private static String colorStyle(Color c) {
        if (c == null) return "";
        return ";color:#" + String.format("%06X", c.getRGB() & 0xFFFFFF);
    }
}

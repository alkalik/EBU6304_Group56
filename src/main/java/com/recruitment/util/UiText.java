package com.recruitment.util;

import java.awt.Color;

/**
 * Builds HTML labels/button text that mix Segoe UI with symbol/emoji fonts,
 * so Unicode icons render correctly on Windows without empty boxes.
 */
public final class UiText {

    private static final String SYMBOL_FONT = "Segoe UI Symbol";
    private static final String EMOJI_FONT  = "Segoe UI Emoji";
    private static final String TEXT_FONT   = "Segoe UI";

    private UiText() {}

    public static String symbolText(String symbol, String label, boolean emoji) {
        return symbolText(symbol, label, emoji, null);
    }

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

    private static String colorStyle(Color c) {
        if (c == null) return "";
        return ";color:#" + String.format("%06X", c.getRGB() & 0xFFFFFF);
    }
}

package com.recruitment.util;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A custom Border that paints a multi-layer soft drop shadow beneath a rounded card panel,
 * producing a Material Design / 2026 Spatial UI "elevation" effect without any
 * third-party library dependency.
 *
 * Usage:
 *   panel.setOpaque(false);
 *   panel.setBorder(new ShadowBorder(12, 4));
 *
 * The component must have setOpaque(false) so the shadow painted in the insets area shows through.
 */
public class ShadowBorder implements Border {

    private final int cornerRadius;
    private final int shadowSize;
    private final int shadowOffset;
    private final Color shadowColor;
    private final Color cardBackground;

    /**
     * @param cornerRadius  arc radius for the rounded rectangle card
     * @param shadowSize    number of shadow gradient layers (controls softness, typically 6-12)
     */
    public ShadowBorder(int cornerRadius, int shadowSize) {
        this(cornerRadius, shadowSize, 2, new Color(0x6C5CE7), Color.WHITE);
    }

    public ShadowBorder(int cornerRadius, int shadowSize, int shadowOffset,
                        Color shadowColor, Color cardBackground) {
        this.cornerRadius  = cornerRadius;
        this.shadowSize    = shadowSize;
        this.shadowOffset  = shadowOffset;
        this.shadowColor   = shadowColor;
        this.cardBackground = cardBackground;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int total = shadowSize + shadowOffset;
        return new Insets(shadowSize, shadowSize, total + 2, shadowSize + 2);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cardX      = x + shadowSize;
        int cardY      = y + shadowSize;
        int cardWidth  = width  - shadowSize * 2 - (shadowOffset + 2);
        int cardHeight = height - shadowSize * 2 - (shadowOffset + 2);

        // Paint concentric shadow layers from outermost (most transparent) to innermost
        for (int i = shadowSize; i >= 1; i--) {
            float alpha = 0.04f * (shadowSize - i + 1);
            g2.setColor(new Color(
                    shadowColor.getRed()   / 255f,
                    shadowColor.getGreen() / 255f,
                    shadowColor.getBlue()  / 255f,
                    Math.min(alpha, 1.0f)
            ));
            int inset = shadowSize - i;
            g2.fillRoundRect(
                    cardX - inset + shadowOffset,
                    cardY - inset + shadowOffset,
                    cardWidth  + inset * 2,
                    cardHeight + inset * 2,
                    cornerRadius + inset,
                    cornerRadius + inset
            );
        }

        // Paint the card background itself (white rounded rect)
        g2.setColor(cardBackground);
        g2.fillRoundRect(cardX, cardY, cardWidth, cardHeight, cornerRadius, cornerRadius);

        g2.dispose();
    }

    /**
     * Convenience factory: standard card shadow matching the app's primary purple theme.
     */
    public static ShadowBorder card() {
        return new ShadowBorder(14, 10, 3, new Color(0x6C, 0x5C, 0xE7), Color.WHITE);
    }

    /**
     * Convenience factory: lighter shadow for secondary / inline cards.
     */
    public static ShadowBorder subtle() {
        return new ShadowBorder(10, 6, 2, new Color(0x6C, 0x5C, 0xE7), Color.WHITE);
    }
}

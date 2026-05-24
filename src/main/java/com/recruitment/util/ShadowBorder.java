package com.recruitment.util;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom {@link Border} that paints a multi-layer soft drop shadow beneath a rounded card panel,
 * producing a Material Design / spatial UI elevation effect without third-party dependencies.
 *
 * <p>The host component must call {@code setOpaque(false)} so the shadow painted in the
 * border insets is visible around the card.
 *
 * <p>Usage example:
 * <pre>{@code
 * panel.setOpaque(false);
 * panel.setBorder(new ShadowBorder(12, 4));
 * }</pre>
 *
 * @see #card()
 * @see #subtle()
 */
public class ShadowBorder implements Border {

    /** Corner arc radius of the card rectangle in pixels. */
    private final int cornerRadius;
    /** Number of gradient shadow layers (controls softness; typically 6–12). */
    private final int shadowSize;
    /** Vertical offset applied to shadow layers in pixels. */
    private final int shadowOffset;
    /** Base tint colour for shadow layers. */
    private final Color shadowColor;
    /** Fill colour of the card surface. */
    private final Color cardBackground;

    /**
     * Creates a shadow border with default offset, shadow tint, and white card background.
     *
     * @param cornerRadius arc radius for the rounded rectangle card
     * @param shadowSize   number of shadow gradient layers (controls softness, typically 6–12)
     */
    public ShadowBorder(int cornerRadius, int shadowSize) {
        this(cornerRadius, shadowSize, 2, new Color(0x88, 0x88, 0xAA), Color.WHITE);
    }

    /**
     * Creates a fully customised shadow border.
     *
     * @param cornerRadius   arc radius for the rounded card
     * @param shadowSize     number of shadow gradient layers
     * @param shadowOffset   vertical offset of the shadow stack
     * @param shadowColor    base colour for shadow layers
     * @param cardBackground fill colour for the card interior
     */
    public ShadowBorder(int cornerRadius, int shadowSize, int shadowOffset,
                        Color shadowColor, Color cardBackground) {
        this.cornerRadius  = cornerRadius;
        this.shadowSize    = shadowSize;
        this.shadowOffset  = shadowOffset;
        this.shadowColor   = shadowColor;
        this.cardBackground = cardBackground;
    }

    /**
     * {@inheritDoc}
     * <p>Insets reserve space for the shadow on all sides; bottom inset is slightly larger
     * to accommodate vertical shadow offset.
     */
    @Override
    public Insets getBorderInsets(Component c) {
        int total = shadowSize + shadowOffset;
        return new Insets(shadowSize, shadowSize, total + 2, shadowSize + 2);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false} so the component background may show through the shadow area
     */
    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>Paints concentric semi-transparent shadow layers, then the rounded card fill.
     */
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
     * Standard card shadow matching the application's primary purple theme.
     *
     * @return preconfigured {@link ShadowBorder} for primary content cards
     */
    public static ShadowBorder card() {
        return new ShadowBorder(12, 8, 2, new Color(0x88, 0x88, 0xAA), Color.WHITE);
    }

    /**
     * Lighter shadow preset for secondary or inline cards.
     *
     * @return preconfigured {@link ShadowBorder} with reduced shadow depth
     */
    public static ShadowBorder subtle() {
        return new ShadowBorder(10, 5, 1, new Color(0x88, 0x88, 0xAA), Color.WHITE);
    }
}

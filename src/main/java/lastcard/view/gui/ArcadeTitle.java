package lastcard.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import javax.swing.JComponent;

/**
 * Un titolo a pixel: crema con righe di scansione e ombra rossa sfalsata, una riga per ogni stringa.
 * Si usa per "ULTIMA CARTA" nel menu e, più piccolo, per i titoli delle altre schermate.
 */
public class ArcadeTitle extends JComponent {

    static final Color FILL = ArcadeButtonUI.CREAM;
    static final Color SHADOW = ArcadeButtonUI.SHADOW;

    private final String[] lines;
    private final float size;
    private final int shadow;
    private final int lineGap;

    /** @param size corpo del font in pixel: 52 per il menu, circa 22 per i titoli delle schermate */
    public ArcadeTitle(float size, String... lines) {
        this.lines = Objects.requireNonNull(lines, "lines").clone();
        this.size = size;
        this.shadow = Math.max(2, Math.round(size / 9f));
        this.lineGap = Math.round(size * 0.2f);
        setOpaque(false);
    }

    private Font font() {
        return ArcadeFont.of(size);
    }

    @Override
    public Dimension getPreferredSize() {
        Font f = font();
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, getFontMetrics(f).stringWidth(line));
        }
        int lineH = Math.round(size);
        return new Dimension(w + shadow, lines.length * lineH + (lines.length - 1) * lineGap + shadow);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font f = font();
            int lineH = Math.round(size);
            for (int i = 0; i < lines.length; i++) {
                String s = lines[i];
                TextLayout tl = new TextLayout(s, f, g.getFontRenderContext());
                double x = (getWidth() - shadow - tl.getAdvance()) / 2.0;
                double y = i * (lineH + lineGap) + tl.getAscent();
                Shape back = tl.getOutline(AffineTransform.getTranslateInstance(x + shadow, y + shadow));
                Shape front = tl.getOutline(AffineTransform.getTranslateInstance(x, y));
                g.setColor(SHADOW);
                g.fill(back);
                g.setColor(FILL);
                g.fill(front);
                // righe di scansione solo dentro le lettere
                Shape clip = g.getClip();
                g.clip(front);
                g.setColor(new Color(0f, 0f, 0f, 0.22f));
                Rectangle2D r = front.getBounds2D();
                for (double yy = r.getY(); yy < r.getMaxY(); yy += 4) {
                    g.fill(new Rectangle2D.Double(r.getX(), yy, r.getWidth(), 2));
                }
                g.setClip(clip);
            }
        } finally {
            g.dispose();
        }
    }
}

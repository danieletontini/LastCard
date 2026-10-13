package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Le caselle da spuntare nello stile cabinato: il testo a pixel a sinistra, a destra un riquadro scuro
 * che, spuntato, mostra un segno di spunta a pixel con l'ombra rossa, come i titoli.
 *
 * Gli stati sono quelli dei pulsanti: col mouse sopra il riquadro e il testo si accendono, il focus da
 * tastiera mostra la freccetta, disattivata la casella si spegne.
 * Il riquadro sta sul bordo destro: in una colonna di caselle larghe uguali, i riquadri restano allineati.
 */
public class ArcadeCheckBoxUI extends BasicButtonUI {

    private static final float FONT_SIZE = 12f;
    private static final int BOX = 24;
    private static final int TEXT_GAP = 24;
    private static final int HEIGHT = 36;
    private static final int CHECK_WIDTH = 14;

    /** Il segno di spunta, 7x6. */
    private static final int[][] CHECK = {
        { 0,0,0,0,0,0,1 },
        { 0,0,0,0,0,1,1 },
        { 1,0,0,0,1,1,0 },
        { 1,1,0,1,1,0,0 },
        { 0,1,1,1,0,0,0 },
        { 0,0,1,0,0,0,0 }
    };

    private static final ArcadeCheckBoxUI INSTANCE = new ArcadeCheckBoxUI();

    /** Lo stesso oggetto va bene per tutte le caselle: non tiene stato. */
    public static ArcadeCheckBoxUI instance() {
        return INSTANCE;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setFont(ArcadeFont.of(FONT_SIZE));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        FontMetrics fm = b.getFontMetrics(b.getFont());
        int arrow = ArcadeButtonUI.FOCUS_ARROW_WIDTH + ArcadeButtonUI.FOCUS_ARROW_GAP;
        return new Dimension(arrow + fm.stringWidth(label(b)) + TEXT_GAP + BOX + 2, HEIGHT);
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean enabled = m.isEnabled();
            boolean hot = enabled && m.isRollover();
            boolean focused = FocusIndicator.shows(b);
            int w = c.getWidth();
            double cy = c.getHeight() / 2.0;

            Color text = !enabled ? ArcadeButtonUI.TEXT_DISABLED
                    : hot ? ArcadeButtonUI.CREAM_HOVER : ArcadeButtonUI.CREAM;

            // il testo, dopo lo spazio della freccetta del focus
            int textX = ArcadeButtonUI.FOCUS_ARROW_WIDTH + ArcadeButtonUI.FOCUS_ARROW_GAP;
            g.setFont(b.getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(text);
            g.drawString(label(b), textX, (int) (cy - fm.getHeight() / 2.0 + fm.getAscent()));
            if (focused) {
                ArcadeButtonUI.paintFocusArrow(g, 0, cy, text);
            }

            // il riquadro, sul bordo destro
            double bx = w - BOX - 1;
            double by = cy - BOX / 2.0;
            g.setColor(hot ? ArcadeButtonUI.BODY_HOVER : ArcadeButtonUI.BODY);
            g.fill(new Rectangle2D.Double(bx, by, BOX, BOX));
            g.setStroke(new BasicStroke(2f));
            g.setColor(!enabled ? ArcadeButtonUI.EDGE : hot ? ArcadeButtonUI.CREAM : ArcadeButtonUI.EDGE);
            g.draw(new Rectangle2D.Double(bx + 1, by + 1, BOX - 2, BOX - 2));

            if (m.isSelected()) {
                double checkHeight = PixelGlyphs.heightFor(CHECK, CHECK_WIDTH);
                double x = bx + (BOX - CHECK_WIDTH) / 2.0;
                double y = by + (BOX - checkHeight) / 2.0;
                if (enabled) {
                    PixelGlyphs.draw(g, CHECK, x + 2, y + 2, CHECK_WIDTH, ArcadeButtonUI.SHADOW);
                }
                PixelGlyphs.draw(g, CHECK, x, y, CHECK_WIDTH, enabled ? text : ArcadeButtonUI.TEXT_DISABLED);
            }
        } finally {
            g.dispose();
        }
    }

    private static String label(AbstractButton b) {
        String t = b.getText();
        return t == null ? "" : t.toUpperCase();
    }
}

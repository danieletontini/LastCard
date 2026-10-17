package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * Il riquadro scuro con il bordo sottile che sta sopra lo sfondo del cabinato e tiene leggibile il contenuto:
 * il testo delle Regole, le scelte di Nuova partita. Gli angoli e il bordo crescono con la scala.
 */
final class ArcadeBox extends JPanel {

    private static final Color FILL = new Color(0x15, 0x16, 0x1B, 235);

    private float scale;

    ArcadeBox(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        this.scale = 1f;
    }

    void setScale(float scale) {
        this.scale = scale;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double arc = 12 * scale;
            g.setColor(FILL);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            g.setStroke(new BasicStroke(2f * scale));
            g.setColor(ArcadeButtonUI.EDGE);
            g.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, arc, arc));
        } finally {
            g.dispose();
        }
    }
}

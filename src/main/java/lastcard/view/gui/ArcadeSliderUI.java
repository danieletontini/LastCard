package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Lo slider orizzontale nello stile cabinato: una barra scura con il bordo, riempita di crema fino al cursore,
 * e un cursore crema con l'ombra rossa, come i pulsanti PRIMARY. Disattivato (con il muto), si spegne.
 */
public class ArcadeSliderUI extends BasicSliderUI {

    private static final int TRACK_HEIGHT = 10;
    private static final int THUMB_WIDTH = 14;
    private static final int THUMB_HEIGHT = 24;
    private static final int THUMB_SHADOW = 3;

    public ArcadeSliderUI(JSlider slider) {
        super(slider);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(THUMB_WIDTH + THUMB_SHADOW, THUMB_HEIGHT + THUMB_SHADOW);
    }

    @Override
    public void paintTrack(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Rectangle track = trackRect;
            double y = track.getCenterY() - TRACK_HEIGHT / 2.0;
            Rectangle2D.Double bar = new Rectangle2D.Double(track.x, y, track.width, TRACK_HEIGHT);
            g.setColor(ArcadeButtonUI.BODY);
            g.fill(bar);

            // la parte piena arriva fino al centro del cursore
            double filled = thumbRect.getCenterX() - THUMB_SHADOW / 2.0 - track.x;
            g.setColor(slider.isEnabled() ? ArcadeButtonUI.CREAM : ArcadeButtonUI.EDGE);
            g.fill(new Rectangle2D.Double(track.x, y, Math.max(0, filled), TRACK_HEIGHT));

            g.setStroke(new BasicStroke(2f));
            g.setColor(ArcadeButtonUI.EDGE);
            g.draw(new Rectangle2D.Double(track.x + 1, y + 1, track.width - 2, TRACK_HEIGHT - 2));
        } finally {
            g.dispose();
        }
    }

    @Override
    public void paintThumb(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            double x = thumbRect.x;
            double y = thumbRect.y;
            if (slider.isEnabled()) {
                g.setColor(ArcadeButtonUI.SHADOW);
                g.fill(new Rectangle2D.Double(x + THUMB_SHADOW, y + THUMB_SHADOW, THUMB_WIDTH, THUMB_HEIGHT));
            }
            g.setColor(slider.isEnabled() ? ArcadeButtonUI.CREAM : ArcadeButtonUI.EDGE);
            g.fill(new Rectangle2D.Double(x, y, THUMB_WIDTH, THUMB_HEIGHT));
        } finally {
            g.dispose();
        }
    }

    // Niente rettangolo tratteggiato del focus di Swing: stonerebbe con lo stile.
    @Override
    public void paintFocus(Graphics g) {
    }
}

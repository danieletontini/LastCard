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

    // Le misure a grandezza normale, moltiplicate per la scala.
    private static final int WIDTH = 320;
    private static final int TRACK_HEIGHT = 12;
    private static final int THUMB_WIDTH = 16;
    private static final int THUMB_HEIGHT = 28;
    private static final int THUMB_SHADOW = 3;

    private final int width;
    private final int trackHeight;
    private final int thumbWidth;
    private final int thumbHeight;

    public ArcadeSliderUI(JSlider slider) {
        this(slider, 1f);
    }

    /** Uno slider più grande: per cambiare scala si installa una nuova ArcadeSliderUI con la scala voluta. */
    public ArcadeSliderUI(JSlider slider, float scale) {
        super(slider);
        this.width = Math.round(WIDTH * scale);
        this.trackHeight = Math.round(TRACK_HEIGHT * scale);
        this.thumbWidth = Math.round(THUMB_WIDTH * scale);
        this.thumbHeight = Math.round(THUMB_HEIGHT * scale);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(thumbWidth + THUMB_SHADOW, thumbHeight + THUMB_SHADOW);
    }

    @Override
    public Dimension getPreferredHorizontalSize() {
        return new Dimension(width, thumbHeight + THUMB_SHADOW + 4);
    }

    @Override
    public void paintTrack(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Rectangle track = trackRect;
            double y = track.getCenterY() - trackHeight / 2.0;
            Rectangle2D.Double bar = new Rectangle2D.Double(track.x, y, track.width, trackHeight);
            g.setColor(ArcadeButtonUI.BODY);
            g.fill(bar);

            // la parte piena arriva fino al centro del cursore
            double filled = thumbRect.getCenterX() - THUMB_SHADOW / 2.0 - track.x;
            g.setColor(slider.isEnabled() ? ArcadeButtonUI.CREAM : ArcadeButtonUI.EDGE);
            g.fill(new Rectangle2D.Double(track.x, y, Math.max(0, filled), trackHeight));

            g.setStroke(new BasicStroke(2f));
            g.setColor(ArcadeButtonUI.EDGE);
            g.draw(new Rectangle2D.Double(track.x + 1, y + 1, track.width - 2, trackHeight - 2));
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
                g.fill(new Rectangle2D.Double(x + THUMB_SHADOW, y + THUMB_SHADOW, thumbWidth, thumbHeight));
            }
            g.setColor(slider.isEnabled() ? ArcadeButtonUI.CREAM : ArcadeButtonUI.EDGE);
            g.fill(new Rectangle2D.Double(x, y, thumbWidth, thumbHeight));
        } finally {
            g.dispose();
        }
    }

    // Niente rettangolo tratteggiato del focus di Swing: stonerebbe con lo stile.
    @Override
    public void paintFocus(Graphics g) {
    }
}

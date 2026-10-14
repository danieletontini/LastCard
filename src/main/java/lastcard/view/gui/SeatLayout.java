package lastcard.view.gui;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Dove si siedono gli avversari attorno al tavolo: solo geometria, senza disegno, così si può verificare nei test.
 *
 * Il giocatore umano è in basso al centro; gli avversari stanno su un arco di ellisse che parte in basso a sinistra,
 * passa in alto e finisce in basso a destra, nell'ordine di gioco in senso orario. Con un avversario solo è
 * in alto al centro; più sono, più l'arco si apre verso il basso, fino a un massimo oltre il quale si stringono.
 */
final class SeatLayout {

    /** Il centro dell'ellisse, in proporzione all'altezza: un po' sopra la metà, perché sotto c'è la mano. */
    private static final double CENTER_Y = 0.46;
    /** Dove l'umano "si siede": il punto del percorso sopra la sua mano. */
    private static final double VIEWER_Y = 0.73;
    private static final double MAX_HALF_SPAN = 140;
    private static final double FIRST_HALF_SPAN = 30;
    private static final double HALF_SPAN_STEP = 25;

    private SeatLayout() {
    }

    /**
     * I centri dei riquadri degli avversari, nell'ordine di gioco a partire da chi gioca dopo l'umano.
     * I riquadri, grandi boxWidth x boxHeight, restano dentro i margini.
     */
    static List<Point2D.Double> opponents(int count, double width, double height,
            double boxWidth, double boxHeight, double margin) {
        List<Point2D.Double> seats = new ArrayList<>();
        if (count <= 0) {
            return seats;
        }
        double cx = width / 2;
        double cy = height * CENTER_Y;
        double rx = cx - margin - boxWidth / 2;
        double ry = cy - margin - boxHeight / 2;
        double halfSpan = count == 1 ? 0 : Math.min(MAX_HALF_SPAN, FIRST_HALF_SPAN + HALF_SPAN_STEP * (count - 1));

        for (int i = 0; i < count; i++) {
            // dall'angolo più a sinistra (90 + halfSpan) a quello più a destra (90 - halfSpan)
            double degrees = count == 1 ? 90 : 90 + halfSpan - i * (2 * halfSpan / (count - 1));
            double angle = Math.toRadians(degrees);
            double x = cx + rx * Math.cos(angle);
            double y = cy - ry * Math.sin(angle);
            x = clamp(x, margin + boxWidth / 2, width - margin - boxWidth / 2);
            y = clamp(y, margin + boxHeight / 2, height - margin - boxHeight / 2);
            seats.add(new Point2D.Double(x, y));
        }
        return seats;
    }

    /** Il punto del giocatore umano, da cui parte e a cui torna il percorso del giro. */
    static Point2D.Double viewer(double width, double height) {
        return new Point2D.Double(width / 2, height * VIEWER_Y);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

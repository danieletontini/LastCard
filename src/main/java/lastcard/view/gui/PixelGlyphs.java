package lastcard.view.gui;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * I simboli disegnati a blocchi, come sprite di un gioco anni Ottanta.
 *
 * <p>Ogni matrix e una griglia di 0 e 1: 1 significa blocco pieno. Disegnarli
 * così invece che con curve ha tre vantaggi: scalano a qualunque dimensione
 * senza sfocarsi, non richiedono font esterni, e si modificano cambiando una
 * riga di numeri.</p>
 */
public final class PixelGlyphs {

    private PixelGlyphs() {
    }

    /** Doppia freccia opposta: inverti il senso di marcia. Griglia 17x8. */
    public static final int[][] REVERSE = {
        { 0,0,0,1,0,0,0, 0,0,0, 0,0,0,1,0,0,0 },
        { 0,0,1,1,1,0,0, 0,0,0, 0,0,0,1,0,0,0 },
        { 0,1,1,1,1,1,0, 0,0,0, 0,0,0,1,0,0,0 },
        { 1,1,1,1,1,1,1, 0,0,0, 0,0,0,1,0,0,0 },
        { 0,0,0,1,0,0,0, 0,0,0, 1,1,1,1,1,1,1 },
        { 0,0,0,1,0,0,0, 0,0,0, 0,1,1,1,1,1,0 },
        { 0,0,0,1,0,0,0, 0,0,0, 0,0,1,1,1,0,0 },
        { 0,0,0,1,0,0,0, 0,0,0, 0,0,0,1,0,0,0 }
    };

    /** Cerchio sbarrato: salta turno. Griglia 13x13. */
    public static final int[][] SKIP = {
        { 0,0,0,0,0,1,1,1,0,0,0,0,0 },
        { 0,0,0,1,1,0,0,0,1,1,0,0,0 },
        { 0,0,1,0,0,0,0,0,0,1,1,0,0 },
        { 0,1,0,0,0,0,0,0,1,1,0,1,0 },
        { 0,1,0,0,0,0,0,1,1,0,0,1,0 },
        { 1,0,0,0,0,0,1,1,0,0,0,0,1 },
        { 1,0,0,0,0,1,1,0,0,0,0,0,1 },
        { 1,0,0,0,1,1,0,0,0,0,0,0,1 },
        { 0,1,0,1,1,0,0,0,0,0,0,1,0 },
        { 0,1,1,1,0,0,0,0,0,0,0,1,0 },
        { 0,0,1,0,0,0,0,0,0,0,1,0,0 },
        { 0,0,0,1,1,0,0,0,1,1,0,0,0 },
        { 0,0,0,0,0,1,1,1,0,0,0,0,0 }
    };


    /**
     * Disegna una matrix con l'angolo in alto a sinistra in (x, y).
     *
     * <p>I blocchi vengono allineati al pixel intero così da combaciare senza
     * fessure, qualunque sia la scala.</p>
     *
     * @param width larghezza totale occupata dal glifo
     */
    public static void draw(Graphics2D g, int[][] matrix, double x, double y,
                               double width, Color color) {
        int cols = matrix[0].length;
        int rows = matrix.length;
        double side = width / cols;

        g.setColor(color);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (matrix[r][c] == 0) {
                    continue;
                }
                int x0 = (int) Math.round(x + c * side);
                int x1 = (int) Math.round(x + (c + 1) * side);
                int y0 = (int) Math.round(y + r * side);
                int y1 = (int) Math.round(y + (r + 1) * side);
                g.fillRect(x0, y0, x1 - x0, y1 - y0);
            }
        }
    }

    /** Disegna una matrix centrata in (cx, cy). */
    public static void drawCentered(Graphics2D g, int[][] matrix, double cx, double cy,
                                       double width, Color color) {
        double side = width / matrix[0].length;
        double height = side * matrix.length;
        draw(g, matrix, cx - width / 2, cy - height / 2, width, color);
    }

    /** Altezza che il glifo occupa a una data width. */
    public static double heightFor(int[][] matrix, double width) {
        return width / matrix[0].length * matrix.length;
    }
}

package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.util.List;

import org.junit.jupiter.api.Test;

/** I posti degli avversari al tavolo: dentro lo schermo, nell'ordine del giro, uno solo in alto al centro. */
class SeatLayoutTest {

    private static final double W = 960;
    private static final double H = 640;
    private static final double BOX_W = 132;
    private static final double BOX_H = 54;
    private static final double MARGIN = 18;

    private static List<Point2D.Double> seats(int count) {
        return SeatLayout.opponents(count, W, H, BOX_W, BOX_H, MARGIN);
    }

    @Test
    void aSingleOpponentSitsAtTheTopCenter() {
        Point2D.Double seat = seats(1).get(0);

        assertEquals(W / 2, seat.x, 0.5);
        assertEquals(MARGIN + BOX_H / 2, seat.y, 0.5);
    }

    @Test
    void everySeatStaysInsideTheScreen() {
        for (int count = 1; count <= 9; count++) {
            for (Point2D.Double seat : seats(count)) {
                assertTrue(seat.x - BOX_W / 2 >= MARGIN - 0.5 && seat.x + BOX_W / 2 <= W - MARGIN + 0.5,
                        count + " avversari: x=" + seat.x);
                assertTrue(seat.y - BOX_H / 2 >= MARGIN - 0.5 && seat.y + BOX_H / 2 <= H - MARGIN + 0.5,
                        count + " avversari: y=" + seat.y);
            }
        }
    }

    @Test
    void seatsGoFromLeftOverTheTopToTheRight() {
        for (int count = 2; count <= 9; count++) {
            List<Point2D.Double> seats = seats(count);
            assertTrue(seats.get(0).x < W / 2, "il primo dopo di te è a sinistra");
            assertTrue(seats.get(count - 1).x > W / 2, "l'ultimo prima di te è a destra");
            // simmetrici rispetto al centro: il primo e l'ultimo alla stessa altezza
            assertEquals(seats.get(0).y, seats.get(count - 1).y, 0.5);
            assertEquals(W, seats.get(0).x + seats.get(count - 1).x, 0.5);
        }
    }

    @Test
    void neighboursNeverOverlap() {
        for (int count = 2; count <= 9; count++) {
            List<Point2D.Double> seats = seats(count);
            for (int i = 0; i + 1 < seats.size(); i++) {
                Point2D.Double a = seats.get(i);
                Point2D.Double b = seats.get(i + 1);
                boolean apart = Math.abs(a.x - b.x) >= BOX_W || Math.abs(a.y - b.y) >= BOX_H;
                assertTrue(apart, count + " avversari: si sovrappongono i posti " + i + " e " + (i + 1));
            }
        }
    }
}

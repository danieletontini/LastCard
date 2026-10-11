package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;

import lastcard.model.CardColor;
import lastcard.model.CardFace;
import lastcard.model.FaceSymbol;

/**
 * Disegna una carta su un {@link Graphics2D}, nello stile cabinato.
 *
 * <p>Nessuna dimensione è fissa: tutto deriva dalla larghezza passata, così la
 * stessa classe serve la mano del giocatore, la pila degli scarti e le
 * miniature degli avversari.</p>
 *
 * <p>La mappa {@code glyphs} associa ogni simbolo al modo di disegnarlo:
 * aggiungere una carta nuova significa registrare una voce, non toccare uno
 * {@code switch}. Negli angoli si scrive l'etichetta della carta; i simboli senza
 * etichetta (inverti, salta, jolly) registrano il loro disegno in {@code cornerGlyphs}.
 * Se per un simbolo manca il disegno del centro o quello degli angoli, la carta non
 * viene disegnata affatto e si ottiene un'eccezione con il nome del simbolo.</p>
 *
 * <p>Disegna ogni volta da zero: per disegnare spesso le stesse carte si passa
 * da {@link CardImageCache}.</p>
 */
public final class CardRenderer {

    /** Altezza / larghezza di una carta. */
    public static final double RATIO = 194.0 / 130.0;

    // Proporzioni, tutte riferite alla larghezza della carta.
    private static final double FRAME_RADIUS = 10.0 / 130;
    private static final double FRAME_THICKNESS = 9.0 / 130;
    private static final double SCREEN_RADIUS = 4.0 / 130;
    private static final double DIGIT_SIZE = 50.0 / 130;
    private static final double CORNER_SIZE = 11.0 / 130;
    private static final double CORNER_MARGIN = 8.0 / 130;
    private static final double TEXT_SHADOW = 3.0 / 130;
    private static final double SCANLINE_STEP = 4.0 / 130;

    /** Spessore del bordo crema delle carte giocabili. */
    private static final double PLAYABLE_BORDER = 3.0 / 130;

    private static final double WILD_WHEEL = 88.0 / 130;
    private static final double WILD_CORNER_WHEEL = 19.0 / 130;
    private static final double REVERSE_GLYPH = 90.0 / 130;
    private static final double SKIP_GLYPH = 86.0 / 130;
    private static final double REVERSE_CORNER = 26.0 / 130;
    private static final double SKIP_CORNER = 22.0 / 130;
    private static final double WILD_RING = 3.0 / 130;

    private static final double MINI_WIDTH = 34.0 / 130;
    private static final double MINI_HEIGHT = 48.0 / 130;
    private static final double MINI_OFFSET_X = 18.0 / 130;
    private static final double MINI_OFFSET_Y = 12.0 / 130;
    private static final double MINI4_WIDTH = 26.0 / 130;
    private static final double MINI4_HEIGHT = 37.0 / 130;
    private static final double MINI4_GAP = 6.0 / 130;
    private static final double MINI_BORDER = 2.5 / 130;

    private static final double STRIPE_HEIGHT = 30.0 / 130;
    private static final double STRIPE_GAP = 14.0 / 130;
    private static final double STRIPE_ANGLE = Math.toRadians(-54);

    /** Come si disegna il contenuto di una carta dentro al suo schermo. */
    @FunctionalInterface
    private interface Glyph {
        void draw(Graphics2D g, Rectangle2D.Double screen, CardFace face, double width);
    }

    private final Map<FaceSymbol, Glyph> glyphs = new EnumMap<>(FaceSymbol.class);

    // Gli angoli dei simboli senza etichetta. Gli altri scrivono negli angoli la loro etichetta.
    private final Map<FaceSymbol, Glyph> cornerGlyphs = new EnumMap<>(FaceSymbol.class);

    public CardRenderer() {
        glyphs.put(FaceSymbol.NUMBER, this::number);
        glyphs.put(FaceSymbol.DRAW_TWO, this::drawTwo);
        glyphs.put(FaceSymbol.REVERSE, this::reverse);
        glyphs.put(FaceSymbol.SKIP, this::skip);
        glyphs.put(FaceSymbol.WILD, this::wild);
        glyphs.put(FaceSymbol.WILD_DRAW_FOUR, this::wildDrawFour);

        cornerGlyphs.put(FaceSymbol.REVERSE, this::reverseCorners);
        cornerGlyphs.put(FaceSymbol.SKIP, this::skipCorners);
        cornerGlyphs.put(FaceSymbol.WILD, this::wildCorners);
    }

    /** Altezza corrispondente a una larghezza. */
    public static int heightFor(int width) {
        return (int) Math.round(width * RATIO);
    }

    /** Disegna una carta scoperta, non evidenziata, con l'angolo in alto a sinistra in (x, y). */
    public void draw(Graphics2D g0, CardFace face, int x, int y, int width) {
        draw(g0, face, x, y, width, false);
    }

    /**
     * Disegna una carta scoperta con l'angolo in alto a sinistra in (x, y).
     *
     * @param playable se vero, la cornice ha il bordo crema delle carte che si possono giocare
     */
    public void draw(Graphics2D g0, CardFace face, int x, int y, int width, boolean playable) {
        Glyph glyph = glyphs.get(face.symbol());
        if (glyph == null) {
            throw new IllegalArgumentException("Nessun disegno registrato per " + face.symbol());
        }
        // Controllato prima di disegnare, così non resta una carta disegnata a metà.
        Glyph corner = cornerGlyphs.get(face.symbol());
        if (corner == null) {
            if (face.label().isEmpty()) {
                throw new IllegalArgumentException("Nessun disegno registrato per gli angoli di " + face.symbol()
                        + ": serve un'etichetta oppure una voce in cornerGlyphs");
            }
            corner = this::labelCorners;
        }
        Graphics2D g = prepare(g0);
        try {
            double w = width;
            double h = w * RATIO;

            frame(g, x, y, w, h, playable);
            Rectangle2D.Double screen = screen(x, y, w, h);
            field(g, screen, ArcadePalette.field(face.color()), w);

            Shape previousClip = g.getClip();
            g.clip(screenShape(screen, w));
            glyph.draw(g, screen, face, w);
            corner.draw(g, screen, face, w);
            g.setClip(previousClip);
        } finally {
            g.dispose();
        }
    }

    /** Disegna una carta coperta: il dorso a bande diagonali. */
    public void drawBack(Graphics2D g0, int x, int y, int width) {
        Graphics2D g = prepare(g0);
        try {
            double w = width;
            double h = w * RATIO;

            frame(g, x, y, w, h, false);
            Rectangle2D.Double screen = screen(x, y, w, h);

            g.setColor(ArcadePalette.FIELD_BACK);
            g.fill(screenShape(screen, w));

            Shape previousClip = g.getClip();
            g.clip(screenShape(screen, w));
            diagonalStripes(g, screen, w);
            scanlines(g, screen, w);
            g.setClip(previousClip);
        } finally {
            g.dispose();
        }
    }

    // ------------------------------------------------------------------
    // parti comuni
    // ------------------------------------------------------------------

    private Graphics2D prepare(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    private void frame(Graphics2D g, double x, double y, double w, double h, boolean playable) {
        double radius = w * FRAME_RADIUS * 2;
        g.setColor(ArcadePalette.FRAME);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, radius, radius));

        // il tratto è centrato sul bordo: lo si rientra di metà spessore per non uscire dalla carta
        double stroke = playable ? Math.max(2, w * PLAYABLE_BORDER) : Math.max(1, w * 1.0 / 130);
        double inset = stroke / 2;
        g.setColor(playable ? ArcadePalette.INK : ArcadePalette.FRAME_EDGE);
        g.setStroke(new BasicStroke((float) stroke));
        g.draw(new RoundRectangle2D.Double(x + inset, y + inset, w - stroke, h - stroke, radius, radius));
    }

    private Rectangle2D.Double screen(double x, double y, double w, double h) {
        double border = w * FRAME_THICKNESS;
        return new Rectangle2D.Double(x + border, y + border, w - 2 * border, h - 2 * border);
    }

    private Shape screenShape(Rectangle2D.Double screen, double w) {
        double radius = w * SCREEN_RADIUS * 2;
        return new RoundRectangle2D.Double(screen.x, screen.y, screen.width, screen.height, radius, radius);
    }

    private void field(Graphics2D g, Rectangle2D.Double screen, Color color, double w) {
        g.setColor(color);
        g.fill(screenShape(screen, w));

        Shape previousClip = g.getClip();
        g.clip(screenShape(screen, w));
        scanlines(g, screen, w);
        g.setClip(previousClip);
    }

    /** Le righe di scansione del monitor: una riga scura ogni due. */
    private void scanlines(Graphics2D g, Rectangle2D.Double screen, double w) {
        double step = Math.max(2, w * SCANLINE_STEP);
        double thickness = step / 2;
        g.setColor(new Color(0f, 0f, 0f, ArcadePalette.SCANLINE_OPACITY));
        for (double yy = screen.y; yy < screen.getMaxY(); yy += step) {
            g.fill(new Rectangle2D.Double(screen.x, yy, screen.width, thickness));
        }
    }

    /** L'etichetta ("7", "+2", "+4") ripetuta nei due angoli opposti. */
    private void labelCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double m = w * CORNER_MARGIN;
        g.setFont(ArcadeFont.of((float) (w * CORNER_SIZE)));
        g.setColor(ArcadePalette.ink(face.color()));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(face.label(), (float) (screen.x + m), (float) (screen.y + m + fm.getAscent()));
        g.drawString(face.label(),
                (float) (screen.getMaxX() - m - fm.stringWidth(face.label())),
                (float) (screen.getMaxY() - m - fm.getDescent()));
    }

    private void reverseCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        matrixCorners(g, screen, face, w, PixelGlyphs.REVERSE, REVERSE_CORNER);
    }

    private void skipCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        matrixCorners(g, screen, face, w, PixelGlyphs.SKIP, SKIP_CORNER);
    }

    /** Il jolly ha negli angoli la ruota dei colori in piccolo. */
    private void wildCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double m = w * CORNER_MARGIN;
        double d = w * WILD_CORNER_WHEEL;
        colorWheel(g, screen.x + m, screen.y + m, d, w * 1.0 / 130);
        colorWheel(g, screen.getMaxX() - m - d, screen.getMaxY() - m - d, d, w * 1.0 / 130);
    }

    /** Un simbolo a blocchi ripetuto in piccolo nei due angoli opposti. */
    private void matrixCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w,
                               int[][] matrix, double size) {
        double width = w * size;
        double height = PixelGlyphs.heightFor(matrix, width);
        double m = w * CORNER_MARGIN;
        Color ink = ArcadePalette.ink(face.color());

        PixelGlyphs.draw(g, matrix, screen.x + m, screen.y + m, width, ink);
        PixelGlyphs.draw(g, matrix, screen.getMaxX() - m - width, screen.getMaxY() - m - height, width, ink);
    }

    // ------------------------------------------------------------------
    // i simboli
    // ------------------------------------------------------------------

    private void number(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        Font font = ArcadeFont.of((float) (w * DIGIT_SIZE));
        double shadow = w * TEXT_SHADOW;
        double cx = screen.getCenterX();
        double cy = screen.getCenterY();

        centeredText(g, face.label(), cx + shadow, cy + shadow, font, ArcadePalette.shadow(face.color()));
        centeredText(g, face.label(), cx, cy, font, ArcadePalette.ink(face.color()));
    }

    /** Due carte sovrapposte, come sulle carte pesca classiche. */
    private void drawTwo(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double cw = w * MINI_WIDTH;
        double ch = w * MINI_HEIGHT;
        double dx = w * MINI_OFFSET_X;
        double dy = w * MINI_OFFSET_Y;
        double x = screen.getCenterX() - (cw + dx) / 2;
        double y = screen.getCenterY() - (ch + dy) / 2;
        Color border = ArcadePalette.ink(face.color());

        miniCard(g, x, y, cw, ch, ArcadePalette.INK, border, w);
        miniCard(g, x + dx, y + dy, cw, ch, ArcadePalette.field(face.color()), border, w);
    }

    /** Quattro carte, una per colore: distingue il pesca quattro dal jolly, che è tondo. */
    private void wildDrawFour(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double cw = w * MINI4_WIDTH;
        double ch = w * MINI4_HEIGHT;
        double gap = w * MINI4_GAP;
        double x0 = screen.getCenterX() - (2 * cw + gap) / 2;
        double y0 = screen.getCenterY() - (2 * ch + gap) / 2;
        CardColor[] colors = ArcadePalette.playableColors();

        for (int i = 0; i < 4; i++) {
            double x = x0 + (i % 2) * (cw + gap);
            double y = y0 + (i / 2) * (ch + gap);
            miniCard(g, x, y, cw, ch, ArcadePalette.field(colors[i]), ArcadePalette.INK, w);
        }
    }

    private void reverse(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        PixelGlyphs.drawCentered(g, PixelGlyphs.REVERSE, screen.getCenterX(), screen.getCenterY(),
                w * REVERSE_GLYPH, ArcadePalette.ink(face.color()));
    }

    private void skip(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        PixelGlyphs.drawCentered(g, PixelGlyphs.SKIP, screen.getCenterX(), screen.getCenterY(),
                w * SKIP_GLYPH, ArcadePalette.ink(face.color()));
    }

    /** Ruota tonda a quattro spicchi. */
    private void wild(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double d = w * WILD_WHEEL;
        colorWheel(g, screen.getCenterX() - d / 2, screen.getCenterY() - d / 2, d, w * WILD_RING);
    }

    // ------------------------------------------------------------------
    // mattoncini
    // ------------------------------------------------------------------

    /** Cerchio diviso in quattro spicchi, con anello crema. */
    private void colorWheel(Graphics2D g, double x, double y, double d, double ring) {
        Shape previousClip = g.getClip();
        g.clip(new Ellipse2D.Double(x, y, d, d));

        CardColor[] colors = ArcadePalette.playableColors();
        double half = d / 2;
        for (int i = 0; i < 4; i++) {
            g.setColor(ArcadePalette.field(colors[i]));
            g.fill(new Rectangle2D.Double(x + (i % 2) * half, y + (i / 2) * half, half + 1, half + 1));
        }
        g.setClip(previousClip);

        g.setColor(ArcadePalette.INK);
        g.setStroke(new BasicStroke((float) Math.max(1, ring)));
        g.draw(new Ellipse2D.Double(x + ring / 2, y + ring / 2, d - ring, d - ring));
    }

    private void miniCard(Graphics2D g, double x, double y, double width, double height,
                          Color fill, Color border, double w) {
        double thickness = Math.max(1, w * MINI_BORDER);
        g.setColor(fill);
        g.fill(new Rectangle2D.Double(x, y, width, height));
        g.setColor(border);
        g.setStroke(new BasicStroke((float) thickness));
        g.draw(new Rectangle2D.Double(x + thickness / 2, y + thickness / 2,
                width - thickness, height - thickness));
    }

    /** Quattro bande oblique nei colori di gioco: il dorso. */
    private void diagonalStripes(Graphics2D g, Rectangle2D.Double screen, double w) {
        double stripe = w * STRIPE_HEIGHT;
        double step = stripe + w * STRIPE_GAP;
        double length = w * 3;

        // ruotate, le bande devono coprire la diagonale dello schermo, non la sua altezza
        double diagonal = Math.hypot(screen.width, screen.height);
        int count = (int) Math.ceil(diagonal / step) + 1;

        AffineTransform previous = g.getTransform();
        g.rotate(STRIPE_ANGLE, screen.getCenterX(), screen.getCenterY());

        CardColor[] colors = ArcadePalette.playableColors();
        double firstY = screen.getCenterY() - (count * step) / 2;
        for (int i = 0; i < count; i++) {
            g.setColor(ArcadePalette.field(colors[i % colors.length]));
            g.fill(new Rectangle2D.Double(screen.getCenterX() - length / 2, firstY + i * step, length, stripe));
        }
        g.setTransform(previous);
    }

    private void centeredText(Graphics2D g, String text, double cx, double cy, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        double width = fm.stringWidth(text);
        double height = fm.getAscent() - fm.getDescent();
        g.drawString(text, (float) (cx - width / 2), (float) (cy + height / 2));
    }
}

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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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

        // No Mercy
        glyphs.put(FaceSymbol.DRAW_FOUR, this::colorDrawFour);
        glyphs.put(FaceSymbol.SKIP_EVERYONE, this::skipEveryone);
        glyphs.put(FaceSymbol.DISCARD_ALL, this::discardAll);
        glyphs.put(FaceSymbol.WILD_DRAW_SIX, this::wildDrawSix);
        glyphs.put(FaceSymbol.WILD_DRAW_TEN, this::wildDrawTen);
        glyphs.put(FaceSymbol.WILD_REVERSE_DRAW_FOUR, this::wildReverseDrawFour);
        glyphs.put(FaceSymbol.WILD_COLOR_ROULETTE, this::colorRoulette);
        glyphs.put(FaceSymbol.WILD_SHUFFLE_HANDS, this::shuffleHands);

        cornerGlyphs.put(FaceSymbol.REVERSE, this::reverseCorners);
        cornerGlyphs.put(FaceSymbol.SKIP, this::skipCorners);
        cornerGlyphs.put(FaceSymbol.WILD, this::wildCorners);
        cornerGlyphs.put(FaceSymbol.SKIP_EVERYONE, this::skipEveryoneCorners);
        cornerGlyphs.put(FaceSymbol.DISCARD_ALL, this::discardAllCorners);
        cornerGlyphs.put(FaceSymbol.WILD_COLOR_ROULETTE, this::colorRouletteCorners);
        cornerGlyphs.put(FaceSymbol.WILD_SHUFFLE_HANDS, this::wildCorners);
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
    // i simboli No Mercy
    // Le misure sono multipli di u, cioè 1/130 della larghezza: restano proporzionali come le altre.
    // ------------------------------------------------------------------

    /** +4 colorato: quattro mini carte dello stesso colore aperte a ventaglio (il Jolly +4 ne ha quattro colori). */
    private void colorDrawFour(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        double cx = screen.getCenterX();
        double cy = screen.getCenterY();
        double cw = 32 * u;
        double ch = 46 * u;
        double pivotY = cy + 52 * u;    // il ventaglio ruota attorno a un punto sotto le carte
        AffineTransform previous = g.getTransform();
        for (double angle : new double[] { -30, -10, 10, 30 }) {
            g.rotate(Math.toRadians(angle), cx, pivotY);
            miniCard(g, cx - cw / 2, cy - ch / 2 - 6 * u, cw, ch,
                    ArcadePalette.field(face.color()), ArcadePalette.ink(face.color()), w);
            g.setTransform(previous);
        }
    }

    private void skipEveryone(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        skipEveryoneSymbol(g, screen.getCenterX(), screen.getCenterY(), w / 130, face.color());
    }

    private void skipEveryoneCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130 * 0.30;
        double half = 56 * u;
        double m = w * CORNER_MARGIN;
        skipEveryoneSymbol(g, screen.x + m + half, screen.y + m + half, u, face.color());
        skipEveryoneSymbol(g, screen.getMaxX() - m - half, screen.getMaxY() - m - half, u, face.color());
    }

    /** Salta tutti: il cerchio sbarrato del Salta con quattro frecce che gli girano attorno in senso orario. */
    private void skipEveryoneSymbol(Graphics2D g, double cx, double cy, double u, CardColor color) {
        Color ink = ArcadePalette.ink(color);
        PixelGlyphs.drawCentered(g, PixelGlyphs.SKIP, cx, cy, 58 * u, ink);

        double r = 46 * u;
        double block = 3 * u;
        int[][] right = PixelGlyphs.SMALL_ARROW;
        int[][] down = PixelGlyphs.rotateClockwise(right);
        int[][] left = PixelGlyphs.rotateClockwise(down);
        int[][] up = PixelGlyphs.rotateClockwise(left);
        PixelGlyphs.drawCentered(g, right, cx, cy - r, 7 * block, ink);
        PixelGlyphs.drawCentered(g, down, cx + r, cy, 5 * block, ink);
        PixelGlyphs.drawCentered(g, left, cx, cy + r, 7 * block, ink);
        PixelGlyphs.drawCentered(g, up, cx - r, cy, 5 * block, ink);

        // puntini sulle diagonali: chiudono il giro
        double d = r * 0.72;
        double dot = Math.max(1, 4 * u);
        g.setColor(ink);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                g.fill(new Rectangle2D.Double(cx + sx * d - dot / 2, cy + sy * d - dot / 2, dot, dot));
            }
        }
    }

    /** Al centro il cestino è ridotto al 75% e alzato, per lasciare liberi gli angoli. */
    private void discardAll(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        binWithCards(g, screen.getCenterX(), screen.getCenterY() - 6 * u, u * 0.75, face.color());
    }

    private void discardAllCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130 * 0.27;
        double halfWidth = 40 * u;
        double halfHeight = 52 * u;
        double m = w * CORNER_MARGIN;
        binWithCards(g, screen.x + m + halfWidth, screen.y + m + halfHeight, u, face.color());
        binWithCards(g, screen.getMaxX() - m - halfWidth, screen.getMaxY() - m - halfHeight, u, face.color());
    }

    /** Scarta tutto: due carte inclinate che entrano in un cestino a pixel. */
    private void binWithCards(Graphics2D g, double cx, double cy, double u, CardColor color) {
        Color ink = ArcadePalette.ink(color);
        Color fill = ArcadePalette.field(color);
        double w = u * 130;
        double cw = 30 * u;
        double ch = 42 * u;

        AffineTransform previous = g.getTransform();
        // { spostamento x, spostamento y, rotazione in gradi } di ciascuna carta
        for (double[] card : new double[][] { { -11, -14, -16 }, { 11, -20, 14 } }) {
            double x = cx + card[0] * u;
            double y = cy + card[1] * u;
            g.rotate(Math.toRadians(card[2]), x, y);
            miniCard(g, x - cw / 2, y - ch / 2, cw, ch, fill, ink, w);
            g.setTransform(previous);
        }

        double binWidth = 72 * u;
        double side = binWidth / PixelGlyphs.BIN[0].length;
        double binHeight = side * PixelGlyphs.BIN.length;
        double bx = cx - binWidth / 2;
        double by = cy + 2 * u;
        // il fondo del cestino copre la parte bassa delle carte, che così sembrano dentro
        g.setColor(fill);
        g.fill(new Rectangle2D.Double(bx + side, by + side, binWidth - 2 * side, binHeight - side));
        PixelGlyphs.draw(g, PixelGlyphs.BIN, bx, by, binWidth, ink);
    }

    /** Jolly +6: sei mini carte, due colonne per tre file, nei quattro colori. */
    private void wildDrawSix(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        double cw = 26 * u;
        double ch = 37 * u;
        double gap = 6 * u;
        double x0 = screen.getCenterX() - (2 * cw + gap) / 2;
        double y0 = screen.getCenterY() - (3 * ch + 2 * gap) / 2;
        CardColor[] colors = {
            CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE, CardColor.YELLOW, CardColor.RED };
        for (int i = 0; i < colors.length; i++) {
            miniCard(g, x0 + (i % 2) * (cw + gap), y0 + (i / 2) * (ch + gap), cw, ch,
                    ArcadePalette.field(colors[i]), ArcadePalette.INK, w);
        }
    }

    /** Jolly +10: "+10" grande dentro una cornice di quadratini che girano nei quattro colori. */
    private void wildDrawTen(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        double cx = screen.getCenterX();
        double cy = screen.getCenterY();

        double square = 7 * u;
        double step = 11 * u;
        int cols = 9;
        int rows = 7;
        double x0 = cx - ((cols - 1) * step + square) / 2;
        double y0 = cy - ((rows - 1) * step + square) / 2;
        // il perimetro percorso in senso orario, così i colori girano attorno alla scritta
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            cells.add(new int[] { i, 0 });
        }
        for (int j = 1; j < rows; j++) {
            cells.add(new int[] { cols - 1, j });
        }
        for (int i = cols - 2; i >= 0; i--) {
            cells.add(new int[] { i, rows - 1 });
        }
        for (int j = rows - 2; j >= 1; j--) {
            cells.add(new int[] { 0, j });
        }
        CardColor[] colors = ArcadePalette.playableColors();
        for (int k = 0; k < cells.size(); k++) {
            g.setColor(ArcadePalette.field(colors[k % colors.length]));
            g.fill(new Rectangle2D.Double(x0 + cells.get(k)[0] * step, y0 + cells.get(k)[1] * step, square, square));
        }

        // il "+" è più piccolo del "10", come un prefisso; prima l'ombra sfalsata, poi la scritta
        Font big = ArcadeFont.of((float) (28 * u));
        Font small = ArcadeFont.of((float) (16 * u));
        g.setFont(big);
        FontMetrics bigMetrics = g.getFontMetrics();
        g.setFont(small);
        FontMetrics smallMetrics = g.getFontMetrics();
        double plusWidth = smallMetrics.stringWidth("+") + 2 * u;
        double left = cx - (plusWidth + bigMetrics.stringWidth("10")) / 2;
        double base = cy + (bigMetrics.getAscent() - bigMetrics.getDescent()) / 2.0;
        double shadow = 3 * u;
        Color[] layers = { ArcadePalette.shadow(face.color()), ArcadePalette.INK };
        for (int i = 0; i < layers.length; i++) {
            double offset = i == 0 ? shadow : 0;
            g.setColor(layers[i]);
            g.setFont(small);
            g.drawString("+", (float) (left + offset), (float) (base - 6 * u + offset));
            g.setFont(big);
            g.drawString("10", (float) (left + plusWidth + offset), (float) (base + offset));
        }
    }

    /** Jolly Cambia Giro +4: le frecce dell'Inverti, ogni metà di un colore, con "+4" sopra. */
    private void wildReverseDrawFour(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        double cx = screen.getCenterX();
        double cy = screen.getCenterY();

        int[][] matrix = PixelGlyphs.REVERSE;
        double width = 90 * u;
        double side = width / matrix[0].length;
        double x0 = cx - width / 2;
        double y0 = cy + 12 * u - side * matrix.length / 2;
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[0].length; c++) {
                if (matrix[r][c] == 0) {
                    continue;
                }
                boolean leftArrow = c < matrix[0].length / 2;
                boolean top = r < matrix.length / 2;
                CardColor color = leftArrow ? (top ? CardColor.RED : CardColor.YELLOW)
                                            : (top ? CardColor.GREEN : CardColor.BLUE);
                g.setColor(ArcadePalette.field(color));
                // come in PixelGlyphs: blocchi allineati al pixel, senza fessure
                int ax = (int) Math.round(x0 + c * side);
                int bx = (int) Math.round(x0 + (c + 1) * side);
                int ay = (int) Math.round(y0 + r * side);
                int by = (int) Math.round(y0 + (r + 1) * side);
                g.fillRect(ax, ay, bx - ax, by - ay);
            }
        }

        Font font = ArcadeFont.of((float) (16 * u));
        centeredText(g, face.label(), cx + 2 * u, cy - 28 * u, font, ArcadePalette.shadow(face.color()));
        centeredText(g, face.label(), cx, cy - 30 * u, font, ArcadePalette.INK);
    }

    /** Roulette: la ruota del jolly con i perni sull'anello e una lancetta in alto, come una ruota della fortuna. */
    private void colorRoulette(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        rouletteWheel(g, screen.getCenterX(), screen.getCenterY() + 7 * u, 80 * u, u, true);
    }

    /**
     * Rimescola Mani: le quattro mini carte del Jolly +4, ma sparse e storte come un mazzo appena mescolato,
     * invece che allineate in griglia. Negli angoli la ruota dei colori, come il jolly semplice.
     */
    private void shuffleHands(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        double cx = screen.getCenterX();
        double cy = screen.getCenterY();
        double cw = 34 * u;
        double ch = 48 * u;
        double[] offsetsX = { -20, 18, -16, 20 };
        double[] offsetsY = { -22, -18, 20, 16 };
        double[] angles = { -28, 16, 22, -14 };
        CardColor[] colors = ArcadePalette.playableColors();
        AffineTransform previous = g.getTransform();
        for (int i = 0; i < colors.length; i++) {
            double x = cx + offsetsX[i] * u;
            double y = cy + offsetsY[i] * u;
            g.rotate(Math.toRadians(angles[i]), x, y);
            miniCard(g, x - cw / 2, y - ch / 2, cw, ch, ArcadePalette.field(colors[i]), ArcadePalette.INK, w);
            g.setTransform(previous);
        }
    }

    /** Negli angoli la ruota in piccolo, senza perni; quella in alto scende un po' per far posto alla lancetta. */
    private void colorRouletteCorners(Graphics2D g, Rectangle2D.Double screen, CardFace face, double w) {
        double u = w / 130;
        double d = 19 * u;
        double m = w * CORNER_MARGIN;
        rouletteWheel(g, screen.x + m + d / 2, screen.y + m + 5 * u + d / 2, d, u * 0.3, false);
        rouletteWheel(g, screen.getMaxX() - m - d / 2, screen.getMaxY() - m - d / 2, d, u * 0.3, false);
    }

    private void rouletteWheel(Graphics2D g, double cx, double cy, double d, double u, boolean withPegs) {
        colorWheel(g, cx - d / 2, cy - d / 2, d, withPegs ? 3 * u : Math.max(1, d / 19));
        if (withPegs) {
            g.setColor(ArcadePalette.INK);
            double radius = d / 2 - 1.5 * u;
            double peg = 4 * u;
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI / 8 + i * Math.PI / 4;
                g.fill(new Rectangle2D.Double(cx + Math.cos(angle) * radius - peg / 2,
                        cy + Math.sin(angle) * radius - peg / 2, peg, peg));
            }
        }
        double pointerWidth = withPegs ? 22 * u : d * 0.42;
        double pointerHeight = PixelGlyphs.heightFor(PixelGlyphs.POINTER, pointerWidth);
        double top = cy - d / 2 - pointerHeight * 0.55;
        // un bordo scuro dietro la lancetta la stacca dagli spicchi
        double outline = pointerWidth / 14;
        PixelGlyphs.draw(g, PixelGlyphs.POINTER, cx - pointerWidth / 2 - outline, top - outline,
                pointerWidth + 2 * outline, ArcadePalette.FRAME);
        PixelGlyphs.draw(g, PixelGlyphs.POINTER, cx - pointerWidth / 2, top, pointerWidth, ArcadePalette.INK);
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

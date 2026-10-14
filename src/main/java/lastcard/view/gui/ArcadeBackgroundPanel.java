package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Lo sfondo delle schermate: la cornice scura del cabinato e, dentro, lo "schermo" con l'immagine di
 * sfondo e le righe di scansione.
 *
 * L'immagine copre tutto lo schermo senza deformarsi: se la finestra ha proporzioni diverse, ne
 * taglia un po' ai bordi. Cornice e righe le disegna il codice, così restano nitide a ogni dimensione.
 * Se l'immagine manca, lo schermo resta in tinta unita e l'avviso va sullo standard error.
 *
 * I componenti aggiunti restano dentro lo schermo, mai sopra la cornice; quelli che devono lasciar
 * vedere lo sfondo vanno resi trasparenti con setOpaque(false).
 */
public class ArcadeBackgroundPanel extends JPanel {

    /** Lo sfondo del menu principale: carte sparse e scurite, 1920x1200. */
    public static final String MENU_BACKGROUND = "/images/menu-background.png";

    static final Color BEZEL = new Color(0x1C1D24);
    static final Color SCREEN = new Color(0x111216);
    static final Color SCREEN_EDGE = new Color(0x2A2B33);

    private static final int MARGIN = 18;
    private static final int RADIUS = 18;
    private static final float SCANLINE_ALPHA = 0.18f;
    private static final int SCANLINE_STEP = 3;
    /** Spazio fra il bordo dello schermo e i componenti. */
    private static final int PADDING = 14;

    // Le immagini già caricate: più schermate con lo stesso sfondo ne tengono in memoria una copia sola.
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();

    private final transient BufferedImage image;

    /** Sfondo con immagine, per esempio {@link #MENU_BACKGROUND}; null per lo schermo in tinta unita. */
    public ArcadeBackgroundPanel(LayoutManager layout, String imageResource) {
        super(layout);
        this.image = imageResource == null ? null : image(imageResource);
        setOpaque(true);
        int inset = MARGIN + PADDING;
        setBorder(new EmptyBorder(inset, inset, inset, inset));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(BEZEL);
            g.fillRect(0, 0, w, h);

            Shape screen = new RoundRectangle2D.Double(MARGIN, MARGIN, w - 2.0 * MARGIN, h - 2.0 * MARGIN, RADIUS, RADIUS);
            g.setColor(SCREEN);
            g.fill(screen);

            Shape clip = g.getClip();
            g.clip(screen);
            if (image != null) {
                drawCover(g, w, h);
            }
            g.setColor(new Color(0f, 0f, 0f, SCANLINE_ALPHA));
            for (int y = MARGIN; y < h - MARGIN; y += SCANLINE_STEP) {
                g.fillRect(MARGIN, y, w - 2 * MARGIN, 1);
            }
            g.setClip(clip);

            g.setColor(SCREEN_EDGE);
            g.setStroke(new BasicStroke(2f));
            g.draw(screen);
        } finally {
            g.dispose();
        }
    }

    // Scala l'immagine finché copre tutto lo schermo, centrata; quello che avanza resta fuori dal clip.
    private void drawCover(Graphics2D g, int w, int h) {
        double sw = w - 2.0 * MARGIN;
        double sh = h - 2.0 * MARGIN;
        double scale = Math.max(sw / image.getWidth(), sh / image.getHeight());
        int dw = (int) Math.ceil(image.getWidth() * scale);
        int dh = (int) Math.ceil(image.getHeight() * scale);
        int dx = (int) Math.round(MARGIN + (sw - dw) / 2);
        int dy = (int) Math.round(MARGIN + (sh - dh) / 2);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, dx, dy, dw, dh, null);
    }

    /** Un'immagine delle risorse, caricata una volta sola e condivisa; null se manca. La usa anche il tavolo. */
    static BufferedImage image(String resource) {
        return IMAGES.computeIfAbsent(resource, r -> load(r));
    }

    private static BufferedImage load(String resource) {
        try (InputStream in = ArcadeBackgroundPanel.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("risorsa non trovata: " + resource);
            }
            return ImageIO.read(in);
        } catch (Exception e) {
            System.err.println("[ArcadeBackgroundPanel] " + e.getMessage() + " - sfondo in tinta unita");
            return null;
        }
    }
}

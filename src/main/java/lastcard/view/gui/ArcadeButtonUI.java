package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicHTML;

/**
 * Disegna i pulsanti nello stile cabinato: rettangoli piatti, righe di scansione, testo in Press Start 2P.
 *
 * Tre varianti, scelte con {@link ButtonFactory#setVariant}:
 * PRIMARY (crema con ombra rossa, l'azione principale), SECONDARY (scuro con bordo) e QUIET (come
 * SECONDARY ma con il testo spento, per Esci e Indietro). Un pulsante a due stati selezionato si
 * disegna come PRIMARY: è così che ChoiceGroup mostra la scelta attiva.
 *
 * Stati: mouse sopra o focus da tastiera schiariscono il bordo; premuto, il pulsante scende sulla
 * sua ombra; disattivato, si spegne. Il testo semplice va in maiuscolo; il testo HTML (i pulsanti su
 * due righe di Gioca) lo disegna Swing, con il colore del tema.
 */
public class ArcadeButtonUI extends BasicButtonUI {

    /** Le varianti di aspetto. */
    public enum Variant { PRIMARY, SECONDARY, QUIET }

    /** Chiave della proprietà del pulsante che contiene la variante. */
    static final String VARIANT_KEY = "lastcard.arcadeVariant";

    // Colori del tema, oltre a quelli delle carte in ArcadePalette.
    static final Color CREAM = new Color(0xF6E3AE);
    static final Color CREAM_HOVER = new Color(0xFFF8E2);
    static final Color SHADOW = new Color(0xC32B3A);
    static final Color BODY = new Color(0x15161B);
    static final Color BODY_HOVER = new Color(0x1D1E25);
    static final Color BODY_PRESSED = new Color(0x0E0F13);
    static final Color EDGE = new Color(0x3A3B47);
    static final Color TEXT_DARK = new Color(0x1A1410);
    static final Color TEXT_MUTED = new Color(0x8A8C99);
    static final Color TEXT_DISABLED = new Color(0x4A4C57);

    /** Quanto l'ombra rossa sporge in basso a destra; il pulsante premuto scende di questa misura. */
    static final int DEPTH = 4;
    private static final int ARC = 4;
    private static final int PAD_X = 24;
    private static final int HEIGHT = 46;
    /** Il corpo del testo dei pulsanti a grandezza normale; più grande, il pulsante cresce in proporzione. */
    static final float FONT_SIZE = 14f;
    /** Spessore del bordo rosso che compare sui pulsanti PRIMARY col mouse sopra. */
    private static final int HOVER_BORDER = 3;
    /** La freccetta del focus da tastiera: larghezza e distanza dal testo. */
    static final int FOCUS_ARROW_WIDTH = 6;
    static final int FOCUS_ARROW_GAP = 10;

    /** Freccia a pixel che punta a destra: il focus da tastiera, come il cursore dei menu dei cabinati. */
    private static final int[][] FOCUS_ARROW = {
        { 1,0,0 },
        { 1,1,0 },
        { 1,1,1 },
        { 1,1,0 },
        { 1,0,0 }
    };

    private static final ArcadeButtonUI INSTANCE = new ArcadeButtonUI();

    /** Lo stesso oggetto va bene per tutti i pulsanti: non tiene stato. */
    public static ArcadeButtonUI instance() {
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
        b.setForeground(CREAM);
        // il font a pixel solo per il testo semplice: le righe di descrizione in HTML restano leggibili
        if (!isHtml(b)) {
            b.setFont(ArcadeFont.of(FONT_SIZE));
        }
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        if (isHtml(b)) {
            Dimension d = super.getPreferredSize(c);
            return new Dimension(d.width + 2 * PAD_X + DEPTH, Math.max(HEIGHT, d.height + 28) + DEPTH);
        }
        // Altezza e margini crescono con il font: per ingrandire un pulsante basta dargli un font più grande.
        float scale = Math.max(1f, b.getFont().getSize2D() / FONT_SIZE);
        FontMetrics fm = b.getFontMetrics(b.getFont());
        int text = fm.stringWidth(label(b));
        return new Dimension(text + Math.round(2 * PAD_X * scale) + DEPTH, Math.round(HEIGHT * scale) + DEPTH);
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean enabled = m.isEnabled();
            boolean pressed = m.isArmed() && m.isPressed();
            // Mouse sopra e focus da tastiera si mostrano in modo diverso: all'apertura di una schermata il
            // focus va al primo pulsante, e se avessero lo stesso aspetto quello sembrerebbe sempre "sotto il mouse".
            // La freccetta del focus compare solo usando la tastiera: lo decide FocusIndicator.
            boolean hot = enabled && m.isRollover();
            boolean focused = FocusIndicator.shows(b);
            Variant v = m.isSelected() ? Variant.PRIMARY : variantOf(b);

            int w = c.getWidth() - DEPTH;
            int h = c.getHeight() - DEPTH;
            // premuto: il corpo scende sull'ombra, come un tasto vero
            int off = pressed ? DEPTH / 2 : 0;
            Shape body = new RoundRectangle2D.Double(off, off, w, h, ARC, ARC);

            Color text;
            if (v == Variant.PRIMARY) {
                if (enabled) {
                    g.setColor(SHADOW);
                    g.fill(new RoundRectangle2D.Double(DEPTH, DEPTH, w, h, ARC, ARC));
                }
                g.setColor(!enabled ? EDGE : hot ? CREAM_HOVER : CREAM);
                g.fill(body);
                scanlines(g, body, hot ? 0.04f : 0.10f);
                if (hot) {
                    // mouse sopra: il crema si accende e compare un bordo rosso, come la scritta
                    g.setStroke(new BasicStroke(HOVER_BORDER));
                    g.setColor(SHADOW);
                    double inset = HOVER_BORDER / 2.0;
                    g.draw(new RoundRectangle2D.Double(off + inset, off + inset, w - HOVER_BORDER, h - HOVER_BORDER,
                            ARC, ARC));
                }
                text = !enabled ? TEXT_MUTED : hot ? SHADOW : TEXT_DARK;
            } else {
                g.setColor(pressed ? BODY_PRESSED : hot ? BODY_HOVER : BODY);
                g.fill(body);
                g.setStroke(new BasicStroke(2f));
                g.setColor(hot ? CREAM : EDGE);
                g.draw(new RoundRectangle2D.Double(off + 1, off + 1, w - 2, h - 2, ARC, ARC));
                text = !enabled ? TEXT_DISABLED : (v == Variant.QUIET && !hot) ? TEXT_MUTED : CREAM;
            }

            if (isHtml(b)) {
                // l'HTML lo impagina Swing: si sposta solo l'origine e si imposta il colore
                b.setForeground(text);
                g.translate(off, off);
                g.clipRect(0, 0, w, h);
                Graphics2D gh = (Graphics2D) g.create();
                try {
                    paintHtml(gh, b, w, h);
                } finally {
                    gh.dispose();
                }
                if (focused) {
                    paintFocusArrow(g, FOCUS_ARROW_GAP, h / 2.0, text);
                }
            } else {
                Font f = b.getFont();
                g.setFont(f);
                FontMetrics fm = g.getFontMetrics();
                String s = label(b);
                int tx = off + (w - fm.stringWidth(s)) / 2;
                int ty = off + (h - fm.getHeight()) / 2 + fm.getAscent();
                g.setColor(text);
                g.drawString(s, tx, ty);
                if (focused) {
                    paintFocusArrow(g, tx - FOCUS_ARROW_GAP - FOCUS_ARROW_WIDTH, off + h / 2.0, text);
                }
            }
        } finally {
            g.dispose();
        }
    }

    // ------------------------------------------------------------------

    static Variant variantOf(AbstractButton b) {
        Object v = b.getClientProperty(VARIANT_KEY);
        return v instanceof Variant ? (Variant) v : Variant.SECONDARY;
    }

    private static boolean isHtml(AbstractButton b) {
        return BasicHTML.isHTMLString(b.getText());
    }

    private static String label(AbstractButton b) {
        String t = b.getText();
        return t == null ? "" : t.toUpperCase();
    }

    /** La freccetta del focus con il bordo sinistro in x, centrata in verticale su cy. La usano anche le caselle. */
    static void paintFocusArrow(Graphics2D g, double x, double cy, Color color) {
        double height = PixelGlyphs.heightFor(FOCUS_ARROW, FOCUS_ARROW_WIDTH);
        PixelGlyphs.draw(g, FOCUS_ARROW, x, cy - height / 2, FOCUS_ARROW_WIDTH, color);
    }

    /** Righe di scansione orizzontali sul corpo del pulsante, come sulle carte. */
    private static void scanlines(Graphics2D g, Shape area, float alpha) {
        Shape clip = g.getClip();
        g.clip(area);
        Rectangle2D r = area.getBounds2D();
        g.setColor(new Color(0f, 0f, 0f, alpha));
        for (double y = r.getY(); y < r.getMaxY(); y += 4) {
            g.fill(new Rectangle2D.Double(r.getX(), y, r.getWidth(), 2));
        }
        g.setClip(clip);
    }

    /** Il testo HTML centrato nel pulsante, impaginato dalla View che Swing ha già preparato. */
    private static void paintHtml(Graphics2D g, AbstractButton b, int w, int h) {
        javax.swing.text.View view = (javax.swing.text.View) b.getClientProperty(BasicHTML.propertyKey);
        if (view == null) {
            return;
        }
        Insets in = new Insets(14, PAD_X, 14, PAD_X);
        float pw = view.getPreferredSpan(javax.swing.text.View.X_AXIS);
        float ph = view.getPreferredSpan(javax.swing.text.View.Y_AXIS);
        int availW = w - in.left - in.right;
        int x = in.left + Math.max(0, (int) (availW - pw) / 2);
        int y = (h - (int) ph) / 2;
        view.paint(g, new java.awt.Rectangle(x, y, Math.min(availW, (int) pw), (int) ph));
    }
}

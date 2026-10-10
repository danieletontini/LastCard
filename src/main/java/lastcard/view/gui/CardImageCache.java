package lastcard.view.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lastcard.model.CardFace;

/**
 * Tiene in memoria le carte già disegnate, per non ridisegnarle a ogni repaint.
 *
 * La prima volta che serve una carta a una certa larghezza, {@link CardRenderer}
 * la disegna su una {@link BufferedImage}; le volte dopo si copia l'immagine,
 * che è molto più veloce. Niente finisce su disco: le immagini vivono solo
 * finché il programma è aperto e si rifanno da sole al prossimo avvio.
 *
 * Le carte uguali condividono la stessa immagine, perché la chiave è la
 * {@link CardFace} (un record, quindi uguale per valore) e non la carta: le
 * 108 carte del mazzo classico hanno 54 facce diverse.
 *
 * Si usa solo dal thread di Swing, come tutto il resto della GUI.
 */
public final class CardImageCache {

    /** Una voce: cosa c'è disegnato e a che dimensione. {@code face} è null per il dorso. */
    private record Key(CardFace face, int pixelWidth, boolean playable) {
    }

    private final CardRenderer renderer;
    private final Map<Key, BufferedImage> images = new HashMap<>();

    public CardImageCache() {
        this(new CardRenderer());
    }

    public CardImageCache(CardRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /** Disegna una carta scoperta, presa dalla cache se c'è già. */
    public void draw(Graphics2D g, CardFace face, int x, int y, int width, boolean playable) {
        Objects.requireNonNull(face, "face");
        paint(g, new Key(face, pixelWidth(g, width), playable), x, y, width);
    }

    /** Disegna una carta coperta, presa dalla cache se c'è già. */
    public void drawBack(Graphics2D g, int x, int y, int width) {
        paint(g, new Key(null, pixelWidth(g, width), false), x, y, width);
    }

    /** Quante immagini ci sono in memoria. */
    public int size() {
        return images.size();
    }

    /** Svuota la cache, per esempio se cambia la dimensione delle carte in modo permanente. */
    public void clear() {
        images.clear();
    }

    private void paint(Graphics2D g, Key key, int x, int y, int width) {
        BufferedImage image = images.computeIfAbsent(key, this::render);
        Graphics2D copy = (Graphics2D) g.create();
        try {
            // l'immagine ha già la risoluzione dello schermo: va solo riportata alla dimensione logica
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            copy.drawImage(image, x, y, width, CardRenderer.heightFor(width), null);
        } finally {
            copy.dispose();
        }
    }

    private BufferedImage render(Key key) {
        int w = key.pixelWidth();
        BufferedImage image = new BufferedImage(w, CardRenderer.heightFor(w), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            if (key.face() == null) {
                renderer.drawBack(g, 0, 0, w);
            } else {
                renderer.draw(g, key.face(), 0, 0, w, key.playable());
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Larghezza reale in pixel. Con lo schermo scalato da Windows (125%, 150%)
     * una carta larga 130 occupa 163 o 195 pixel veri: si disegna a quella
     * risoluzione, altrimenti l'immagine ingrandita viene sfocata.
     */
    private static int pixelWidth(Graphics2D g, int width) {
        AffineTransform t = g.getTransform();
        double scale = Math.max(1.0, Math.hypot(t.getScaleX(), t.getShearY()));
        return (int) Math.round(width * scale);
    }
}

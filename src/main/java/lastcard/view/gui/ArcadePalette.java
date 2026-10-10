package lastcard.view.gui;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import lastcard.model.CardColor;

/**
 * I colori dello stile cabinato. Unico punto in cui vivono i valori esadecimali:
 * cambiare una tinta qui la cambia ovunque.
 */
public final class ArcadePalette {

    private ArcadePalette() {
    }

    /** Cornice scura della carta, come il bezel di un monitor. */
    public static final Color FRAME = new Color(0x1C1C24);

    /** Filo chiaro all'interno della cornice. */
    public static final Color FRAME_EDGE = new Color(0x343442);

    /** Crema dei simboli e del testo, e del bordo delle carte giocabili. */
    public static final Color INK = new Color(0xFFE9A8);

    /** Inchiostro alternativo: sul giallo il crema non ha contrasto sufficiente. */
    public static final Color INK_DARK = new Color(0x2B1C00);

    /** Campo delle carte senza colore proprio: jolly e pesca quattro. */
    public static final Color FIELD_BLACK = new Color(0x15151C);

    /** Campo del dorso. */
    public static final Color FIELD_BACK = new Color(0x121A2E);

    /** Verde del tavolo da gioco, per quando serve la tinta piatta invece dell'immagine. */
    public static final Color TABLE = new Color(0x1B5E3A);

    /** Opacità delle righe di scansione sul campo colore. */
    public static final float SCANLINE_OPACITY = 0.26f;

    private static final Map<CardColor, Color> FIELD = new EnumMap<>(CardColor.class);
    private static final Map<CardColor, Color> SHADOW = new EnumMap<>(CardColor.class);

    static {
        FIELD.put(CardColor.RED, new Color(0xC32B3A));
        FIELD.put(CardColor.YELLOW, new Color(0xD99A12));
        FIELD.put(CardColor.GREEN, new Color(0x2A8F4E));
        FIELD.put(CardColor.BLUE, new Color(0x2461C4));
        FIELD.put(CardColor.BLACK, FIELD_BLACK);

        SHADOW.put(CardColor.RED, new Color(0x5E0F18));
        // sul giallo si scrive scuro, quindi il distacco lo dà una tinta chiara
        SHADOW.put(CardColor.YELLOW, new Color(0xF6E3AE));
        SHADOW.put(CardColor.GREEN, new Color(0x0E3E20));
        SHADOW.put(CardColor.BLUE, new Color(0x0D2A58));
        SHADOW.put(CardColor.BLACK, new Color(0x000000));
    }

    /** Colore di fondo della carta. */
    public static Color field(CardColor color) {
        return FIELD.get(color);
    }

    /** Tinta più scura dello stesso campo, per l'ombra sfalsata del testo. */
    public static Color shadow(CardColor color) {
        return SHADOW.get(color);
    }

    /**
     * Colore di simboli e testo sul campo indicato.
     *
     * <p>Sul giallo il crema darebbe un contrasto di circa 1.8:1, illeggibile:
     * è l'unico caso in cui si scrive scuro invece che chiaro.</p>
     */
    public static Color ink(CardColor color) {
        return color == CardColor.YELLOW ? INK_DARK : INK;
    }

    /** I quattro colori giocabili, nell'ordine usato da jolly, pesca quattro e dorso. */
    public static CardColor[] playableColors() {
        return new CardColor[] { CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE };
    }
}

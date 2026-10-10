package lastcard.view.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

/**
 * Carica il font a pixel Press Start 2P dalle risorse del progetto.
 *
 * Non è installato su nessun sistema, quindi viaggia dentro al jar:
 * {@code src/main/resources/font/PressStart2P-Regular.ttf} (licenza SIL OFL,
 * testo in {@code OFL-PressStart2P.txt}). Se manca, il gioco non si pianta:
 * usa il monospazio di sistema e avvisa sullo standard error.
 */
public final class ArcadeFont {

    private static final String RESOURCE = "/font/PressStart2P-Regular.ttf";

    private static Font base;

    private ArcadeFont() {
    }

    /** Il font alla dimensione richiesta. La prima chiamata lo carica. */
    public static synchronized Font of(float size) {
        if (base == null) {
            base = load();
        }
        return base.deriveFont(size);
    }

    private static Font load() {
        try (InputStream in = ArcadeFont.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("risorsa non trovata: " + RESOURCE);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            System.err.println("[ArcadeFont] " + e.getMessage() + " - uso il monospazio di sistema");
            return new Font(Font.MONOSPACED, Font.BOLD, 12);
        }
    }
}

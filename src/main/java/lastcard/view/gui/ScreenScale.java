package lastcard.view.gui;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;

/**
 * Quanto ingrandire testi e controlli di una schermata, in base alla sua misura: 1 nella finestra da 960x640,
 * di più a schermo intero (circa 1,7 a 1920x1080). Non scende sotto 1 e non supera 2,5.
 * Le schermate moltiplicano le loro misure per questo valore, così le proporzioni restano quelle di partenza.
 */
final class ScreenScale {

    static final int BASE_WIDTH = 960;
    static final int BASE_HEIGHT = 640;
    /** Oltre questa scala non si cresce più, anche su schermi molto grandi. */
    static final float MAX = 2.5f;

    private ScreenScale() {
    }

    static float of(Component component) {
        float byWidth = component.getWidth() / (float) BASE_WIDTH;
        float byHeight = component.getHeight() / (float) BASE_HEIGHT;
        return Math.max(1f, Math.min(MAX, Math.min(byWidth, byHeight)));
    }

    /** Chiama apply con la nuova scala ogni volta che il componente cambia misura e la scala cambia davvero. */
    static void follow(Component component, Consumer<Float> apply) {
        component.addComponentListener(new ComponentAdapter() {
            private float last = 1f;

            @Override
            public void componentResized(ComponentEvent e) {
                float scale = of(component);
                if (scale != last) {
                    last = scale;
                    apply.accept(scale);
                }
            }
        });
    }
}

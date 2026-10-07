package uno.view.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;

// Se i file delle regole non finiscono tra le risorse (e quindi nel jar), questi test se ne accorgono.
class RulesLoaderTest {

    public RulesLoaderTest() {
    }

    @Test
    void classicRulesAreLoaded() throws IOException {
        String text = RulesLoader.load(RulesLoader.CLASSIC_PATH);
        assertFalse(text.isBlank());
    }

    @Test
    void noMercyRulesAreLoaded() throws IOException {
        String text = RulesLoader.load(RulesLoader.NO_MERCY_PATH);
        assertFalse(text.isBlank());
    }

    @Test
    void accentedLettersAreReadCorrectly() throws IOException {
        String text = RulesLoader.load(RulesLoader.NO_MERCY_PATH);
        assertTrue(text.contains("più"));
    }

    @Test
    void missingFileIsReportedWithAnException() {
        try {
            RulesLoader.load("/rules/missing.txt");
            fail("A missing file should throw IOException.");
        } catch (IOException e) {
            // atteso: la schermata mostra un messaggio d'errore invece di andare in crash
        }
    }
}

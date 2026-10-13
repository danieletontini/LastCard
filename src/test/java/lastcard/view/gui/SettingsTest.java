package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Le opzioni partono da valori sensati, restano salvate e il muto porta il volume effettivo a zero.
class SettingsTest {

    private Preferences node;

    @BeforeEach
    void setUp() {
        // Un nodo a parte per ogni test, così le impostazioni vere dell'utente non vengono toccate.
        node = Preferences.userRoot().node("lastcard-test-" + System.nanoTime());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        node.removeNode();
    }

    @Test
    void defaultsAreWindowedWithSoundOn() {
        Settings settings = new Settings(node);

        assertFalse(settings.isFullScreen());
        assertFalse(settings.isMuted());
        assertEquals(80, settings.getVolume());
    }

    @Test
    void choicesAreStillThereNextTime() {
        Settings first = new Settings(node);
        first.setFullScreen(true);
        first.setMuted(true);
        first.setVolume(35);

        Settings next = new Settings(node);

        assertTrue(next.isFullScreen());
        assertTrue(next.isMuted());
        assertEquals(35, next.getVolume());
    }

    @Test
    void everyStartIsFullScreenWithSoundAtSixtySeven() {
        Settings last = new Settings(node);
        last.setFullScreen(false);
        last.setMuted(true);
        last.setVolume(20);

        Settings next = new Settings(node);
        next.applyStartupDefaults();

        assertTrue(next.isFullScreen());
        assertFalse(next.isMuted());
        assertEquals(67, next.getVolume());
        assertEquals(67, next.effectiveVolume());
    }

    @Test
    void volumeStaysBetweenZeroAndOneHundred() {
        Settings settings = new Settings(node);

        settings.setVolume(150);
        assertEquals(100, settings.getVolume());

        settings.setVolume(-5);
        assertEquals(0, settings.getVolume());
    }

    @Test
    void muteSilencesWithoutForgettingTheVolume() {
        Settings settings = new Settings(node);
        settings.setVolume(60);

        settings.setMuted(true);
        assertEquals(0, settings.effectiveVolume());
        assertEquals(60, settings.getVolume());

        settings.setMuted(false);
        assertEquals(60, settings.effectiveVolume());
    }
}

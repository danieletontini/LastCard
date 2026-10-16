package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/** Il percorso fra le schermate: Indietro riporta da dove si è venuti, mai a un tavolo già lasciato. */
class NavigatorTest {

    private static Navigator navigator() {
        Navigator navigator = new Navigator();
        for (Screen screen : Screen.values()) {
            navigator.register(screen, new JPanel());
        }
        return navigator;
    }

    // Dal menu fino al tavolo, come quando si inizia una partita.
    private static Navigator atTheTable() {
        Navigator navigator = navigator();
        navigator.show(Screen.MAIN_MENU);
        navigator.show(Screen.PLAY);
        navigator.show(Screen.NEW_GAME);
        navigator.show(Screen.TABLE);
        return navigator;
    }

    @Test
    void backGoesThroughTheScreensInReverseOrder() {
        Navigator navigator = navigator();
        navigator.show(Screen.MAIN_MENU);
        navigator.show(Screen.PLAY);
        navigator.show(Screen.NEW_GAME);

        navigator.back();
        assertEquals(Screen.PLAY, navigator.current());
        navigator.back();
        assertEquals(Screen.MAIN_MENU, navigator.current());
        navigator.back();
        assertEquals(Screen.MAIN_MENU, navigator.current(), "dal menu principale non si torna più indietro");
    }

    @Test
    void afterAGameNewGameThenBackDoesNotReturnToTheTable() {
        Navigator navigator = atTheTable();

        navigator.show(Screen.NEW_GAME);
        navigator.back();

        assertEquals(Screen.PLAY, navigator.current());
        navigator.back();
        assertEquals(Screen.MAIN_MENU, navigator.current());
    }

    @Test
    void leavingTheTableForTheMainMenuForgetsThePath() {
        Navigator navigator = atTheTable();

        navigator.show(Screen.MAIN_MENU);
        navigator.show(Screen.OPTIONS);
        navigator.back();
        assertEquals(Screen.MAIN_MENU, navigator.current());
        navigator.back();
        assertEquals(Screen.MAIN_MENU, navigator.current(), "il tavolo lasciato non è più dietro al menu");
    }

    @Test
    void aSecondGameKeepsTheSamePath() {
        Navigator navigator = atTheTable();
        navigator.show(Screen.NEW_GAME);
        navigator.show(Screen.TABLE);

        navigator.show(Screen.NEW_GAME);
        navigator.back();

        assertEquals(Screen.PLAY, navigator.current(), "anche dopo più partite di fila");
    }
}

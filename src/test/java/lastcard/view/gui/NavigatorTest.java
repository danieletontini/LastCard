package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/** Il percorso fra le schermate: Indietro riporta da dove si è venuti, mai a un tavolo già lasciato. */
class NavigatorTest {

    // Dal menu fino al tavolo, come quando si inizia una partita.
    private static Navigator atTheTable() {
        Navigator navigator = new Navigator();
        for (Screen screen : Screen.values()) {
            navigator.register(screen, new JPanel());
        }
        navigator.show(Screen.MAIN_MENU);
        navigator.show(Screen.NEW_GAME);
        navigator.show(Screen.TABLE);
        return navigator;
    }

    @Test
    void backNeverReturnsToATableAlreadyLeft() {
        Navigator newGame = atTheTable();
        newGame.show(Screen.NEW_GAME);
        newGame.back();
        assertEquals(Screen.MAIN_MENU, newGame.current(), "da Nuova partita dopo una partita si torna al menu");

        Navigator menu = atTheTable();
        menu.show(Screen.MAIN_MENU);
        menu.show(Screen.OPTIONS);
        menu.back();
        menu.back();
        assertEquals(Screen.MAIN_MENU, menu.current(), "il tavolo lasciato non è più dietro al menu");
    }
}

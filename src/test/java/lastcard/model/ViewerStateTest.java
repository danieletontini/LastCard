package lastcard.model;

import static lastcard.model.GameSetup.hand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Lo stato visto da un giocatore preciso: il tavolo grafico mostra sempre la mano dell'umano, anche quando
 * tocca a una CPU, mentre chi deve agire resta quello vero.
 */
class ViewerStateTest {

    private static final List<String> ABC = List.of("A", "B", "C");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);

    @Test
    void aPlayerSeesTheirOwnHandEvenWhenItIsNotTheirTurn() {
        Card marker = new NumberCard(CardColor.BLUE, 1);
        Game game = GameSetup.start(ABC, List.of(hand(), hand(marker), hand()), RED_FIVE);

        VisibleState seenByB = game.getVisibleStateFor("B");

        assertEquals("A", seenByB.currentPlayerName(), "chi deve agire non cambia");
        assertTrue(seenByB.currentHand().contains(marker));
        assertEquals(game.getVisibleState().players(), seenByB.players());
    }

    @Test
    void onlyTheActingPlayerCanPass() {
        List<Card> drawPile = new ArrayList<>(List.of(new NumberCard(CardColor.RED, 2)));
        drawPile.addAll(Collections.nCopies(10, GameSetup.FILLER));
        Game game = GameSetup.start(ABC, List.of(hand(), hand(), hand()), RED_FIVE, drawPile);
        game.execute(new Draw("A"));

        assertTrue(game.getVisibleStateFor("A").canPass());
        assertFalse(game.getVisibleStateFor("B").canPass());
    }

    @Test
    void anUnknownPlayerIsRejected() {
        Game game = GameSetup.start(ABC, List.of(hand(), hand(), hand()), RED_FIVE);

        assertThrows(IllegalArgumentException.class, () -> game.getVisibleStateFor("Z"));
    }
}

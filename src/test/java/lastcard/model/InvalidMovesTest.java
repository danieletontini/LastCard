package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;
import static lastcard.model.GameSetup.trimHand;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Le mosse che il modello rifiuta, e che dopo un rifiuto lo stato della partita resta com'era. */
class InvalidMovesTest {

    private static final List<String> TWO = List.of("A", "B");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    private static final Card RED_SEVEN = new NumberCard(CardColor.RED, 7);

    private static Game started() {
        return GameSetup.start(TWO, List.of(hand(RED_SEVEN), hand(RED_SEVEN)), RED_FIVE);
    }

    @Test
    void aGameNeedsBetweenTwoAndTenPlayers() {
        assertThrows(IllegalArgumentException.class, () -> new Game(List.of("A")));
        assertThrows(IllegalArgumentException.class,
                () -> new Game(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")));
    }

    @Test
    void playerNamesMustBeUnique() {
        assertThrows(IllegalArgumentException.class, () -> new Game(List.of("A", "A")));
    }

    @Test
    void movesBeforeTheStartAreRejected() {
        Game game = GameSetup.create(TWO, List.of(hand(), hand()), RED_FIVE);

        assertThrows(IllegalStateException.class, () -> game.execute(new Draw("A")));
    }

    @Test
    void aGameCannotStartTwice() {
        Game game = started();

        assertThrows(IllegalStateException.class, game::start);
    }

    @Test
    void playingOutOfTurnIsRejected() {
        Game game = started();

        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("B", 0)));
        assertEquals(7, handSize(game, "B"));
        assertEquals("A", game.getCurrentPlayer().getName());
    }

    @Test
    void drawingOutOfTurnIsRejected() {
        Game game = started();

        assertThrows(IllegalArgumentException.class, () -> game.execute(new Draw("B")));
        assertEquals(7, handSize(game, "B"));
    }

    @Test
    void anUnplayableCardIsRejectedAndStaysInHand() {
        Game game = started();

        // In posizione 1 c'è un riempitivo giallo, che sul 5 rosso non si può giocare.
        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("A", 1)));
        assertEquals(7, handSize(game, "A"));
        assertEquals(RED_FIVE, game.getVisibleState().top());
        assertEquals("A", game.getCurrentPlayer().getName());
    }

    @Test
    void aHandIndexOutOfRangeIsRejected() {
        Game game = started();

        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("A", 7)));
        assertThrows(IllegalArgumentException.class, () -> new PlayCard("A", -1));
    }

    @Test
    void choosingAColorWithoutAWildIsRejected() {
        Game game = started();

        assertThrows(IllegalStateException.class, () -> game.execute(new ChooseColor("A", CardColor.BLUE)));
        assertEquals(CardColor.RED, game.getActiveColor());
    }

    @Test
    void blackCannotBeChosenAsColor() {
        assertThrows(IllegalArgumentException.class, () -> new ChooseColor("A", CardColor.BLACK));
    }

    @Test
    void onlyWhoPlayedTheWildChoosesTheColor() {
        Game game = GameSetup.start(TWO, List.of(hand(new Wild()), hand()), RED_FIVE);
        game.execute(new PlayCard("A", 0));

        assertThrows(IllegalArgumentException.class, () -> game.execute(new ChooseColor("B", CardColor.BLUE)));
        assertEquals(GameState.AWAITING_COLOR, game.getState());
    }

    @Test
    void nothingCanBePlayedWhileWaitingForTheColor() {
        Game game = GameSetup.start(TWO, List.of(hand(new Wild(), RED_SEVEN), hand()), RED_FIVE);
        game.execute(new PlayCard("A", 0));

        assertThrows(IllegalStateException.class, () -> game.execute(new PlayCard("A", 0)));
        assertThrows(IllegalStateException.class, () -> game.execute(new Draw("A")));
    }

    @Test
    void noMoveIsAcceptedAfterTheGameIsOver() {
        Game game = started();
        trimHand(game, "A", 1);
        game.execute(new PlayCard("A", 0));

        assertEquals(GameState.FINISHED, game.getState());
        assertThrows(IllegalStateException.class, () -> game.execute(new Draw("B")));
        assertThrows(IllegalStateException.class, () -> game.execute(new PlayCard("B", 0)));
    }
}

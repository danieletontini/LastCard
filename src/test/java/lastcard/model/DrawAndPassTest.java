package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static lastcard.model.GameSetup.FILLER;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** La pesca: carta non giocabile, carta giocabile da giocare o tenere, mosse non permesse, mazzo esaurito. */
class DrawAndPassTest {

    private static final List<String> TWO = List.of("A", "B");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    private static final Card RED_SEVEN = new NumberCard(CardColor.RED, 7);

    private static Game gameWhereADraws(Card drawn) {
        return GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE, List.of(drawn, FILLER, FILLER));
    }

    @Test
    void drawingAnUnplayableCardPassesTheTurn() {
        Game game = gameWhereADraws(FILLER);

        game.execute(new Draw("A"));

        assertEquals(8, handSize(game, "A"));
        assertEquals("B", game.getCurrentPlayer().getName());
        assertFalse(game.getVisibleState().canPass());
    }

    @Test
    void drawingAPlayableCardKeepsTheTurn() {
        Game game = gameWhereADraws(RED_SEVEN);

        game.execute(new Draw("A"));

        assertEquals("A", game.getCurrentPlayer().getName());
        assertTrue(game.getVisibleState().canPass());
        assertEquals(RED_SEVEN, game.getVisibleState().currentHand().get(7));
    }

    @Test
    void theDrawnCardCanBePlayedRightAway() {
        Game game = gameWhereADraws(RED_SEVEN);

        game.execute(new Draw("A"));
        game.execute(new PlayCard("A", 7));

        assertEquals(RED_SEVEN, game.getVisibleState().top());
        assertEquals(7, handSize(game, "A"));
        assertEquals("B", game.getCurrentPlayer().getName());
        assertFalse(game.getVisibleState().canPass());
    }

    @Test
    void passingKeepsTheDrawnCardAndEndsTheTurn() {
        Game game = gameWhereADraws(RED_SEVEN);
        List<GameEvent> events = new ArrayList<>();
        game.addObserver(events::add);

        game.execute(new Draw("A"));
        game.execute(new Pass("A"));

        assertEquals(8, handSize(game, "A"));
        assertEquals("B", game.getCurrentPlayer().getName());
        assertEquals(List.of(GameEvent.Type.CARDS_DRAWN, GameEvent.Type.TURN_PASSED),
                events.stream().map(GameEvent::type).toList());
    }

    @Test
    void afterDrawingOnlyTheDrawnCardCanBePlayed() {
        Card redThree = new NumberCard(CardColor.RED, 3);
        Game game = GameSetup.start(TWO, List.of(hand(redThree), hand()), RED_FIVE, List.of(RED_SEVEN));

        game.execute(new Draw("A"));

        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("A", 0)));
        assertEquals("A", game.getCurrentPlayer().getName());
    }

    @Test
    void drawingTwiceInTheSameTurnIsRejected() {
        Game game = gameWhereADraws(RED_SEVEN);

        game.execute(new Draw("A"));

        assertThrows(IllegalStateException.class, () -> game.execute(new Draw("A")));
        assertEquals(8, handSize(game, "A"));
    }

    @Test
    void passingWithoutDrawingIsRejected() {
        Game game = gameWhereADraws(RED_SEVEN);

        assertThrows(IllegalStateException.class, () -> game.execute(new Pass("A")));
        assertEquals("A", game.getCurrentPlayer().getName());
    }

    @Test
    void passingAfterAnUnplayableDrawIsRejected() {
        Game game = gameWhereADraws(FILLER);

        game.execute(new Draw("A"));

        assertThrows(IllegalStateException.class, () -> game.execute(new Pass("B")));
    }

    @Test
    void drawingFromAnExhaustedDeckDrawsNothingAndPassesTheTurn() {
        // Nessuna carta da pescare e negli scarti solo la cima: non c'è niente da rimescolare.
        Game game = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE, List.of());
        List<GameEvent> events = new ArrayList<>();
        game.addObserver(events::add);

        game.execute(new Draw("A"));

        assertEquals(7, handSize(game, "A"));
        assertEquals("B", game.getCurrentPlayer().getName());
        assertEquals(0, events.get(0).count());
    }

    @Test
    void anExhaustedDeckIsRefilledFromTheDiscardPile() {
        Card redThree = new NumberCard(CardColor.RED, 3);
        Game game = GameSetup.start(TWO, List.of(hand(redThree), hand()), RED_FIVE, List.of());

        game.execute(new PlayCard("A", 0));     // negli scarti ora ci sono il 5 e, in cima, il 3
        game.execute(new Draw("B"));

        assertEquals(8, handSize(game, "B"));
        assertEquals(redThree, game.getVisibleState().top());
        assertEquals(15, game.totalCardCount());
    }
}

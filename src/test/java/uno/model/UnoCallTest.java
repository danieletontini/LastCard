package uno.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uno.model.GameSetup.hand;
import static uno.model.GameSetup.handSize;
import static uno.model.GameSetup.trimHand;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La chiamata di UNO: quando si può fare, la penalità per chi se ne dimentica, la vittoria con l'ultima carta. */
class UnoCallTest {

    private static final List<String> TWO = List.of("A", "B");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    private static final Card RED_SEVEN = new NumberCard(CardColor.RED, 7);

    private List<GameEvent> events;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
    }

    /** A ha in mano card più un riempitivo: giocando card resta con una carta. */
    private Game gameWithTwoCardsFor(Card card) {
        Game game = GameSetup.start(TWO, List.of(hand(card), hand()), RED_FIVE);
        trimHand(game, "A", 2);
        game.addObserver(events::add);
        return game;
    }

    private List<GameEvent.Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @Test
    void callingUnoBeforeThePenultimateCardAvoidsThePenalty() {
        Game game = gameWithTwoCardsFor(RED_SEVEN);

        game.execute(new CallUno("A"));
        game.execute(new PlayCard("A", 0));

        assertEquals(1, handSize(game, "A"));
        assertEquals(List.of(GameEvent.Type.UNO_CALLED, GameEvent.Type.CARD_PLAYED), types());
    }

    @Test
    void forgettingUnoCostsTwoCards() {
        Game game = gameWithTwoCardsFor(RED_SEVEN);

        game.execute(new PlayCard("A", 0));

        assertEquals(3, handSize(game, "A"));
        GameEvent penalty = events.get(events.size() - 1);
        assertEquals(GameEvent.Type.UNO_PENALTY, penalty.type());
        assertEquals("A", penalty.playerName());
        assertEquals(2, penalty.count());
    }

    @Test
    void unoCanBeCalledWhileChoosingTheColorAfterAWild() {
        Game game = gameWithTwoCardsFor(new Wild());

        game.execute(new PlayCard("A", 0));
        game.execute(new CallUno("A"));
        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(1, handSize(game, "A"));
        assertFalse(types().contains(GameEvent.Type.UNO_PENALTY));
    }

    @Test
    void forgettingUnoAfterAWildIsPenalizedOnceTheColorIsChosen() {
        Game game = gameWithTwoCardsFor(new Wild());

        game.execute(new PlayCard("A", 0));
        assertEquals(1, handSize(game, "A"));

        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(3, handSize(game, "A"));
        assertEquals(GameEvent.Type.UNO_PENALTY, events.get(events.size() - 1).type());
    }

    @Test
    void theUnoCallDoesNotCarryOverToTheNextTurn() {
        // A chiama UNO ma poi pesca invece di giocare: la chiamata si perde con la fine del turno.
        Game game = gameWithTwoCardsFor(RED_SEVEN);

        game.execute(new CallUno("A"));
        game.execute(new Draw("A"));              // pesca un riempitivo: il turno passa a B
        game.execute(new Draw("B"));              // anche B pesca un riempitivo: torna ad A

        assertFalse(GameSetup.player(game, "A").hasCalledUno());
    }

    @Test
    void unoWithMoreThanTwoCardsIsRejected() {
        Game game = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE);

        assertThrows(IllegalStateException.class, () -> game.execute(new CallUno("A")));
    }

    @Test
    void unoOutOfTurnIsRejected() {
        Game game = gameWithTwoCardsFor(RED_SEVEN);
        trimHand(game, "B", 2);

        assertThrows(IllegalArgumentException.class, () -> game.execute(new CallUno("B")));
    }

    @Test
    void playingTheLastCardWinsWithoutPenalty() {
        Game game = GameSetup.start(TWO, List.of(hand(RED_SEVEN), hand()), RED_FIVE);
        trimHand(game, "A", 1);
        game.addObserver(events::add);

        game.execute(new PlayCard("A", 0));

        assertEquals(GameState.FINISHED, game.getState());
        assertEquals("A", game.getWinner().orElseThrow().getName());
        assertEquals(List.of(GameEvent.Type.CARD_PLAYED, GameEvent.Type.GAME_WON), types());
    }
}

package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;
import static lastcard.model.GameSetup.trimHand;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La dichiarazione di Ultima!: quando si può fare, la penalità per chi se ne dimentica, la vittoria con l'ultima carta. */
class LastCardCallTest {

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
    void callingLastCardBeforeThePenultimateCardAvoidsThePenalty() {
        Game game = gameWithTwoCardsFor(RED_SEVEN);

        game.execute(new CallLastCard("A"));
        game.execute(new PlayCard("A", 0));

        assertEquals(1, handSize(game, "A"));
        assertEquals(List.of(GameEvent.Type.LAST_CARD_CALLED, GameEvent.Type.CARD_PLAYED), types());
    }

    @Test
    void forgettingLastCardCostsTwoCards() {
        Game game = gameWithTwoCardsFor(RED_SEVEN);

        game.execute(new PlayCard("A", 0));

        assertEquals(3, handSize(game, "A"));
        GameEvent penalty = events.get(events.size() - 1);
        assertEquals(GameEvent.Type.LAST_CARD_PENALTY, penalty.type());
        assertEquals("A", penalty.playerName());
        assertEquals(2, penalty.count());
    }

    @Test
    void lastCardCanBeCalledWhileChoosingTheColorAfterAWild() {
        Game game = gameWithTwoCardsFor(new Wild());

        game.execute(new PlayCard("A", 0));
        game.execute(new CallLastCard("A"));
        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(1, handSize(game, "A"));
        assertFalse(types().contains(GameEvent.Type.LAST_CARD_PENALTY));
    }

    @Test
    void forgettingLastCardAfterAWildIsPenalizedOnceTheColorIsChosen() {
        Game game = gameWithTwoCardsFor(new Wild());

        game.execute(new PlayCard("A", 0));
        assertEquals(1, handSize(game, "A"));

        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(3, handSize(game, "A"));
        assertEquals(GameEvent.Type.LAST_CARD_PENALTY, events.get(events.size() - 1).type());
    }

    @Test
    void theLastCardCallDoesNotCarryOverToTheNextTurn() {
        // A dichiara Ultima! ma poi pesca invece di giocare: la chiamata si perde con la fine del turno.
        Game game = gameWithTwoCardsFor(RED_SEVEN);

        game.execute(new CallLastCard("A"));
        game.execute(new Draw("A"));              // pesca un riempitivo: il turno passa a B
        game.execute(new Draw("B"));              // anche B pesca un riempitivo: torna ad A

        assertFalse(GameSetup.player(game, "A").hasCalledLastCard());
    }

    @Test
    void lastCardWithMoreThanTwoCardsIsRejected() {
        Game game = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE);

        assertThrows(IllegalStateException.class, () -> game.execute(new CallLastCard("A")));
    }

    @Test
    void lastCardOutOfTurnIsRejected() {
        Game game = gameWithTwoCardsFor(RED_SEVEN);
        trimHand(game, "B", 2);

        assertThrows(IllegalArgumentException.class, () -> game.execute(new CallLastCard("B")));
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

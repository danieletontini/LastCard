package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static lastcard.model.GameSetup.FILLER;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Chi gioca dopo chi: senso di marcia, carte azione, jolly e prima carta scoperta. A è il primo, l'ultimo è il mazziere. */
class TurnRulesTest {

    private static final List<String> TWO = List.of("A", "B");
    private static final List<String> THREE = List.of("A", "B", "C");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);

    @Test
    void firstPlayerStartsAfterANumberCard() {
        Game game = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE);

        assertEquals("A", game.getCurrentPlayer().getName());
        assertEquals(CardColor.RED, game.getActiveColor());
    }

    @Test
    void reverseWithTwoPlayersActsAsSkip() {
        Game game = GameSetup.start(TWO, List.of(hand(new Reverse(CardColor.RED)), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));

        assertEquals("A", game.getCurrentPlayer().getName());
    }

    @Test
    void reverseWithThreePlayersChangesDirection() {
        Game game = GameSetup.start(THREE, List.of(hand(new Reverse(CardColor.RED)), hand(), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));

        assertEquals(Direction.COUNTERCLOCKWISE, game.getDirection());
        assertEquals("C", game.getCurrentPlayer().getName());
    }

    @Test
    void skipJumpsOverTheNextPlayer() {
        Game game = GameSetup.start(THREE, List.of(hand(new Skip(CardColor.RED)), hand(), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));

        assertEquals("C", game.getCurrentPlayer().getName());
    }

    @Test
    void drawTwoMakesTheNextPlayerDrawAndLoseTheTurn() {
        Game game = GameSetup.start(THREE, List.of(hand(new DrawTwo(CardColor.RED)), hand(), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));

        assertEquals(9, handSize(game, "B"));
        assertEquals("C", game.getCurrentPlayer().getName());
    }

    @Test
    void wildDrawFourPenalizesTheNextAndLetsThePlayerChooseTheColor() {
        Game game = GameSetup.start(THREE, List.of(hand(new WildDrawFour()), hand(), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));

        assertEquals(GameState.AWAITING_COLOR, game.getState());
        assertEquals("A", game.getVisibleState().currentPlayerName());
        assertEquals(11, handSize(game, "B"));

        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(CardColor.BLUE, game.getActiveColor());
        assertEquals("C", game.getCurrentPlayer().getName());
    }

    @Test
    void wildLetsThePlayerChooseAndThenPassesTheTurn() {
        Game game = GameSetup.start(THREE, List.of(hand(new Wild()), hand(), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));
        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(CardColor.BLUE, game.getActiveColor());
        assertEquals("B", game.getCurrentPlayer().getName());
    }

    @Test
    void initialWildIsChosenByTheFirstPlayerWhoThenPlays() {
        Game game = GameSetup.start(TWO, List.of(hand(), hand()), new Wild());

        assertEquals(GameState.AWAITING_COLOR, game.getState());
        assertEquals("A", game.getVisibleState().currentPlayerName());

        game.execute(new ChooseColor("A", CardColor.RED));

        assertEquals("A", game.getCurrentPlayer().getName());
        assertEquals(7, handSize(game, "A"));
    }

    @Test
    void initialWildDrawFourPenalizesTheFirstPlayerAndTheDealerChooses() {
        Game game = GameSetup.start(THREE, List.of(hand(), hand(), hand()), new WildDrawFour());

        assertEquals(11, handSize(game, "A"));
        assertEquals("C", game.getVisibleState().currentPlayerName());

        game.execute(new ChooseColor("C", CardColor.RED));

        assertEquals("B", game.getCurrentPlayer().getName());
    }

    @Test
    void initialSkipSkipsTheFirstPlayer() {
        Game game = GameSetup.start(THREE, List.of(hand(), hand(), hand()), new Skip(CardColor.RED));

        assertEquals("B", game.getCurrentPlayer().getName());
    }

    @Test
    void initialDrawTwoPenalizesTheFirstPlayer() {
        Game game = GameSetup.start(THREE, List.of(hand(), hand(), hand()), new DrawTwo(CardColor.RED));

        assertEquals(9, handSize(game, "A"));
        assertEquals("B", game.getCurrentPlayer().getName());
    }

    @Test
    void flipDirectionWithTwoPlayersLetsThePenaltyHitTheOpponent() {
        // La sequenza del Jolly Cambia Giro Pesca 4: con reverseDirection la penalità tornerebbe a chi gioca.
        Game game = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE);

        game.flipDirection();
        game.penalizeNext(4);

        assertEquals(Direction.COUNTERCLOCKWISE, game.getDirection());
        assertEquals(11, handSize(game, "B"));
        assertEquals(7, handSize(game, "A"));
    }

    @Test
    void aGameWithoutExplicitRulesUsesTheClassicOnes() {
        assertEquals(GameRules.classic(), new Game(TWO).getRules());
    }

    @Test
    void fillerCardsAreNeverPlayableOnRed() {
        // Garanzia per gli altri test: i riempitivi non cambiano lo scenario.
        assertEquals(false, FILLER.isPlayable(RED_FIVE, CardColor.RED));
    }
}

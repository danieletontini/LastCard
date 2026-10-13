package lastcard.model;

import static lastcard.model.GameSetup.FILLER;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;
import static lastcard.model.GameSetup.player;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le regole No Mercy di Game, una per test: accumulo, eliminazione, pesca finché giocabile, Salta Tutti,
 * Scarta Tutto, Roulette, scambio 7, rotazione 0. La prima carta è un rosso 5, quindi comincia A.
 */
class NoMercyRulesTest {

    private static final List<String> ABC = List.of("A", "B", "C");
    private static final List<String> AB = List.of("A", "B");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);

    private static Game start(List<String> names, List<List<Card>> hands) {
        return GameSetup.start(GameRules.noMercy(), names, hands, RED_FIVE);
    }

    private static Game start(List<String> names, List<List<Card>> hands, List<Card> drawPile) {
        return GameSetup.start(GameRules.noMercy(), names, hands, RED_FIVE, drawPile);
    }

    private static String current(Game game) {
        return game.getCurrentPlayer().getName();
    }

    // ------------------------------------------------------------------
    // accumulo delle pescate
    // ------------------------------------------------------------------

    @Test
    void drawPenaltiesStackAndWhoCannotAnswerDrawsTheTotal() {
        Game game = start(ABC, List.of(
                hand(new DrawTwo(CardColor.RED)),
                hand(new ColorDrawFour(CardColor.BLUE)),
                hand()));

        game.execute(new PlayCard("A", 0));
        assertEquals(2, game.getVisibleState().pendingDraw());
        assertEquals("B", current(game), "con l'accumulo chi riceve la penalità non salta subito il turno");

        // un Pesca 4 di un altro colore rilancia un +2: conta il valore, non il colore
        game.execute(new PlayCard("B", 0));
        assertEquals(6, game.getVisibleState().pendingDraw());
        assertEquals("C", current(game));

        game.execute(new Draw("C"));
        assertEquals(7 + 6, handSize(game, "C"));
        assertEquals(0, game.getVisibleState().pendingDraw());
        assertEquals("A", current(game), "chi pesca il totale perde il turno");
    }

    @Test
    void theStackingEventNamesWhoPlayedTheDrawCard() {
        Game game = GameSetup.create(GameRules.noMercy(), ABC, List.of(
                hand(new DrawTwo(CardColor.RED)), hand(), hand()), RED_FIVE, Collections.nCopies(30, FILLER));
        List<GameEvent> events = new ArrayList<>();
        game.addObserver(events::add);
        game.start();

        game.execute(new PlayCard("A", 0));

        GameEvent stacked = events.stream()
                .filter(event -> event.type() == GameEvent.Type.PENALTY_STACKED)
                .findFirst()
                .orElseThrow();
        assertEquals("A", stacked.playerName());
        assertEquals(2, stacked.count());
    }

    @Test
    void withAPendingPenaltyOnlyEqualOrHigherDrawCardsCanBePlayed() {
        Game game = start(ABC, List.of(
                hand(new ColorDrawFour(CardColor.RED)),
                hand(new DrawTwo(CardColor.RED), new NumberCard(CardColor.RED, 5)),
                hand()));
        game.execute(new PlayCard("A", 0));

        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("B", 0)),
                "un +2 non rilancia un +4");
        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("B", 1)),
                "con una penalità in corso le carte normali sono bloccate");
        assertThrows(IllegalStateException.class, () -> game.execute(new Pass("B")));
        assertEquals(4, game.getVisibleState().pendingDraw());
    }

    // ------------------------------------------------------------------
    // eliminazione a 25 carte
    // ------------------------------------------------------------------

    @Test
    void reachingTwentyFiveCardsEliminatesThePlayerWhoIsThenLeftOutOfTheTurns() {
        Game game = start(ABC, List.of(
                hand(new WildDrawTen()),
                hand(new WildDrawTen()),
                hand()));
        int cards = game.totalCardCount();

        game.execute(new PlayCard("A", 0));
        game.execute(new ChooseColor("A", CardColor.YELLOW));
        game.execute(new PlayCard("B", 0));
        game.execute(new ChooseColor("B", CardColor.YELLOW));
        game.execute(new Draw("C"));

        Player c = player(game, "C");
        assertTrue(c.isEliminated(), "7 + 20 carte superano la soglia di 25");
        assertTrue(c.getHand().isEmpty(), "le carte dell'eliminato tornano negli scarti");
        assertEquals(cards, game.totalCardCount());
        assertTrue(game.getVisibleState().players().get(2).eliminated());
        assertEquals(GameState.IN_PROGRESS, game.getState());

        assertEquals("A", current(game));
        game.execute(new PlayCard("A", 0));
        assertEquals("B", current(game));
        game.execute(new PlayCard("B", 0));
        assertEquals("A", current(game), "C è fuori dal giro");
    }

    @Test
    void theLastPlayerStandingWinsTheHand() {
        Game game = start(AB, List.of(hand(new WildDrawTen()), hand(new WildDrawTen())));

        game.execute(new PlayCard("A", 0));
        game.execute(new ChooseColor("A", CardColor.YELLOW));
        game.execute(new PlayCard("B", 0));
        game.execute(new ChooseColor("B", CardColor.YELLOW));
        game.execute(new Draw("A"));

        assertTrue(player(game, "A").isEliminated());
        assertEquals(GameState.FINISHED, game.getState());
        assertEquals("B", game.getWinner().orElseThrow().getName());
        assertFalse(player(game, "A").hasWon(), "una mano vuota per eliminazione non è una vittoria");
    }

    // ------------------------------------------------------------------
    // pesca finché giocabile
    // ------------------------------------------------------------------

    @Test
    void drawingContinuesUntilAPlayableCardComesOut() {
        List<Card> drawPile = new ArrayList<>(List.of(FILLER, FILLER, FILLER, new NumberCard(CardColor.RED, 2)));
        drawPile.addAll(Collections.nCopies(10, FILLER));
        Game game = start(AB, List.of(hand(), hand()), drawPile);

        game.execute(new Draw("A"));

        assertEquals(7 + 4, handSize(game, "A"));
        assertEquals("A", current(game), "la carta giocabile si può giocare subito");
        assertTrue(game.getVisibleState().canPass());
        game.execute(new PlayCard("A", 10));
        assertEquals("B", current(game));
    }

    @Test
    void classicRulesStillDrawASingleCard() {
        List<Card> drawPile = new ArrayList<>(List.of(FILLER, new NumberCard(CardColor.RED, 2)));
        drawPile.addAll(Collections.nCopies(10, FILLER));
        Game game = GameSetup.start(GameRules.classic(), AB, List.of(hand(), hand()), RED_FIVE, drawPile);

        game.execute(new Draw("A"));

        assertEquals(8, handSize(game, "A"));
        assertEquals("B", current(game));
    }

    // ------------------------------------------------------------------
    // carte azione No Mercy
    // ------------------------------------------------------------------

    @Test
    void skipEveryoneGivesTheTurnBackToWhoPlayedIt() {
        Game game = start(ABC, List.of(hand(new SkipEveryone(CardColor.RED)), hand(), hand()));

        game.execute(new PlayCard("A", 0));

        assertEquals("A", current(game));
    }

    @Test
    void discardAllThrowsAwayEveryCardOfThatColorAndStaysOnTop() {
        Game game = start(ABC, List.of(
                hand(new DiscardAll(CardColor.RED), new NumberCard(CardColor.RED, 1),
                        new NumberCard(CardColor.RED, 3), new NumberCard(CardColor.BLUE, 4)),
                hand(), hand()));
        int cards = game.totalCardCount();

        game.execute(new PlayCard("A", 0));

        assertEquals(7 - 3, handSize(game, "A"));
        assertTrue(player(game, "A").getHand().contains(new NumberCard(CardColor.BLUE, 4)));
        assertEquals(new DiscardAll(CardColor.RED), game.getVisibleState().top());
        assertEquals(CardColor.RED, game.getActiveColor());
        assertEquals("B", current(game));
        assertEquals(cards, game.totalCardCount());
    }

    @Test
    void discardingTheLastCardsWithDiscardAllWinsTheHand() {
        List<Card> reds = new ArrayList<>(List.of(new DiscardAll(CardColor.RED)));
        for (int number = 0; number < 6; number++) {
            reds.add(new NumberCard(CardColor.RED, number));
        }
        Game game = start(ABC, List.of(reds, hand(), hand()));

        game.execute(new PlayCard("A", 0));

        assertEquals(GameState.FINISHED, game.getState());
        assertEquals("A", game.getWinner().orElseThrow().getName());
    }

    @Test
    void colorRouletteMakesTheNextPlayerRevealCardsUntilTheChosenColor() {
        List<Card> drawPile = new ArrayList<>(List.of(FILLER, new WildDrawSix(), new NumberCard(CardColor.BLUE, 8)));
        drawPile.addAll(Collections.nCopies(10, FILLER));
        Game game = start(ABC, List.of(hand(new WildColorRoulette()), hand(), hand()), drawPile);

        game.execute(new PlayCard("A", 0));
        assertEquals(GameState.AWAITING_ROULETTE_COLOR, game.getState());
        assertEquals("B", game.getVisibleState().currentPlayerName(), "il colore lo sceglie chi subisce la Roulette");
        assertThrows(IllegalArgumentException.class, () -> game.execute(new ChooseColor("A", CardColor.BLUE)));

        game.execute(new ChooseColor("B", CardColor.BLUE));

        assertEquals(7 + 3, handSize(game, "B"), "il jolly scoperto non conta come blu");
        assertEquals(CardColor.BLUE, game.getActiveColor());
        assertEquals("C", current(game), "chi subisce la Roulette perde il turno");
    }

    // ------------------------------------------------------------------
    // numeri: scambio 7 e rotazione 0
    // ------------------------------------------------------------------

    @Test
    void playingASevenSwapsHandsWithTheChosenOpponent() {
        Card marker = new NumberCard(CardColor.BLUE, 1);
        Game game = start(ABC, List.of(hand(new NumberCard(CardColor.RED, 7)), hand(), hand(marker)));

        game.execute(new PlayCard("A", 0));
        assertEquals(GameState.AWAITING_SWAP_TARGET, game.getState());
        assertEquals("A", game.getVisibleState().currentPlayerName());

        game.execute(new SwapHands("A", "C"));

        assertEquals(7, handSize(game, "A"));
        assertTrue(player(game, "A").getHand().contains(marker));
        assertEquals(6, handSize(game, "C"));
        assertEquals("B", current(game));
    }

    @Test
    void playingAZeroPassesEveryHandToTheNextPlayer() {
        Card fromA = new NumberCard(CardColor.BLUE, 1);
        Card fromB = new NumberCard(CardColor.BLUE, 2);
        Card fromC = new NumberCard(CardColor.BLUE, 3);
        Game game = start(ABC, List.of(hand(new NumberCard(CardColor.RED, 0), fromA), hand(fromB), hand(fromC)));

        game.execute(new PlayCard("A", 0));

        assertTrue(player(game, "B").getHand().contains(fromA));
        assertTrue(player(game, "C").getHand().contains(fromB));
        assertTrue(player(game, "A").getHand().contains(fromC));
        assertEquals("B", current(game));
    }

    @Test
    void classicRulesDoNotSwapOnSeven() {
        Game game = GameSetup.start(GameRules.classic(), ABC,
                List.of(hand(new NumberCard(CardColor.RED, 7)), hand(), hand()), RED_FIVE);

        game.execute(new PlayCard("A", 0));

        assertEquals(GameState.IN_PROGRESS, game.getState());
        assertEquals("B", current(game));
    }
}

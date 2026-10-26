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
 * Le regole No Mercy di Game: accumulo, eliminazione, pesca finché giocabile, Salta Tutti, Scarta Tutto, Roulette,
 * scambio 7, rotazione 0. La prima carta è un rosso 5 dove non è detto altro, quindi comincia A.
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

    @Test
    void drawPenaltiesStackOnlyWithEqualOrHigherDrawCards() {
        Game game = start(ABC, List.of(hand(new DrawTwo(CardColor.RED)), hand(new ColorDrawFour(CardColor.BLUE)), hand()));
        game.execute(new PlayCard("A", 0));
        assertEquals(2, game.getVisibleState().pendingDraw());
        assertEquals("B", current(game), "con l'accumulo chi riceve la penalità non salta subito il turno");

        // un Pesca 4 di un altro colore rilancia un +2: conta il valore, non il colore
        game.execute(new PlayCard("B", 0));
        assertEquals(6, game.getVisibleState().pendingDraw());

        game.execute(new Draw("C"));
        assertEquals(7 + 6, handSize(game, "C"), "chi non può rispondere pesca il totale");
        assertEquals(0, game.getVisibleState().pendingDraw());
        assertEquals("A", current(game), "e perde il turno");

        Game blocked = start(ABC, List.of(hand(new ColorDrawFour(CardColor.RED)),
                hand(new DrawTwo(CardColor.RED), new NumberCard(CardColor.RED, 5)), hand()));
        blocked.execute(new PlayCard("A", 0));
        assertThrows(IllegalArgumentException.class, () -> blocked.execute(new PlayCard("B", 0)), "un +2 non rilancia un +4");
        assertThrows(IllegalArgumentException.class, () -> blocked.execute(new PlayCard("B", 1)),
                "con una penalità in corso le carte normali sono bloccate");
        assertEquals(4, blocked.getVisibleState().pendingDraw());
    }

    @Test
    void aReverseDrawFourAsFirstCardIsAnsweredByWhoChoosesTheColor() {
        // Il mazziere è C. La carta inverte il giro: il primo a giocare è B, non A, ed è B a scegliere il colore
        // e poi a dover rispondere alla penalità.
        Game game = GameSetup.start(GameRules.noMercy(), ABC, List.of(hand(), hand(), hand()), new WildReverseDrawFour());

        assertEquals(GameState.AWAITING_COLOR, game.getState());
        assertEquals("B", game.getVisibleState().currentPlayerName(), "sceglie il colore il primo dopo il mazziere");

        game.execute(new ChooseColor("B", CardColor.BLUE));

        assertEquals("B", current(game), "e risponde alla penalità lo stesso giocatore");
        assertEquals(4, game.getVisibleState().pendingDraw());
    }

    @Test
    void twentyFiveCardsEliminateAPlayerAndTheLastOneStandingWins() {
        Game game = start(ABC, List.of(hand(new WildDrawTen()), hand(new WildDrawTen()), hand()));
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
        assertEquals(GameState.IN_PROGRESS, game.getState());
        game.execute(new PlayCard("A", 0));
        game.execute(new PlayCard("B", 0));
        assertEquals("A", current(game), "C è fuori dal giro");

        Game duel = start(AB, List.of(hand(new WildDrawTen()), hand(new WildDrawTen())));
        duel.execute(new PlayCard("A", 0));
        duel.execute(new ChooseColor("A", CardColor.YELLOW));
        duel.execute(new PlayCard("B", 0));
        duel.execute(new ChooseColor("B", CardColor.YELLOW));
        duel.execute(new Draw("A"));
        assertEquals(GameState.FINISHED, duel.getState(), "resta solo B");
        assertEquals("B", duel.getWinner().orElseThrow().getName());
        assertFalse(player(duel, "A").hasWon(), "una mano vuota per eliminazione non è una vittoria");
    }

    @Test
    void withoutAPlayableCardYouKeepDrawing() {
        List<Card> drawPile = new ArrayList<>(List.of(FILLER, FILLER, FILLER, new NumberCard(CardColor.RED, 2)));
        drawPile.addAll(Collections.nCopies(10, FILLER));
        Game game = start(AB, List.of(hand(), hand()), drawPile);

        game.execute(new Draw("A"));

        assertEquals(7 + 4, handSize(game, "A"));
        assertEquals("A", current(game), "la carta giocabile si può giocare subito");
        game.execute(new PlayCard("A", 10));
        assertEquals("B", current(game));
    }

    @Test
    void skipEveryoneDiscardAllAndTheColorRoulette() {
        Game skip = start(ABC, List.of(hand(new SkipEveryone(CardColor.RED)), hand(), hand()));
        skip.execute(new PlayCard("A", 0));
        assertEquals("A", current(skip), "Salta Tutti: rigioca chi l'ha giocata");

        Game discard = start(ABC, List.of(hand(new DiscardAll(CardColor.RED), new NumberCard(CardColor.RED, 1),
                new NumberCard(CardColor.RED, 3), new NumberCard(CardColor.BLUE, 4)), hand(), hand()));
        int cards = discard.totalCardCount();
        discard.execute(new PlayCard("A", 0));
        assertEquals(7 - 3, handSize(discard, "A"), "Scarta Tutto si porta via le altre rosse");
        assertTrue(player(discard, "A").getHand().contains(new NumberCard(CardColor.BLUE, 4)));
        assertEquals(new DiscardAll(CardColor.RED), discard.getVisibleState().top(), "e resta in cima");
        assertEquals(cards, discard.totalCardCount());

        List<Card> reds = new ArrayList<>(List.of(new DiscardAll(CardColor.RED)));
        for (int number = 0; number < 6; number++) {
            reds.add(new NumberCard(CardColor.RED, number));
        }
        Game emptied = start(ABC, List.of(reds, hand(), hand()));
        emptied.execute(new PlayCard("A", 0));
        assertEquals("A", emptied.getWinner().orElseThrow().getName(), "svuotare la mano con Scarta Tutto vince");

        List<Card> drawPile = new ArrayList<>(List.of(FILLER, new WildDrawSix(), new NumberCard(CardColor.BLUE, 8)));
        drawPile.addAll(Collections.nCopies(10, FILLER));
        Game roulette = start(ABC, List.of(hand(new WildColorRoulette()), hand(), hand()), drawPile);
        roulette.execute(new PlayCard("A", 0));
        assertEquals(GameState.AWAITING_ROULETTE_COLOR, roulette.getState());
        assertEquals("B", roulette.getVisibleState().currentPlayerName(), "il colore lo sceglie chi subisce la Roulette");
        roulette.execute(new ChooseColor("B", CardColor.BLUE));
        assertEquals(7 + 3, handSize(roulette, "B"), "scopre finché esce il blu; il jolly non conta come blu");
        assertEquals("C", current(roulette), "e perde il turno");
    }

    @Test
    void sevenSwapsAndZeroRotatesHandsOnlyInNoMercy() {
        Card marker = new NumberCard(CardColor.BLUE, 1);
        Game seven = start(ABC, List.of(hand(new NumberCard(CardColor.RED, 7)), hand(), hand(marker)));
        seven.execute(new PlayCard("A", 0));
        assertEquals(GameState.AWAITING_SWAP_TARGET, seven.getState());
        seven.execute(new SwapHands("A", "C"));
        assertTrue(player(seven, "A").getHand().contains(marker), "A ha preso la mano di C");
        assertEquals(6, handSize(seven, "C"));
        assertEquals("B", current(seven));

        Card fromA = new NumberCard(CardColor.BLUE, 1);
        Card fromB = new NumberCard(CardColor.BLUE, 2);
        Card fromC = new NumberCard(CardColor.BLUE, 3);
        Game zero = start(ABC, List.of(hand(new NumberCard(CardColor.RED, 0), fromA), hand(fromB), hand(fromC)));
        zero.execute(new PlayCard("A", 0));
        assertTrue(player(zero, "B").getHand().contains(fromA), "ogni mano passa al successivo");
        assertTrue(player(zero, "C").getHand().contains(fromB));
        assertTrue(player(zero, "A").getHand().contains(fromC));

        Game classic = GameSetup.start(GameRules.classic(), ABC,
                List.of(hand(new NumberCard(CardColor.RED, 7)), hand(), hand()), RED_FIVE);
        classic.execute(new PlayCard("A", 0));
        assertEquals(GameState.IN_PROGRESS, classic.getState(), "nella classica il 7 è un numero come gli altri");
        assertEquals("B", current(classic));
    }
}

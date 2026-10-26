package lastcard.model;

import static lastcard.model.GameSetup.FILLER;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;
import static lastcard.model.GameSetup.trimHand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le regole classiche di Game, con partite dalle carte decise dal test (GameSetup): chi gioca dopo chi, carte
 * azione e jolly, prima carta scoperta, pesca e passo, Ultima!, mosse vietate, eventi per le viste.
 * A è il primo a giocare, l'ultimo giocatore è il mazziere; la prima carta è un 5 rosso dove non è detto altro.
 */
class ClassicRulesTest {

    private static final List<String> TWO = List.of("A", "B");
    private static final List<String> THREE = List.of("A", "B", "C");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    private static final Card RED_SEVEN = new NumberCard(CardColor.RED, 7);
    private static final Card RED_THREE = new NumberCard(CardColor.RED, 3);

    private static String current(Game game) {
        return game.getCurrentPlayer().getName();
    }

    private static List<GameEvent.Type> types(List<GameEvent> events) {
        return events.stream().map(GameEvent::type).toList();
    }

    @Test
    void actionCardsChangeWhoPlaysNext() {
        Game number = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE);
        assertEquals("A", current(number), "dopo una carta numero comincia il primo");
        assertEquals(CardColor.RED, number.getActiveColor());

        Game skip = GameSetup.start(THREE, List.of(hand(new Skip(CardColor.RED)), hand(), hand()), RED_FIVE);
        skip.execute(new PlayCard("A", 0));
        assertEquals("C", current(skip), "Salta fa perdere il turno a B");

        Game reverse = GameSetup.start(THREE, List.of(hand(new Reverse(CardColor.RED)), hand(), hand()), RED_FIVE);
        reverse.execute(new PlayCard("A", 0));
        assertEquals(Direction.COUNTERCLOCKWISE, reverse.getDirection());
        assertEquals("C", current(reverse), "Inverti: il giro torna indietro");

        Game reverseInTwo = GameSetup.start(TWO, List.of(hand(new Reverse(CardColor.RED)), hand()), RED_FIVE);
        reverseInTwo.execute(new PlayCard("A", 0));
        assertEquals("A", current(reverseInTwo), "in due Inverti vale come Salta");
    }

    @Test
    void drawCardsMakeTheNextPlayerDrawAndLoseTheTurn() {
        Game drawTwo = GameSetup.start(THREE, List.of(hand(new DrawTwo(CardColor.RED)), hand(), hand()), RED_FIVE);
        drawTwo.execute(new PlayCard("A", 0));
        assertEquals(9, handSize(drawTwo, "B"));
        assertEquals("C", current(drawTwo));

        Game drawFour = GameSetup.start(THREE, List.of(hand(new WildDrawFour()), hand(), hand()), RED_FIVE);
        drawFour.execute(new PlayCard("A", 0));
        assertEquals(GameState.AWAITING_COLOR, drawFour.getState());
        assertEquals("A", drawFour.getVisibleState().currentPlayerName(), "il colore lo sceglie chi ha giocato il +4");
        assertEquals(11, handSize(drawFour, "B"));

        drawFour.execute(new ChooseColor("A", CardColor.BLUE));
        assertEquals(CardColor.BLUE, drawFour.getActiveColor());
        assertEquals("C", current(drawFour));
    }

    @Test
    void aWildLetsOnlyWhoPlayedItChooseTheColor() {
        Game game = GameSetup.start(THREE, List.of(hand(new Wild(), RED_SEVEN), hand(), hand()), RED_FIVE);
        game.execute(new PlayCard("A", 0));

        assertThrows(IllegalArgumentException.class, () -> game.execute(new ChooseColor("B", CardColor.BLUE)),
                "il colore non lo sceglie un altro");
        assertThrows(IllegalStateException.class, () -> game.execute(new PlayCard("A", 0)),
                "finché non c'è il colore non si gioca");
        assertThrows(IllegalStateException.class, () -> game.execute(new Draw("A")));

        game.execute(new ChooseColor("A", CardColor.BLUE));
        assertEquals(CardColor.BLUE, game.getActiveColor());
        assertEquals("B", current(game));
    }

    @Test
    void theFirstCardHasItsEffect() {
        Game wild = GameSetup.start(TWO, List.of(hand(), hand()), new Wild());
        assertEquals("A", wild.getVisibleState().currentPlayerName(), "Jolly: il colore lo sceglie il primo");
        wild.execute(new ChooseColor("A", CardColor.RED));
        assertEquals("A", current(wild), "e poi gioca lui");

        Game drawFour = GameSetup.start(THREE, List.of(hand(), hand(), hand()), new WildDrawFour());
        assertEquals(11, handSize(drawFour, "A"), "+4: il primo pesca quattro carte");
        assertEquals("C", drawFour.getVisibleState().currentPlayerName(), "e il colore lo sceglie il mazziere");
        drawFour.execute(new ChooseColor("C", CardColor.RED));
        assertEquals("B", current(drawFour));

        Game skip = GameSetup.start(THREE, List.of(hand(), hand(), hand()), new Skip(CardColor.RED));
        assertEquals("B", current(skip), "Salta: il primo perde il turno");

        Game drawTwo = GameSetup.start(THREE, List.of(hand(), hand(), hand()), new DrawTwo(CardColor.RED));
        assertEquals(9, handSize(drawTwo, "A"));
        assertEquals("B", current(drawTwo));
    }

    @Test
    void aDrawnCardCanBePlayedRightAwayOrKept() {
        Game unplayable = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE, List.of(FILLER, FILLER));
        unplayable.execute(new Draw("A"));
        assertEquals(8, handSize(unplayable, "A"));
        assertEquals("B", current(unplayable), "una carta pescata non giocabile passa il turno");
        assertFalse(unplayable.getVisibleState().canPass());

        Game played = GameSetup.start(TWO, List.of(hand(RED_THREE), hand()), RED_FIVE, List.of(RED_SEVEN, FILLER));
        played.execute(new Draw("A"));
        assertEquals("A", current(played), "una carta pescata giocabile tiene il turno");
        assertTrue(played.getVisibleState().canPass());
        assertThrows(IllegalArgumentException.class, () -> played.execute(new PlayCard("A", 0)),
                "dopo aver pescato si gioca solo la carta pescata");
        assertThrows(IllegalStateException.class, () -> played.execute(new Draw("A")), "si pesca una volta sola");
        played.execute(new PlayCard("A", 7));
        assertEquals(RED_SEVEN, played.getVisibleState().top());
        assertEquals("B", current(played));

        Game kept = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE, List.of(RED_SEVEN, FILLER));
        List<GameEvent> events = new ArrayList<>();
        kept.addObserver(events::add);
        kept.execute(new Draw("A"));
        kept.execute(new Pass("A"));
        assertEquals(8, handSize(kept, "A"), "passando si tiene la carta");
        assertEquals("B", current(kept));
        assertEquals(List.of(GameEvent.Type.CARDS_DRAWN, GameEvent.Type.TURN_PASSED), types(events));
    }

    @Test
    void anEmptyDeckIsRefilledFromTheDiscardPileOrDrawsNothing() {
        Game refilled = GameSetup.start(TWO, List.of(hand(RED_THREE), hand()), RED_FIVE, List.of());
        refilled.execute(new PlayCard("A", 0));     // negli scarti ora ci sono il 5 e, in cima, il 3
        refilled.execute(new Draw("B"));
        assertEquals(8, handSize(refilled, "B"), "il 5 sotto la cima torna nel mazzo");
        assertEquals(RED_THREE, refilled.getVisibleState().top());
        assertEquals(15, refilled.totalCardCount());

        // Nessuna carta da pescare e negli scarti solo la cima: non c'è niente da rimescolare.
        Game exhausted = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE, List.of());
        exhausted.execute(new Draw("A"));
        assertEquals(7, handSize(exhausted, "A"));
        assertEquals("B", current(exhausted), "si pesca quel che c'è, cioè niente, e il turno passa");
    }

    @Test
    void lastCardMustBeCalledBeforeThePenultimateCard() {
        List<GameEvent> events = new ArrayList<>();
        Game called = GameSetup.start(TWO, List.of(hand(RED_SEVEN), hand()), RED_FIVE);
        trimHand(called, "A", 2);
        called.addObserver(events::add);
        called.execute(new CallLastCard("A"));
        called.execute(new PlayCard("A", 0));
        assertEquals(1, handSize(called, "A"));
        assertEquals(List.of(GameEvent.Type.LAST_CARD_CALLED, GameEvent.Type.CARD_PLAYED), types(events));

        Game forgotten = GameSetup.start(TWO, List.of(hand(RED_SEVEN), hand()), RED_FIVE);
        trimHand(forgotten, "A", 2);
        forgotten.execute(new PlayCard("A", 0));
        assertEquals(3, handSize(forgotten, "A"), "chi se ne dimentica pesca due carte");

        // dopo un jolly si può ancora dichiarare mentre si sceglie il colore; altrimenti la penalità arriva dopo
        Game wildCalled = GameSetup.start(TWO, List.of(hand(new Wild()), hand()), RED_FIVE);
        trimHand(wildCalled, "A", 2);
        wildCalled.execute(new PlayCard("A", 0));
        wildCalled.execute(new CallLastCard("A"));
        wildCalled.execute(new ChooseColor("A", CardColor.BLUE));
        assertEquals(1, handSize(wildCalled, "A"));

        Game wildForgotten = GameSetup.start(TWO, List.of(hand(new Wild()), hand()), RED_FIVE);
        trimHand(wildForgotten, "A", 2);
        wildForgotten.execute(new PlayCard("A", 0));
        wildForgotten.execute(new ChooseColor("A", CardColor.BLUE));
        assertEquals(3, handSize(wildForgotten, "A"));

        Game early = GameSetup.start(TWO, List.of(hand(), hand()), RED_FIVE);
        assertThrows(IllegalStateException.class, () -> early.execute(new CallLastCard("A")),
                "con più di due carte non si dichiara");

        Game last = GameSetup.start(TWO, List.of(hand(RED_SEVEN), hand()), RED_FIVE);
        trimHand(last, "A", 1);
        last.execute(new PlayCard("A", 0));
        assertEquals(GameState.FINISHED, last.getState(), "l'ultima carta chiude la partita senza penalità");
        assertEquals("A", last.getWinner().orElseThrow().getName());
    }

    @Test
    void illegalMovesAreRejectedAndChangeNothing() {
        assertThrows(IllegalArgumentException.class, () -> new Game(List.of("A")));
        assertThrows(IllegalArgumentException.class,
                () -> new Game(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")));
        assertThrows(IllegalArgumentException.class, () -> new Game(List.of("A", "A")), "nomi tutti diversi");

        Game notStarted = GameSetup.create(TWO, List.of(hand(), hand()), RED_FIVE);
        assertThrows(IllegalStateException.class, () -> notStarted.execute(new Draw("A")));

        Game game = GameSetup.start(TWO, List.of(hand(RED_SEVEN), hand(RED_SEVEN)), RED_FIVE);
        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("B", 0)), "fuori turno");
        // in posizione 1 c'è un riempitivo giallo, che sul 5 rosso non si gioca
        assertThrows(IllegalArgumentException.class, () -> game.execute(new PlayCard("A", 1)));
        assertEquals(7, handSize(game, "A"), "la carta rifiutata resta in mano");
        assertEquals(RED_FIVE, game.getVisibleState().top());
        assertEquals("A", current(game));

        trimHand(game, "A", 1);
        game.execute(new PlayCard("A", 0));
        assertThrows(IllegalStateException.class, () -> game.execute(new Draw("B")), "a partita finita non si muove");
    }

    @Test
    void observersHearEveryMoveButNeverWhichCardsAreDrawn() {
        List<GameEvent> events = new ArrayList<>();
        Game game = GameSetup.create(THREE, List.of(hand(new DrawTwo(CardColor.RED)), hand(new WildDrawFour()), hand()),
                RED_FIVE);
        game.addObserver(events::add);
        game.start();
        assertEquals(List.of(GameEvent.Type.GAME_STARTED), types(events));
        assertEquals(RED_FIVE, events.get(0).card());

        events.clear();
        game.execute(new PlayCard("A", 0));
        assertEquals(List.of(GameEvent.Type.CARD_PLAYED, GameEvent.Type.CARDS_DRAWN, GameEvent.Type.PLAYER_SKIPPED),
                types(events));
        assertEquals("A", events.get(0).playerName());
        assertEquals("B", events.get(1).playerName());
        assertEquals(2, events.get(1).count());
        assertNull(events.get(1).card(), "si dice quante carte, non quali");

        // un osservatore tolto non riceve più niente
        List<GameEvent> removed = new ArrayList<>();
        Game other = GameSetup.create(THREE, List.of(hand(), hand(), hand()), RED_FIVE);
        GameObserver observer = removed::add;
        other.addObserver(observer);
        other.removeObserver(observer);
        other.start();
        assertEquals(List.of(), removed);
    }

    @Test
    void eachPlayerSeesTheirOwnHandAndOnlyTheActingPlayerCanPass() {
        Card marker = new NumberCard(CardColor.BLUE, 1);
        List<Card> drawPile = new ArrayList<>(List.of(new NumberCard(CardColor.RED, 2)));
        Game game = GameSetup.start(THREE, List.of(hand(), hand(marker), hand()), RED_FIVE, drawPile);

        VisibleState seenByB = game.getVisibleStateFor("B");
        assertEquals("A", seenByB.currentPlayerName(), "chi deve agire non cambia");
        assertTrue(seenByB.currentHand().contains(marker), "B vede la sua mano anche quando non tocca a lui");

        game.execute(new Draw("A"));
        assertTrue(game.getVisibleStateFor("A").canPass());
        assertFalse(game.getVisibleStateFor("B").canPass());
    }
}

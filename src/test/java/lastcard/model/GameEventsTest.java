package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static lastcard.model.GameSetup.hand;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Gli eventi che arrivano agli observer: quali, in che ordine, e che non rivelino carte nascoste. */
class GameEventsTest {

    private static final List<String> THREE = List.of("A", "B", "C");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);

    private List<GameEvent> events;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
    }

    private Game startObserved(List<String> names, List<List<Card>> hands, Card firstCard) {
        Game game = GameSetup.create(names, hands, firstCard);
        game.addObserver(events::add);
        game.start();
        return game;
    }

    private List<GameEvent.Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @Test
    void startAnnouncesTheFirstCard() {
        startObserved(THREE, List.of(hand(), hand(), hand()), RED_FIVE);

        assertEquals(List.of(GameEvent.Type.GAME_STARTED), types());
        assertEquals(RED_FIVE, events.get(0).card());
    }

    @Test
    void playingACardAnnouncesWhoPlayedWhat() {
        Card redSeven = new NumberCard(CardColor.RED, 7);
        Game game = startObserved(THREE, List.of(hand(redSeven), hand(), hand()), RED_FIVE);
        events.clear();

        game.execute(new PlayCard("A", 0));

        assertEquals(List.of(GameEvent.Type.CARD_PLAYED), types());
        assertEquals("A", events.get(0).playerName());
        assertEquals(redSeven, events.get(0).card());
    }

    @Test
    void drawTwoAnnouncesHowManyCardsButNotWhich() {
        Game game = startObserved(THREE, List.of(hand(new DrawTwo(CardColor.RED)), hand(), hand()), RED_FIVE);
        events.clear();

        game.execute(new PlayCard("A", 0));

        assertEquals(List.of(
                GameEvent.Type.CARD_PLAYED,
                GameEvent.Type.CARDS_DRAWN,
                GameEvent.Type.PLAYER_SKIPPED), types());
        GameEvent drawn = events.get(1);
        assertEquals("B", drawn.playerName());
        assertEquals(2, drawn.count());
        assertNull(drawn.card());
        assertEquals("B", events.get(2).playerName());
    }

    @Test
    void reverseWithTwoPlayersAnnouncesTheReverseAndTheSkip() {
        Game game = startObserved(List.of("A", "B"), List.of(hand(new Reverse(CardColor.RED)), hand()), RED_FIVE);
        events.clear();

        game.execute(new PlayCard("A", 0));

        assertEquals(List.of(
                GameEvent.Type.CARD_PLAYED,
                GameEvent.Type.DIRECTION_REVERSED,
                GameEvent.Type.PLAYER_SKIPPED), types());
        assertEquals("B", events.get(2).playerName());
    }

    @Test
    void wildDrawFourAnnouncesPenaltyThenTheChosenColor() {
        Game game = startObserved(THREE, List.of(hand(new WildDrawFour()), hand(), hand()), RED_FIVE);
        events.clear();

        game.execute(new PlayCard("A", 0));
        game.execute(new ChooseColor("A", CardColor.BLUE));

        assertEquals(List.of(
                GameEvent.Type.CARD_PLAYED,
                GameEvent.Type.CARDS_DRAWN,
                GameEvent.Type.PLAYER_SKIPPED,
                GameEvent.Type.COLOR_CHOSEN), types());
        assertEquals(4, events.get(1).count());
        assertEquals("A", events.get(3).playerName());
        assertEquals(CardColor.BLUE, events.get(3).color());
    }

    @Test
    void removedObserverReceivesNothing() {
        Game game = GameSetup.create(THREE, List.of(hand(), hand(), hand()), RED_FIVE);
        GameObserver observer = events::add;
        game.addObserver(observer);
        game.removeObserver(observer);

        game.start();

        assertEquals(List.of(), events);
    }
}

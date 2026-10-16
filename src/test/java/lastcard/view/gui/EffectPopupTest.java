package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import lastcard.model.Card;
import lastcard.model.CardColor;
import lastcard.model.DiscardAll;
import lastcard.model.DrawTwo;
import lastcard.model.GameEvent;
import lastcard.model.NumberCard;
import lastcard.model.Reverse;
import lastcard.model.Skip;
import lastcard.model.Wild;
import lastcard.model.WildColorRoulette;
import lastcard.model.WildDrawFour;
import lastcard.model.WildReverseDrawFour;

/** Quali mosse fanno comparire il popup al tavolo, e con che testo. Gli eventi sono quelli che manda Game. */
class EffectPopupTest {

    private static final String ME = "Tu";

    private static GameEvent played(String player, Card card) {
        return new GameEvent(GameEvent.Type.CARD_PLAYED, player, card, null, 0, null);
    }

    private static GameEvent event(GameEvent.Type type, String player, int count) {
        return new GameEvent(type, player, null, null, count, null);
    }

    private static Optional<EffectPopup> popup(GameEvent... events) {
        return EffectPopup.of(List.of(events), ME);
    }

    private static EffectPopup only(String title) {
        return new EffectPopup(title, List.of());
    }

    @Test
    void normalMovesShowNothing() {
        assertTrue(popup(played("CPU 1", new NumberCard(CardColor.RED, 5))).isEmpty());
        assertTrue(popup(event(GameEvent.Type.CARDS_DRAWN, "CPU 1", 1)).isEmpty());
        assertTrue(popup(event(GameEvent.Type.TURN_PASSED, "CPU 1", 0)).isEmpty());
        assertTrue(popup(new GameEvent(GameEvent.Type.COLOR_CHOSEN, "CPU 1", null, CardColor.BLUE, 0, null)).isEmpty(),
                "il colore scelto non si mostra");
        assertTrue(popup().isEmpty());
    }

    @Test
    void reverseAndSkipAreNoticed() {
        assertEquals(Optional.of(only("CAMBIO GIRO")),
                popup(played("CPU 1", new Reverse(CardColor.RED)), event(GameEvent.Type.DIRECTION_REVERSED, null, 0)));
        assertEquals(Optional.of(only("SALTO DEL TURNO")),
                popup(played("CPU 1", new Skip(CardColor.RED)), event(GameEvent.Type.PLAYER_SKIPPED, "CPU 2", 0)));
    }

    @Test
    void aDrawCardOnlySaysItWasPlayed() {
        // +2 classico: chi lo subisce pesca e salta il turno, ma il popup dice soltanto "+2".
        assertEquals(Optional.of(only("+2")), popup(played("CPU 1", new DrawTwo(CardColor.RED)),
                event(GameEvent.Type.CARDS_DRAWN, ME, 2), event(GameEvent.Type.PLAYER_SKIPPED, ME, 0)));
        assertEquals(Optional.of(only("+4")), popup(played(ME, new WildDrawFour()),
                event(GameEvent.Type.CARDS_DRAWN, "CPU 1", 4), event(GameEvent.Type.PLAYER_SKIPPED, "CPU 1", 0)));
    }

    @Test
    void aWildIsNoticedWithoutTheColor() {
        assertEquals(Optional.of(only("JOLLY")), popup(played("CPU 1", new Wild()),
                new GameEvent(GameEvent.Type.COLOR_CHOSEN, "CPU 1", null, CardColor.GREEN, 0, null)));
    }

    @Test
    void noMercyShowsTheRisingPenalty() {
        assertEquals(Optional.of(new EffectPopup("+2", List.of("PENALITA': 6 CARTE"))),
                popup(played("CPU 1", new DrawTwo(CardColor.BLUE)), event(GameEvent.Type.PENALTY_STACKED, "CPU 1", 6)));
    }

    @Test
    void aCardWithTwoEffectsShowsBoth() {
        // Jolly Cambia Giro Pesca 4: il giro cambia e la penalità sale.
        assertEquals(Optional.of(new EffectPopup("+4", List.of("CAMBIO GIRO", "PENALITA': 4 CARTE"))),
                popup(played("CPU 1", new WildReverseDrawFour()), event(GameEvent.Type.DIRECTION_REVERSED, null, 0),
                        event(GameEvent.Type.PENALTY_STACKED, "CPU 1", 4)));
    }

    @Test
    void theNoMercyEffectsAreNoticed() {
        assertEquals(Optional.of(only("SALTA TUTTI")), popup(event(GameEvent.Type.ALL_SKIPPED, "CPU 1", 0)));
        assertEquals(Optional.of(only("SCARTA TUTTO")), popup(played("CPU 1", new DiscardAll(CardColor.RED)),
                new GameEvent(GameEvent.Type.CARDS_DISCARDED, "CPU 1", null, CardColor.RED, 3, null)));
        assertEquals(Optional.of(only("ROULETTE")), popup(played("CPU 1", new WildColorRoulette())));
        assertEquals(Optional.of(only("SCAMBIO DI MANI")),
                popup(new GameEvent(GameEvent.Type.HANDS_SWAPPED, "CPU 1", null, null, 0, "CPU 2")));
        assertEquals(Optional.of(only("ROTAZIONE DELLE MANI")), popup(event(GameEvent.Type.HANDS_ROTATED, null, 0)));
        assertEquals(Optional.of(only("MANI RIMESCOLATE")), popup(event(GameEvent.Type.HANDS_SHUFFLED, "CPU 1", 0)));
    }

    @Test
    void theRouletteVictimLosingTheTurnIsNotASkip() {
        // Chi subisce la Roulette scopre carte e perde il turno: ha pescato, quindi niente "salto del turno".
        assertTrue(popup(new GameEvent(GameEvent.Type.COLOR_CHOSEN, ME, null, CardColor.RED, 0, null),
                event(GameEvent.Type.CARDS_DRAWN, ME, 3), event(GameEvent.Type.PLAYER_SKIPPED, ME, 0)).isEmpty());
    }

    @Test
    void eliminationNamesWhoIsOut() {
        assertEquals(Optional.of(only("CPU 2 ELIMINATO")), popup(event(GameEvent.Type.CARDS_DRAWN, "CPU 2", 10),
                event(GameEvent.Type.PLAYER_ELIMINATED, "CPU 2", 0)));
        assertEquals(Optional.of(only("SEI ELIMINATO")), popup(event(GameEvent.Type.PLAYER_ELIMINATED, ME, 0)));
    }

    @Test
    void theFirstCardOfTheGameIsNoticedToo() {
        assertEquals(Optional.of(only("CAMBIO GIRO")),
                popup(new GameEvent(GameEvent.Type.GAME_STARTED, null, new Reverse(CardColor.GREEN), null, 0, null),
                        event(GameEvent.Type.DIRECTION_REVERSED, null, 0)));
    }
}

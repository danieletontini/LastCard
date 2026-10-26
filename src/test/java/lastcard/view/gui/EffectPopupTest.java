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
import lastcard.model.WildColorRoulette;

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

    private static Optional<EffectPopup> only(String title) {
        return Optional.of(new EffectPopup(title, List.of()));
    }

    @Test
    void onlySpecialCardsAndEffectsShowAPopup() {
        assertTrue(popup(played("CPU 1", new NumberCard(CardColor.RED, 5))).isEmpty(), "una giocata normale no");
        assertTrue(popup(event(GameEvent.Type.CARDS_DRAWN, "CPU 1", 1)).isEmpty(), "una pescata normale no");
        assertTrue(popup(new GameEvent(GameEvent.Type.COLOR_CHOSEN, "CPU 1", null, CardColor.BLUE, 0, null)).isEmpty(),
                "il colore scelto non si mostra");

        // +2 classico: chi lo subisce pesca e salta il turno, ma il popup dice soltanto "+2"
        assertEquals(only("+2"), popup(played("CPU 1", new DrawTwo(CardColor.RED)),
                event(GameEvent.Type.CARDS_DRAWN, ME, 2), event(GameEvent.Type.PLAYER_SKIPPED, ME, 0)));

        assertEquals(only("SALTA TUTTI"), popup(event(GameEvent.Type.ALL_SKIPPED, "CPU 1", 0)));
        assertEquals(only("SCARTA TUTTO"), popup(played("CPU 1", new DiscardAll(CardColor.RED)),
                new GameEvent(GameEvent.Type.CARDS_DISCARDED, "CPU 1", null, CardColor.RED, 3, null)));
        assertEquals(only("ROULETTE"), popup(played("CPU 1", new WildColorRoulette())));
        assertEquals(only("ROTAZIONE DELLE MANI"), popup(event(GameEvent.Type.HANDS_ROTATED, null, 0)));
    }
}

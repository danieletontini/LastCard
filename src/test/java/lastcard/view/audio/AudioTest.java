package lastcard.view.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import lastcard.model.CardColor;
import lastcard.model.GameEvent;
import lastcard.model.NumberCard;

// Quali eventi della partita fanno rumore: i suoni sono un altro osservatore della partita, come le viste.
class AudioTest {

    private static GameEvent event(GameEvent.Type type, int count) {
        return new GameEvent(type, "A", new NumberCard(CardColor.RED, 5), CardColor.RED, count, null);
    }

    private static List<SoundEffect> soundsFor(GameEvent... events) {
        List<SoundEffect> played = new ArrayList<>();
        GameSounds sounds = new GameSounds(played::add);
        for (GameEvent e : events) {
            sounds.update(e);
        }
        return played;
    }

    @Test
    void eachGameMomentHasItsSound() {
        assertEquals(List.of(SoundEffect.CARD_PLAYED, SoundEffect.CARD_DRAWN, SoundEffect.LAST_CARD,
                        SoundEffect.PENALTY, SoundEffect.VICTORY),
                soundsFor(event(GameEvent.Type.CARD_PLAYED, 0),
                        event(GameEvent.Type.CARDS_DRAWN, 2),
                        event(GameEvent.Type.LAST_CARD_CALLED, 0),
                        event(GameEvent.Type.LAST_CARD_PENALTY, 2),
                        event(GameEvent.Type.GAME_WON, 0)));
    }
}

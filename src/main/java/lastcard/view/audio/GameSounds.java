package lastcard.view.audio;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import lastcard.model.GameEvent;
import lastcard.model.GameObserver;

/**
 * Trasforma gli eventi della partita in effetti sonori. È un observer come le viste:
 * il modello non sa che esiste l'audio.
 */
public class GameSounds implements GameObserver {

    // Gli eventi che hanno un suono; gli altri passano in silenzio.
    private static final Map<GameEvent.Type, SoundEffect> SOUNDS = new EnumMap<>(GameEvent.Type.class);

    static {
        SOUNDS.put(GameEvent.Type.CARD_PLAYED, SoundEffect.CARD_PLAYED);
        SOUNDS.put(GameEvent.Type.CARDS_DRAWN, SoundEffect.CARD_DRAWN);
        SOUNDS.put(GameEvent.Type.LAST_CARD_CALLED, SoundEffect.LAST_CARD);
        SOUNDS.put(GameEvent.Type.LAST_CARD_PENALTY, SoundEffect.PENALTY);
        SOUNDS.put(GameEvent.Type.GAME_WON, SoundEffect.VICTORY);
    }

    private final Consumer<SoundEffect> player;

    /** Di solito {@code audioPlayer::play}; nei test, una lista che raccoglie i suoni. */
    public GameSounds(Consumer<SoundEffect> player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    @Override
    public void update(GameEvent event) {
        SoundEffect sound = SOUNDS.get(event.type());
        if (sound == null) {
            return;
        }
        // Una pescata di zero carte (mazzo e scarti finiti) non fa rumore.
        if (event.type() == GameEvent.Type.CARDS_DRAWN && event.count() == 0) {
            return;
        }
        player.accept(sound);
    }
}

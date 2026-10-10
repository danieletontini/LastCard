package lastcard.view.audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import lastcard.model.CardColor;
import lastcard.model.GameEvent;
import lastcard.model.NumberCard;

// Quali eventi della partita fanno rumore, come il volume dello slider diventa decibel,
// e che senza file audio il gioco va avanti in silenzio.
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

    @Test
    void otherEventsAreSilent() {
        assertEquals(List.of(), soundsFor(event(GameEvent.Type.GAME_STARTED, 0),
                event(GameEvent.Type.PLAYER_SKIPPED, 0),
                event(GameEvent.Type.DIRECTION_REVERSED, 0),
                event(GameEvent.Type.COLOR_CHOSEN, 0),
                event(GameEvent.Type.TURN_PASSED, 0)));
    }

    @Test
    void drawingNothingMakesNoSound() {
        assertEquals(List.of(), soundsFor(event(GameEvent.Type.CARDS_DRAWN, 0)));
    }

    @Test
    void fullVolumeIsTheFileVolumeAndHalfIsAboutMinusSixDecibels() {
        assertEquals(0f, AudioPlayer.toDecibels(100, -80f), 0.001f);
        assertEquals(-6.02f, AudioPlayer.toDecibels(50, -80f), 0.01f);
    }

    @Test
    void zeroAndVeryLowVolumesUseTheMinimum() {
        assertEquals(-80f, AudioPlayer.toDecibels(0, -80f));
        // 1 vorrebbe -40 dB: se la scheda audio scende al massimo a -30, si usa -30.
        assertEquals(-30f, AudioPlayer.toDecibels(1, -30f));
    }

    @Test
    void volumeStaysBetweenZeroAndOneHundred() {
        AudioPlayer audio = new AudioPlayer();

        audio.setVolume(150);
        assertEquals(100, audio.getVolume());

        audio.setVolume(-5);
        assertEquals(0, audio.getVolume());
    }

    @Test
    void missingFilesAreSilentInsteadOfCrashing() {
        // Nei test la cartella audio non ha file WAV: ogni suono deve semplicemente non suonare.
        AudioPlayer audio = new AudioPlayer();
        assertDoesNotThrow(() -> {
            for (SoundEffect effect : SoundEffect.values()) {
                audio.play(effect);
                audio.play(effect);
            }
            audio.playMusic(Music.MENU);
            audio.setVolume(30);
            audio.stopMusic();
        });
    }
}

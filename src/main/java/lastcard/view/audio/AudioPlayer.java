package lastcard.view.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Riproduce effetti e musica dai file WAV delle risorse, con il volume scelto nelle Opzioni.
 * Se un file manca o il computer non ha l'audio, resta in silenzio: lo segnala una volta sullo
 * standard error e il gioco va avanti. Java riproduce WAV, non MP3 né OGG.
 */
public class AudioPlayer {

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 100;

    private final Map<SoundEffect, Clip> effects = new EnumMap<>(SoundEffect.class);
    private final Map<Music, Clip> tracks = new EnumMap<>(Music.class);
    // I file che non si sono potuti aprire: non si riprova a ogni suono e l'avviso esce una volta sola.
    private final Set<String> unavailable = new HashSet<>();

    private int volume = 80;
    private Music currentMusic;

    /** Il volume da 0 a 100; 0 vuol dire silenzio, e la musica si ferma finché non torna sopra zero. */
    public void setVolume(int volume) {
        this.volume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
        for (Clip clip : effects.values()) {
            applyVolume(clip, this.volume);
        }
        Clip music = currentMusic == null ? null : tracks.get(currentMusic);
        if (music == null) {
            return;
        }
        applyVolume(music, this.volume);
        if (this.volume == 0) {
            music.stop();
        } else if (!music.isRunning()) {
            music.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public int getVolume() {
        return volume;
    }

    /** Suona un effetto dall'inizio; se lo stesso effetto stava già suonando, riparte. */
    public void play(SoundEffect effect) {
        if (volume == 0) {
            return;
        }
        Clip clip = effects.get(effect);
        if (clip == null) {
            clip = open(effect.resourcePath());
            if (clip == null) {
                return;
            }
            effects.put(effect, clip);
        }
        applyVolume(clip, volume);
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /** Fa partire una musica in loop al posto di quella attuale. Se è già quella, non la fa ripartire. */
    public void playMusic(Music music) {
        if (music == currentMusic) {
            return;
        }
        stopMusic();
        currentMusic = music;
        Clip clip = tracks.get(music);
        if (clip == null) {
            clip = open(music.resourcePath());
            if (clip == null) {
                return;
            }
            tracks.put(music, clip);
        }
        applyVolume(clip, volume);
        clip.setFramePosition(0);
        if (volume > 0) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stopMusic() {
        Clip clip = currentMusic == null ? null : tracks.get(currentMusic);
        if (clip != null) {
            clip.stop();
        }
        currentMusic = null;
    }

    private Clip open(String resourcePath) {
        if (unavailable.contains(resourcePath)) {
            return null;
        }
        InputStream resource = AudioPlayer.class.getResourceAsStream(resourcePath);
        if (resource == null) {
            return markUnavailable(resourcePath, "file non trovato");
        }
        // AudioSystem ha bisogno di poter tornare indietro nel flusso per riconoscere il formato.
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(resource))) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            return markUnavailable(resourcePath, e.getMessage());
        }
    }

    private Clip markUnavailable(String resourcePath, String reason) {
        unavailable.add(resourcePath);
        System.err.println("[AudioPlayer] " + resourcePath + ": " + reason + " - nessun suono");
        return null;
    }

    private static void applyVolume(Clip clip, int volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels = toDecibels(volume, gain.getMinimum());
        gain.setValue(Math.min(gain.getMaximum(), decibels));
    }

    /**
     * Converte il volume dello slider (0..100) nel guadagno in decibel che usa Java Sound.
     * L'orecchio sente in scala logaritmica: 100 è il volume del file (0 dB), 50 circa -6 dB, 0 il minimo.
     */
    static float toDecibels(int volume, float minimumDecibels) {
        if (volume <= 0) {
            return minimumDecibels;
        }
        float decibels = (float) (20 * Math.log10(volume / 100.0));
        return Math.max(minimumDecibels, decibels);
    }
}

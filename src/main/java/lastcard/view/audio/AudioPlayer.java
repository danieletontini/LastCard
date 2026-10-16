package lastcard.view.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Riproduce effetti e musica dai file WAV delle risorse, con il volume scelto nelle Opzioni.
 * Se un file manca o il computer non ha l'audio, resta in silenzio: lo segnala una volta sullo
 * standard error e il gioco va avanti. Java riproduce WAV, non MP3 né OGG.
 * Le musiche si ripetono con un fade in all'inizio e un fade out alla fine, così il giro non si sente.
 */
public class AudioPlayer {

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 100;
    /** Quanto durano il fade in all'inizio e il fade out alla fine di una musica in loop, ciascuno. */
    static final double FADE_SECONDS = 1.0;

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
            clip = open(effect.resourcePath(), false);
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
            clip = open(music.resourcePath(), true);
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

    // Con loop vero è una musica: inizio e fine sfumano, così al giro non si sente lo stacco.
    private Clip open(String resourcePath, boolean loop) {
        if (unavailable.contains(resourcePath)) {
            return null;
        }
        InputStream resource = AudioPlayer.class.getResourceAsStream(resourcePath);
        if (resource == null) {
            return markUnavailable(resourcePath, "file non trovato");
        }
        // AudioSystem ha bisogno di poter tornare indietro nel flusso per riconoscere il formato.
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(resource))) {
            AudioFormat format = stream.getFormat();
            byte[] sound = stream.readAllBytes();
            if (loop && canFade(format)) {
                sound = fadeInAndOut(sound, format, (int) Math.round(FADE_SECONDS * format.getFrameRate()));
            }
            Clip clip = AudioSystem.getClip();
            clip.open(format, sound, 0, sound.length);
            return clip;
        } catch (Exception e) {
            return markUnavailable(resourcePath, e.getMessage());
        }
    }

    // La dissolvenza lavora sui campioni: serve PCM con segno a 16 bit, il formato dei WAV del gioco.
    static boolean canFade(AudioFormat format) {
        return format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && format.getSampleSizeInBits() == 16;
    }

    /**
     * Prepara una musica per il loop: i primi fadeFrames crescono dal silenzio (fade in) e gli ultimi fadeFrames
     * sfumano fino al silenzio (fade out), così al giro la traccia non salta da piena a piena. La lunghezza
     * resta la stessa. Le curve sono un quarto di seno: salgono in fretta all'inizio e si posano piano.
     * Se la traccia è troppo corta per le due dissolvenze, resta com'è.
     */
    static byte[] fadeInAndOut(byte[] sound, AudioFormat format, int fadeFrames) {
        int frameSize = format.getFrameSize();
        int channels = format.getChannels();
        int frames = sound.length / frameSize;
        if (fadeFrames <= 0 || frames < 2 * fadeFrames) {
            return sound;
        }
        boolean bigEndian = format.isBigEndian();
        byte[] out = sound.clone();
        for (int i = 0; i < fadeFrames; i++) {
            double gain = Math.sin((i + 0.5) / fadeFrames * Math.PI / 2);
            for (int c = 0; c < channels; c++) {
                int head = i * frameSize + c * 2;
                int tail = (frames - 1 - i) * frameSize + c * 2;
                writeSample(out, head, (int) Math.round(sample(sound, head, bigEndian) * gain), bigEndian);
                writeSample(out, tail, (int) Math.round(sample(sound, tail, bigEndian) * gain), bigEndian);
            }
        }
        return out;
    }

    private static int sample(byte[] data, int offset, boolean bigEndian) {
        int first = data[offset];
        int second = data[offset + 1];
        return bigEndian ? (first << 8) | (second & 0xFF) : (second << 8) | (first & 0xFF);
    }

    private static void writeSample(byte[] data, int offset, int value, boolean bigEndian) {
        byte high = (byte) (value >> 8);
        byte low = (byte) value;
        data[offset] = bigEndian ? high : low;
        data[offset + 1] = bigEndian ? low : high;
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

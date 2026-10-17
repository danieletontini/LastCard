package lastcard.view.gui;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Le impostazioni della schermata Opzioni: schermo intero, muto e volume.
 * Sono della vista, non del gioco: le regole si scelgono in Nuova partita. Ogni modifica viene salvata subito
 * con Preferences, così alla prossima apertura il gioco riparte con le stesse scelte.
 */
public class Settings {

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 100;
    private static final int DEFAULT_VOLUME = 80;
    /** Il volume con cui parte ogni avvio della finestra, qualunque fosse alla chiusura. */
    public static final int STARTUP_VOLUME = 67;

    private static final String FULL_SCREEN = "fullScreen";
    private static final String MUTED = "muted";
    private static final String VOLUME = "volume";

    private final Preferences preferences;

    public Settings() {
        this(Preferences.userNodeForPackage(Settings.class));
    }

    // Per i test: un nodo a parte, da cancellare alla fine, invece di quello vero dell'utente.
    Settings(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    /**
     * Ogni avvio parte allo stesso modo: schermo intero, audio attivo, volume a 67.
     * Le Opzioni cambiano le scelte durante la partita, ma al riavvio si torna a queste.
     */
    public void applyStartupDefaults() {
        setFullScreen(true);
        setMuted(false);
        setVolume(STARTUP_VOLUME);
    }

    public boolean isFullScreen() {
        return preferences.getBoolean(FULL_SCREEN, false);
    }

    public void setFullScreen(boolean fullScreen) {
        preferences.putBoolean(FULL_SCREEN, fullScreen);
    }

    public boolean isMuted() {
        return preferences.getBoolean(MUTED, false);
    }

    public void setMuted(boolean muted) {
        preferences.putBoolean(MUTED, muted);
    }

    /** Il volume scelto con lo slider, da 0 a 100. Resta salvato anche mentre il gioco è muto. */
    public int getVolume() {
        return clamp(preferences.getInt(VOLUME, DEFAULT_VOLUME));
    }

    /** Valori fuori da 0..100 vengono riportati al limite più vicino. */
    public void setVolume(int volume) {
        preferences.putInt(VOLUME, clamp(volume));
    }

    /** Il volume da usare davvero per i suoni: 0 se il gioco è muto. */
    public int effectiveVolume() {
        return isMuted() ? 0 : getVolume();
    }

    private static int clamp(int volume) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
    }
}

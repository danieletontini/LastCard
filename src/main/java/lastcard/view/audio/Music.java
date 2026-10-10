package lastcard.view.audio;

/** Le musiche di sottofondo, ripetute in loop. Ogni voce è un file WAV in src/main/resources/audio/. */
public enum Music {
    MENU("menu-music.wav");

    private final String fileName;

    Music(String fileName) {
        this.fileName = fileName;
    }

    public String resourcePath() {
        return "/audio/" + fileName;
    }
}

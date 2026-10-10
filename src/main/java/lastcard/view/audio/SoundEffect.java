package lastcard.view.audio;

/** Gli effetti sonori brevi. Ogni voce è un file WAV in src/main/resources/audio/. */
public enum SoundEffect {
    CARD_PLAYED("card-played.wav"),
    CARD_DRAWN("card-drawn.wav"),
    LAST_CARD("last-card.wav"),
    PENALTY("penalty.wav"),
    VICTORY("victory.wav"),
    BUTTON_CLICK("button-click.wav");

    private final String fileName;

    SoundEffect(String fileName) {
        this.fileName = fileName;
    }

    public String resourcePath() {
        return "/audio/" + fileName;
    }
}

package lastcard.model;

import java.util.Objects;

public record ChooseColor(String playerName, CardColor color) implements Move {
    public ChooseColor {
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(color, "color");
        if (color == CardColor.BLACK) {
            throw new IllegalArgumentException("The active color cannot be BLACK.");
        }
    }
}

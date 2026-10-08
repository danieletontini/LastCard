package lastcard.model;

import java.util.Objects;

public record PlayCard(String playerName, int handIndex) implements Move {
    public PlayCard {
        Objects.requireNonNull(playerName, "playerName");
        if (handIndex < 0) {
            throw new IllegalArgumentException("handIndex cannot be negative.");
        }
    }
}

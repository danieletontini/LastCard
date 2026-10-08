package lastcard.model;

import java.util.Objects;

public record Draw(String playerName) implements Move {
    public Draw {
        Objects.requireNonNull(playerName, "playerName");
    }
}

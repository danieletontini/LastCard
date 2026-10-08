package lastcard.model;

import java.util.Objects;

public record CallLastCard(String playerName) implements Move {
    public CallLastCard {
        Objects.requireNonNull(playerName, "playerName");
    }
}

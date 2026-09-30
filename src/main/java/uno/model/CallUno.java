package uno.model;

import java.util.Objects;

public record CallUno(String playerName) implements Move {
    public CallUno {
        Objects.requireNonNull(playerName, "playerName");
    }
}

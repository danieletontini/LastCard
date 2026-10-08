package uno.model;

import java.util.Objects;

/** No Mercy, scambio 7: chi ha giocato il 7 sceglie con chi scambiare la mano. */
public record SwapHands(String playerName, String targetName) implements Move {
    public SwapHands {
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(targetName, "targetName");
        if (playerName.equals(targetName)) {
            throw new IllegalArgumentException("A player cannot swap hands with themselves.");
        }
    }
}

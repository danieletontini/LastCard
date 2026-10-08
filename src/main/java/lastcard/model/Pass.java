package lastcard.model;

import java.util.Objects;

/** Dopo aver pescato una carta giocabile, il giocatore la tiene e passa il turno. */
public record Pass(String playerName) implements Move {
    public Pass {
        Objects.requireNonNull(playerName, "playerName");
    }
}

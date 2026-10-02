package uno.model;

import java.util.Objects;

/** Ciò che si vede di un giocatore dal tavolo: nome e numero di carte, mai le carte. */
public record PlayerSummary(String name, int cardCount) {

    public PlayerSummary(String name, int cardCount) {
        this.name = Objects.requireNonNull(name, "name");
        this.cardCount = cardCount;
    }
}

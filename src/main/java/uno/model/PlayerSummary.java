package uno.model;

import java.util.Objects;

/**
 * Ciò che si vede di un giocatore dal tavolo: nome e numero di carte, mai le carte.
 * Più se è stato eliminato (No Mercy) e i punti presi finora (partita a punti).
 */
public record PlayerSummary(String name, int cardCount, boolean eliminated, int score) {

    public PlayerSummary(String name, int cardCount, boolean eliminated, int score) {
        this.name = Objects.requireNonNull(name, "name");
        this.cardCount = cardCount;
        this.eliminated = eliminated;
        this.score = score;
    }
}

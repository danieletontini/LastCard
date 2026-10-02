package uno.model;

import java.util.List;
import java.util.Objects;

/**
 * Ciò che può vedere il giocatore che deve agire: la propria mano, e degli avversari solo il numero di carte.
 * È l'unica cosa del modello che arriva alla vista.
 */
public record VisibleState(
        Card top,
        CardColor activeColor,
        String currentPlayerName,
        List<Card> currentHand,
        List<PlayerSummary> players,
        GameState state) {

    public VisibleState(
            Card top,
            CardColor activeColor,
            String currentPlayerName,
            List<Card> currentHand,
            List<PlayerSummary> players,
            GameState state) {
        this.top = top;
        this.activeColor = activeColor;
        this.currentPlayerName = Objects.requireNonNull(currentPlayerName, "currentPlayerName");
        this.currentHand = List.copyOf(currentHand);
        this.players = List.copyOf(players);
        this.state = Objects.requireNonNull(state, "state");
    }
}

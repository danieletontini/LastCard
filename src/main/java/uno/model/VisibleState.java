package uno.model;

import java.util.List;
import java.util.Objects;

/**
 * Ciò che può vedere il giocatore che deve agire: la propria mano, e degli avversari solo il numero di carte.
 * È l'unica cosa del modello che arriva alla vista.
 * {@code canPass} è vero quando il giocatore ha appena pescato una carta giocabile: può giocarla o passare.
 * {@code pendingDraw} è la penalità accumulata che il giocatore deve rilanciare o pescare (No Mercy), 0 se nessuna.
 */
public record VisibleState(
        Card top,
        CardColor activeColor,
        String currentPlayerName,
        List<Card> currentHand,
        List<PlayerSummary> players,
        GameState state,
        boolean canPass,
        int pendingDraw) {

    public VisibleState(
            Card top,
            CardColor activeColor,
            String currentPlayerName,
            List<Card> currentHand,
            List<PlayerSummary> players,
            GameState state,
            boolean canPass,
            int pendingDraw) {
        this.top = top;
        this.activeColor = activeColor;
        this.currentPlayerName = Objects.requireNonNull(currentPlayerName, "currentPlayerName");
        this.currentHand = List.copyOf(currentHand);
        this.players = List.copyOf(players);
        this.state = Objects.requireNonNull(state, "state");
        this.canPass = canPass;
        this.pendingDraw = pendingDraw;
    }
}

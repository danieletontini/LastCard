package uno.controller;

import java.util.Objects;

import uno.model.CardColor;
import uno.model.Draw;
import uno.model.Game;
import uno.model.GameState;
import uno.model.Move;
import uno.model.PlayCard;
import uno.model.VisibleState;
import uno.view.UnoView;


public class GameController {

    private final Game game;
    private final UnoView view;

    public GameController(Game game, UnoView view) {
        this.game = Objects.requireNonNull(game, "game");
        this.view = Objects.requireNonNull(view, "view");
    }

    // Avvia la partita e mostra il primo stato. 
    public void start() {
        game.start();
        showOutcome();
    }

    
    public void onCardSelected(int index) {
        if (index < 0) {
            view.showError("");
            return;
        }
        perform(new PlayCard(actingPlayerName(), index), "Da implementare");
    }

    /** Il giocatore pesca una carta. */
    public void onDraw() {
        perform(new Draw(actingPlayerName()), "Da implementare");
    }

    /** Il giocatore tiene la carta appena pescata e passa il turno. */
    public void onPass() {
        throw new UnsupportedOperationException("Da implementare.");
    }

    /** Il giocatore dichiara UNO. */
    public void onUnoCalled() {
        throw new UnsupportedOperationException("Da implementare.");
    }

    /** Il giocatore sceglie il colore dopo aver giocato un jolly. */
    public void onColorChosen(CardColor color) {
        throw new UnsupportedOperationException("Da implementare.");
    }

    /** Serve al ciclo della vista console per sapere quando fermarsi. */
    public boolean isGameOver() {
        throw new UnsupportedOperationException("Da implementare.");
    }

    // Esegue la mossa; se il modello la rifiuta mostra il messaggio in italiano, altrimenti il nuovo stato.
    private void perform(Move move, String errorMessage) {
        try {
            game.execute(move);
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(errorMessage);
            return;
        }
        showOutcome();
    }

    // Dopo ogni mossa: vincitore, oppure il nuovo stato e, dopo un jolly, la scelta del colore.
    private void showOutcome() {
        if (game.isFinished()) {
            game.getWinner().ifPresent(winner -> view.showWinner(winner.getName()));
            return;
        }
        VisibleState state = game.getVisibleState();
        view.showState(state);
        if (state.state() == GameState.AWAITING_COLOR) {
            onColorChosen(view.askColor());
        }
    }

    // Chi deve agire ora: dopo un +4 è chi ha giocato la carta, non il giocatore di turno.
    private String actingPlayerName() {
        return game.getVisibleState().currentPlayerName();
    }
}

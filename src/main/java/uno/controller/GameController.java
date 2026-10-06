package uno.controller;

import java.util.Objects;
import java.util.Optional;

import uno.model.CallUno;
import uno.model.CardColor;
import uno.model.ChooseColor;
import uno.model.Draw;
import uno.model.Game;
import uno.model.GameState;
import uno.model.Move;
import uno.model.Pass;
import uno.model.PlayCard;
import uno.model.Player;
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
            view.showError("Scegli una carta della tua mano.");
            return;
        }
        perform(new PlayCard(actingPlayerName(), index), "Non puoi giocare questa carta.");
    }

    /** Il giocatore pesca una carta. */
    public void onDraw() {
        perform(new Draw(actingPlayerName()), "Non puoi pescare adesso: hai già pescato in questo turno.");
    }

    /** Il giocatore tiene la carta appena pescata e passa il turno. */
    public void onPass() {
        perform(new Pass(actingPlayerName()), "Puoi passare solo dopo aver pescato una carta giocabile.");
    }

    /** Il giocatore dichiara UNO. */
    public void onUnoCalled() {
        perform(new CallUno(actingPlayerName()), "Puoi dichiarare UNO solo quando ti restano due carte o meno.");
    }

    /** Il giocatore sceglie il colore dopo aver giocato un jolly. */
    public void onColorChosen(CardColor color) {
        // Il nero non è un colore che si può scegliere: ChooseColor lo rifiuterebbe già nel costruttore.
        if (color == null || color == CardColor.BLACK) {
            view.showError("Scegli un colore tra rosso, giallo, verde e blu.");
            return;
        }
        perform(new ChooseColor(actingPlayerName(), color), "Non puoi scegliere il colore adesso.");
    }

    /** Serve al ciclo della vista console per sapere quando fermarsi. */
    public boolean isGameOver() {
        return game.isFinished();
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
            Optional<Player> winner = game.getWinner();
            if (winner.isPresent()) {
                view.showWinner(winner.get().getName());
            }
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

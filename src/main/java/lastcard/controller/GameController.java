package lastcard.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lastcard.model.CallLastCard;
import lastcard.model.CardColor;
import lastcard.model.ChooseColor;
import lastcard.model.CpuPlayer;
import lastcard.model.Draw;
import lastcard.model.Game;
import lastcard.model.GameState;
import lastcard.model.Move;
import lastcard.model.Pass;
import lastcard.model.PlayCard;
import lastcard.model.Player;
import lastcard.model.VisibleState;
import lastcard.view.GameView;


public class GameController {

    private final Game game;
    private final GameView view;
    // Le CPU della partita, cercate per nome. Chi non è qui è un giocatore umano.
    private final Map<String, CpuPlayer> cpus;

    public GameController(Game game, GameView view, List<CpuPlayer> cpus) {
        this.game = Objects.requireNonNull(game, "game");
        this.view = Objects.requireNonNull(view, "view");
        this.cpus = new HashMap<>();

        Set<String> playerNames = new HashSet<>();
        for (Player player : game.getPlayers()) {
            playerNames.add(player.getName());
        }
        for (CpuPlayer cpu : cpus) {
            if (!playerNames.contains(cpu.getName())) {
                throw new IllegalArgumentException("No player named " + cpu.getName() + " in this game.");
            }
            this.cpus.put(cpu.getName(), cpu);
        }
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

    /** Il giocatore dichiara Ultima! */
    public void onLastCardCalled() {
        perform(new CallLastCard(actingPlayerName()), "Puoi dichiarare Ultima! solo quando ti restano due carte o meno.");
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

    // Dopo ogni mossa: prima giocano le CPU, poi vincitore oppure il nuovo stato e, dopo un jolly, la scelta del colore.
    // Quando si arriva a mostrare lo stato tocca sempre a un umano: la mano mostrata e la scelta del colore sono sue.
    private void showOutcome() {
        playCpuTurns();
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

    // Finché deve agire una CPU, le si chiede cosa fare e si eseguono le sue mosse.
    // Dopo una pescata o un jolly la stessa CPU resta di turno: il ciclo la richiama con il nuovo stato.
    private void playCpuTurns() {
        while (!game.isFinished()) {
            CpuPlayer cpu = cpus.get(actingPlayerName());
            if (cpu == null) {
                return;     // tocca a un umano
            }
            List<Move> moves = cpu.decide(game.getVisibleState());
            for (Move move : moves) {
                if (game.isFinished()) {
                    return;
                }
                game.execute(move);
            }
        }
    }

    // Chi deve agire ora: dopo un +4 è chi ha giocato la carta, non il giocatore di turno.
    private String actingPlayerName() {
        return game.getVisibleState().currentPlayerName();
    }
}

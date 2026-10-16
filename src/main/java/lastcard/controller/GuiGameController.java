package lastcard.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import lastcard.model.PlayerSummary;
import lastcard.model.SwapHands;
import lastcard.model.VisibleState;
import lastcard.view.TableView;

/**
 * Il controller della partita per il tavolo grafico. A differenza di GameController, pensato per la console,
 * non aspetta mai l'input in un ciclo: reagisce ai gesti del giocatore (TableActions) e fa giocare le CPU
 * una mossa alla volta, distanziate da un'attesa, così sul tavolo si vede cosa succede.
 *
 * Dopo ogni mossa, di chiunque sia: mostra lo stato visto dal giocatore umano, poi o chiede una scelta
 * all'umano (colore, avversario dello scambio), o programma la prossima mossa della CPU di turno,
 * o mostra il vincitore.
 */
public class GuiGameController implements TableActions {

    /** Quanto aspetta una CPU prima di ogni sua mossa. */
    public static final int CPU_MOVE_DELAY_MILLIS = 2000;

    private final Game game;
    private final TableView view;
    private final Map<String, CpuPlayer> cpus;
    private final String humanName;
    private final TurnScheduler scheduler;
    private boolean stopped;
    // Il form Opzioni è aperto: nessuno muove finché non si chiude.
    private boolean paused;

    public GuiGameController(Game game, TableView view, List<CpuPlayer> cpus, String humanName,
            TurnScheduler scheduler) {
        this.game = Objects.requireNonNull(game, "game");
        this.view = Objects.requireNonNull(view, "view");
        this.humanName = Objects.requireNonNull(humanName, "humanName");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.cpus = new HashMap<>();
        for (CpuPlayer cpu : Objects.requireNonNull(cpus, "cpus")) {
            this.cpus.put(cpu.getName(), cpu);
        }
        this.stopped = false;
        this.paused = false;
    }

    /** Prepara il tavolo e avvia la partita. */
    public void start() {
        view.reset(humanName, this);
        // Il tavolo va registrato prima di start(), per ricevere anche la prima carta scoperta.
        game.addObserver(view);
        game.start();
        afterMove();
    }

    @Override
    public void onCardSelected(int index) {
        if (index < 0) {
            view.showError("Scegli una carta della tua mano.");
            return;
        }
        perform(new PlayCard(humanName, index), rejectedCardMessage(game.getVisibleState()));
    }

    /**
     * Perché una carta è stata rifiutata. Con una penalità in corso il motivo non è il colore: si può solo
     * rilanciare con una carta Pesca di valore almeno pari a quella in cima, oppure pescare tutto il totale.
     */
    static String rejectedCardMessage(VisibleState state) {
        if (state.pendingDraw() > 0 && state.top() != null) {
            return "Puoi rilanciare solo con un +" + state.top().drawValue()
                    + " o più, oppure pescare " + state.pendingDraw() + " carte.";
        }
        return "Non puoi giocare questa carta.";
    }

    @Override
    public void onDraw() {
        perform(new Draw(humanName), "Non puoi pescare adesso: hai già pescato in questo turno.");
    }

    @Override
    public void onPass() {
        perform(new Pass(humanName), "Puoi passare solo dopo aver pescato una carta giocabile.");
    }

    @Override
    public void onLastCardCalled() {
        perform(new CallLastCard(humanName), "Puoi dichiarare Ultima! solo quando ti restano due carte o meno.");
    }

    @Override
    public void onColorChosen(CardColor color) {
        // Il nero non è un colore che si può scegliere: ChooseColor lo rifiuterebbe già nel costruttore.
        if (color == null || color == CardColor.BLACK) {
            view.showError("Scegli un colore tra rosso, giallo, verde e blu.");
            return;
        }
        perform(new ChooseColor(humanName, color), "Non puoi scegliere il colore adesso.");
    }

    @Override
    public void onSwapTargetChosen(String targetName) {
        if (targetName == null || targetName.equals(humanName)) {
            view.showError("Scegli un avversario.");
            return;
        }
        perform(new SwapHands(humanName, targetName), "Non puoi scambiare la mano con questo giocatore.");
    }

    @Override
    public void onNextHand() {
        if (stopped || paused || game.getState() != GameState.HAND_OVER) {
            return;
        }
        game.startNextHand();
        afterMove();
    }

    @Override
    public void onPause() {
        if (stopped || paused) {
            return;
        }
        paused = true;
        // La mossa della CPU in attesa si annulla: alla ripresa riparte con un'attesa intera.
        scheduler.cancel();
    }

    @Override
    public void onResume() {
        if (stopped || !paused) {
            return;
        }
        paused = false;
        // Si riparte dallo stato attuale: si riprogramma la CPU di turno o si richiede la scelta all'umano.
        afterMove();
    }

    @Override
    public void onLeaveTable() {
        stopped = true;
        scheduler.cancel();
        game.removeObserver(view);
    }

    // Una mossa del giocatore umano: solo quando tocca a lui. Se il modello la rifiuta, il motivo va al tavolo.
    private void perform(Move move, String errorMessage) {
        if (stopped || paused || isHandClosed()) {
            return;
        }
        if (!game.getVisibleState().currentPlayerName().equals(humanName)) {
            view.showError("Aspetta il tuo turno.");
            return;
        }
        try {
            game.execute(move);
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(errorMessage);
            return;
        }
        afterMove();
    }

    // Dopo ogni mossa: lo stato al tavolo, poi la scelta che il modello aspetta dall'umano,
    // la prossima mossa di una CPU dopo l'attesa, oppure il vincitore.
    private void afterMove() {
        if (stopped) {
            return;
        }
        VisibleState state = game.getVisibleStateFor(humanName);
        view.showState(state);
        if (game.isFinished()) {
            game.getWinner().ifPresent(winner -> view.showWinner(winner.getName()));
            return;
        }
        // Partita a punti: la mano è chiusa senza raggiungere l'obiettivo. La prossima parte da onNextHand().
        if (state.state() == GameState.HAND_OVER) {
            view.showHandOver(state.players());
            return;
        }

        CpuPlayer cpu = cpus.get(state.currentPlayerName());
        if (cpu != null) {
            scheduler.schedule(CPU_MOVE_DELAY_MILLIS, () -> playCpuMove(cpu));
            return;
        }
        if (state.state() == GameState.AWAITING_COLOR) {
            view.askColor(false);
        } else if (state.state() == GameState.AWAITING_ROULETTE_COLOR) {
            view.askColor(true);
        } else if (state.state() == GameState.AWAITING_SWAP_TARGET) {
            view.askSwapTarget(swapCandidates(state));
        }
    }

    // Una mossa della CPU: di solito una sola mossa, oppure Ultima! seguita dalla carta.
    // Dopo una pescata o un jolly la stessa CPU resta di turno: la sua mossa seguente arriva dopo un'altra attesa.
    private void playCpuMove(CpuPlayer cpu) {
        if (stopped || paused || isHandClosed()) {
            return;
        }
        for (Move move : cpu.decide(game.getVisibleState())) {
            if (isHandClosed()) {
                break;
            }
            game.execute(move);
        }
        afterMove();
    }

    // La mano è chiusa: è finita la partita oppure, nella partita a punti, si attende la mano successiva.
    private boolean isHandClosed() {
        return game.isFinished() || game.getState() == GameState.HAND_OVER;
    }

    // Con chi si può scambiare la mano: tutti tranne chi sceglie e gli eliminati.
    private List<PlayerSummary> swapCandidates(VisibleState state) {
        List<PlayerSummary> candidates = new ArrayList<>();
        for (PlayerSummary player : state.players()) {
            if (!player.name().equals(humanName) && !player.eliminated()) {
                candidates.add(player);
            }
        }
        return candidates;
    }
}

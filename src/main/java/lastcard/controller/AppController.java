package lastcard.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lastcard.model.BasicCpuStrategy;
import lastcard.model.CpuPlayer;
import lastcard.model.Game;
import lastcard.model.GameRules;
import lastcard.view.MenuView;

/**
 * Il controller del menu: dalle scelte di Nuova partita crea regole, giocatori e partita.
 * La partita viene creata ma non avviata: la avvierà il tavolo, quando ci sarà.
 */
public class AppController implements MenuActions {

    private static final String HUMAN_NAME = "Tu";

    private final MenuView view;
    // L'ultima partita creata e le sue CPU, pronte per il tavolo.
    private Game game;
    private List<CpuPlayer> cpus;

    public AppController(MenuView view) {
        this.view = Objects.requireNonNull(view, "view");
        this.game = null;
        this.cpus = new ArrayList<>();
    }

    @Override
    public void startGame(GameSettings settings) {
        Objects.requireNonNull(settings, "settings");

        List<String> names = new ArrayList<>();
        List<CpuPlayer> newCpus = new ArrayList<>();
        names.add(HUMAN_NAME);
        for (int i = 1; i <= settings.opponents(); i++) {
            String name = "CPU " + i;
            names.add(name);
            newCpus.add(new CpuPlayer(name, new BasicCpuStrategy()));
        }

        this.game = new Game(names, toRules(settings));
        this.cpus = newCpus;
        view.showTable(settings);
    }

    /** Le regole che corrispondono alle impostazioni: classiche o No Mercy, con l'obiettivo di punti scelto. */
    public static GameRules toRules(GameSettings settings) {
        GameRules base;
        if (settings.mode() == GameMode.NO_MERCY) {
            base = GameRules.noMercy();
        } else {
            base = GameRules.classic();
        }
        return base.withPointsToWin(settings.goal().points());
    }

    /** La partita creata con l'ultimo Inizia, se c'è. */
    public Optional<Game> getCurrentGame() {
        return Optional.ofNullable(game);
    }

    /** Le CPU della partita corrente. */
    public List<CpuPlayer> getCurrentCpus() {
        return List.copyOf(cpus);
    }
}

package lastcard.view.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import lastcard.controller.GameController;
import lastcard.model.CardColor;
import lastcard.model.GameEvent;
import lastcard.model.GameObserver;
import lastcard.model.GameState;
import lastcard.model.PlayerSummary;
import lastcard.model.VisibleState;
import lastcard.view.GameView;

public class ConsoleView implements GameView, GameObserver {

    // I colori tra cui si può scegliere dopo un jolly, nell'ordine del menu.
    private static final List<CardColor> CHOOSABLE_COLORS = List.of(
            CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE);

    private final CardFormatter formatter;
    private final Scanner in;

    public ConsoleView(CardFormatter formatter, Scanner in) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.in = Objects.requireNonNull(in, "in");
    }

    /**
     * Avvia la partita e legge i comandi da tastiera finché non c'è un vincitore.
     * Il controller arriva qui e non nel costruttore, perché il controller ha già bisogno della vista per nascere.
     */
    public void run(GameController controller) {
        Objects.requireNonNull(controller, "controller");
        controller.start();
        while (!controller.isGameOver()) {
            System.out.print("> ");
            if (!in.hasNextLine()) {
                return;     // input chiuso (Ctrl+D): si esce senza errori
            }
            String command = in.nextLine().trim().toLowerCase();
            if (!command.isEmpty()) {
                handleCommand(command, controller);
            }
        }
    }

    // Traduce un comando in un'azione del controller: numero = gioca, d = pesca, k = passa, u = Ultima!
    private void handleCommand(String command, GameController controller) {
        if (command.equals("d")) {
            controller.onDraw();
        } else if (command.equals("k")) {
            controller.onPass();
        } else if (command.equals("u")) {
            controller.onLastCardCalled();
        } else {
            try {
                controller.onCardSelected(Integer.parseInt(command));
            } catch (NumberFormatException e) {
                showError("Comando non riconosciuto. Usa un numero, d, k oppure u.");
            }
        }
    }

    @Override
    public void showState(VisibleState state) {
        System.out.println();
        System.out.println("── Turno di " + state.currentPlayerName() + " ──");

        String top = state.top() == null ? "nessuna" : formatter.format(state.top());
        System.out.println("In cima: " + top + "   Colore attivo: " + activeColorText(state));

        System.out.println("Avversari: " + opponentsText(state));
        if (state.pendingDraw() > 0) {
            System.out.println("Penalità in corso: " + cardsText(state.pendingDraw()));
        }
        System.out.println("La tua mano: " + formatter.formatHand(state.currentHand()));

        if (state.state() != GameState.IN_PROGRESS) {
            return;
        }
        if (state.canPass()) {
            
            int drawnIndex = state.currentHand().size() - 1;
            System.out.println("Hai pescato " + formatter.format(state.currentHand().get(drawnIndex))
                    + ": puoi giocarla o passare.");
            System.out.println("Comandi: " + drawnIndex + " = gioca la carta pescata, k = passa, u = Ultima!");
        } else {
            System.out.println("Comandi: numero = gioca la carta, d = pesca, u = Ultima!");
        }
    }

    @Override
    public CardColor askColor() {
        StringBuilder menu = new StringBuilder("Scegli il colore:");
        for (int i = 0; i < CHOOSABLE_COLORS.size(); i++) {
            menu.append("  ").append(i + 1).append(") ").append(formatter.formatColor(CHOOSABLE_COLORS.get(i)));
        }

        while (true) {
            System.out.println(menu);
            System.out.print("> ");
            if (!in.hasNextLine()) {
                // Input chiuso (Ctrl+D): si sceglie il primo colore, poi il ciclo dei comandi si ferma da solo.
                return CHOOSABLE_COLORS.get(0);
            }
            String line = in.nextLine().trim();
            try {
                int choice = Integer.parseInt(line);
                if (choice >= 1 && choice <= CHOOSABLE_COLORS.size()) {
                    return CHOOSABLE_COLORS.get(choice - 1);
                }
            } catch (NumberFormatException e) {
                // non è un numero: si ripete la domanda
            }
            showError("Scrivi un numero da 1 a " + CHOOSABLE_COLORS.size() + ".");
        }
    }

    @Override
    public String askSwapTarget(List<String> opponents) {
        StringBuilder menu = new StringBuilder("Con chi scambi la mano?");
        for (int i = 0; i < opponents.size(); i++) {
            menu.append("  ").append(i + 1).append(") ").append(opponents.get(i));
        }

        while (true) {
            System.out.println(menu);
            System.out.print("> ");
            if (!in.hasNextLine()) {
                // Input chiuso (Ctrl+D): si sceglie il primo, poi il ciclo dei comandi si ferma da solo.
                return opponents.get(0);
            }
            String line = in.nextLine().trim();
            try {
                int choice = Integer.parseInt(line);
                if (choice >= 1 && choice <= opponents.size()) {
                    return opponents.get(choice - 1);
                }
            } catch (NumberFormatException e) {
                // non è un numero: si ripete la domanda
            }
            showError("Scrivi un numero da 1 a " + opponents.size() + ".");
        }
    }

    @Override
    public void showError(String message) {
        System.out.println("Errore: " + message);
    }

    @Override
    public void showWinner(String name) {
        System.out.println();
        System.out.println("*** " + name + " ha vinto la partita! ***");
    }

    @Override
    public void showHandOver(List<PlayerSummary> players) {
        System.out.println();
        System.out.println("── Fine della mano: punteggi ──");
        for (PlayerSummary player : players) {
            System.out.println("  " + player.name() + ": " + player.score() + " punti");
        }
        System.out.println("Premi Invio per iniziare la prossima mano.");
        // Con l'input chiuso (Ctrl+D) non si aspetta: la partita continua e il ciclo dei comandi si ferma da solo.
        if (in.hasNextLine()) {
            in.nextLine();
        }
    }

    // Una riga per ogni cosa che succede nella partita: così si vede anche cosa fanno le CPU nei loro turni.
    @Override
    public void update(GameEvent event) {
        String name = "[" + event.playerName() + "]";
        GameEvent.Type type = event.type();

        if (type == GameEvent.Type.GAME_STARTED) {
            printEvent("Prima carta scoperta: " + formatter.format(event.card()));
        } else if (type == GameEvent.Type.CARD_PLAYED) {
            printEvent(name + " gioca " + formatter.format(event.card()));
        } else if (type == GameEvent.Type.CARDS_DRAWN) {
            printEvent(name + " pesca " + cardsText(event.count()));
        } else if (type == GameEvent.Type.TURN_PASSED) {
            printEvent(name + " tiene la carta pescata e passa");
        } else if (type == GameEvent.Type.COLOR_CHOSEN) {
            printEvent(name + " sceglie il colore " + formatter.formatColor(event.color()));
        } else if (type == GameEvent.Type.DIRECTION_REVERSED) {
            printEvent("Il senso di gioco si inverte");
        } else if (type == GameEvent.Type.PLAYER_SKIPPED) {
            printEvent(name + " salta il turno");
        } else if (type == GameEvent.Type.LAST_CARD_CALLED) {
            printEvent(name + " dichiara Ultima!");
        } else if (type == GameEvent.Type.LAST_CARD_PENALTY) {
            printEvent(name + " non ha dichiarato Ultima!: pesca " + cardsText(event.count()) + " di penalità");
        } else if (type == GameEvent.Type.PENALTY_STACKED) {
            printEvent(name + " rilancia: la penalità sale a " + cardsText(event.count()));
        } else if (type == GameEvent.Type.ALL_SKIPPED) {
            printEvent(name + " fa saltare il turno a tutti gli altri");
        } else if (type == GameEvent.Type.CARDS_DISCARDED) {
            printEvent(name + " scarta altre " + cardsText(event.count()) + " " + formatter.formatColor(event.color()));
        } else if (type == GameEvent.Type.HANDS_SWAPPED) {
            printEvent(name + " scambia la mano con [" + event.targetName() + "]");
        } else if (type == GameEvent.Type.HANDS_ROTATED) {
            printEvent("Tutti passano la propria mano al giocatore successivo");
        } else if (type == GameEvent.Type.PLAYER_ELIMINATED) {
            printEvent(name + " ha troppe carte ed è eliminato");
        } else if (type == GameEvent.Type.HAND_WON) {
            printEvent(name + " chiude la mano e prende " + event.count() + " punti");
        }
        // GAME_WON non stampa niente: il vincitore lo annuncia già showWinner, chiamato dal controller.
    }

    private void printEvent(String message) {
        System.out.println("» " + message);
    }

    private String cardsText(int count) {
        if (count == 1) {
            return "1 carta";
        }
        return count + " carte";
    }

    private String activeColorText(VisibleState state) {
        boolean choosing = state.state() == GameState.AWAITING_COLOR
                || state.state() == GameState.AWAITING_ROULETTE_COLOR;
        if (choosing || state.activeColor() == null) {
            return "da scegliere";
        }
        return formatter.formatColor(state.activeColor());
    }

    private String opponentsText(VisibleState state) {
        List<String> parts = new ArrayList<>();
        for (PlayerSummary player : state.players()) {
            if (player.name().equals(state.currentPlayerName())) {
                continue;
            }
            if (player.eliminated()) {
                parts.add(player.name() + " eliminato");
            } else {
                parts.add(player.name() + " " + player.cardCount());
            }
        }
        return String.join(" · ", parts);
    }
}

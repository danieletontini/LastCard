package uno.view.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import uno.controller.GameController;
import uno.model.CardColor;
import uno.model.GameState;
import uno.model.PlayerSummary;
import uno.model.VisibleState;
import uno.view.UnoView;

public class ConsoleView implements UnoView {

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

    // Traduce un comando in un'azione del controller: numero = gioca, d = pesca, k = passa, u = UNO.
    private void handleCommand(String command, GameController controller) {
        if (command.equals("d")) {
            controller.onDraw();
        } else if (command.equals("k")) {
            controller.onPass();
        } else if (command.equals("u")) {
            controller.onUnoCalled();
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
        System.out.println("La tua mano: " + formatter.formatHand(state.currentHand()));

        if (state.state() != GameState.IN_PROGRESS) {
            return;
        }
        if (state.canPass()) {
            
            int drawnIndex = state.currentHand().size() - 1;
            System.out.println("Hai pescato " + formatter.format(state.currentHand().get(drawnIndex))
                    + ": puoi giocarla o passare.");
            System.out.println("Comandi: " + drawnIndex + " = gioca la carta pescata, k = passa, u = UNO");
        } else {
            System.out.println("Comandi: numero = gioca la carta, d = pesca, u = UNO");
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
    public void showError(String message) {
        System.out.println("Errore: " + message);
    }

    @Override
    public void showWinner(String name) {
        System.out.println();
        System.out.println("*** " + name + " ha vinto la partita! ***");
    }

    private String activeColorText(VisibleState state) {
        if (state.state() == GameState.AWAITING_COLOR || state.activeColor() == null) {
            return "da scegliere";
        }
        return formatter.formatColor(state.activeColor());
    }

    private String opponentsText(VisibleState state) {
        List<String> parts = new ArrayList<>();
        for (PlayerSummary player : state.players()) {
            if (!player.name().equals(state.currentPlayerName())) {
                parts.add(player.name() + " " + player.cardCount());
            }
        }
        return String.join(" · ", parts);
    }
}

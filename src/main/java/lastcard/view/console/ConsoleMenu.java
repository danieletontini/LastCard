package lastcard.view.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import lastcard.controller.GameGoal;
import lastcard.controller.GameMode;
import lastcard.controller.GameSettings;

/**
 * I menu del terminale: quello principale (nuova partita o esci) e le domande prima di una partita,
 * modalità, numero di CPU e obiettivo, come Nuova partita nella GUI.
 */
public class ConsoleMenu {

    /** Il nome usato se il giocatore non ne scrive uno, o se l'input è chiuso. */
    public static final String DEFAULT_PLAYER_NAME = "Giocatore";
    private static final int MAX_NAME_LENGTH = 15;
    // I nomi delle CPU iniziano così: un giocatore con lo stesso nome si confonderebbe con loro.
    private static final String CPU_PREFIX = "cpu";
    // Larghezza interna del riquadro del titolo, in caratteri.
    private static final int TITLE_WIDTH = 40;
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    private final Scanner in;

    public ConsoleMenu(Scanner in) {
        this.in = Objects.requireNonNull(in, "in");
    }

    /** Il titolo del gioco in un riquadro, all'avvio: come la schermata iniziale della GUI. */
    public void showTitle() {
        String border = "═".repeat(TITLE_WIDTH);
        System.out.println();
        System.out.println("╔" + border + "╗");
        System.out.println("║" + centered("", false) + "║");
        System.out.println("║" + centered("U L T I M A   C A R T A", true) + "║");
        System.out.println("║" + centered("", false) + "║");
        System.out.println("║" + centered("Classica e No Mercy contro la CPU", false) + "║");
        System.out.println("╚" + border + "╝");
        System.out.println();
    }

    // Il testo al centro di una riga larga TITLE_WIDTH. Gli spazi si contano sul testo semplice,
    // perché i codici del grassetto non occupano spazio sullo schermo.
    private String centered(String text, boolean bold) {
        int left = (TITLE_WIDTH - text.length()) / 2;
        int right = TITLE_WIDTH - text.length() - left;
        String shown = text;
        if (bold) {
            shown = BOLD + text + RESET;
        }
        return " ".repeat(left) + shown + " ".repeat(right);
    }

    /**
     * Chiede il nome del giocatore, una volta sola all'avvio. Invio senza scrivere niente sceglie il nome predefinito.
     * Il nome non può essere più lungo di MAX_NAME_LENGTH caratteri né iniziare come quello di una CPU.
     */
    public String askPlayerName() {
        while (true) {
            System.out.println("Come ti chiami? (Invio per \"" + DEFAULT_PLAYER_NAME + "\")");
            System.out.print("> ");
            if (!in.hasNextLine()) {
                return DEFAULT_PLAYER_NAME;
            }
            String name = in.nextLine().trim();
            if (name.isEmpty()) {
                return DEFAULT_PLAYER_NAME;
            }
            if (name.length() > MAX_NAME_LENGTH) {
                System.out.println("Errore: il nome può avere al massimo " + MAX_NAME_LENGTH + " caratteri.");
            } else if (name.toLowerCase().startsWith(CPU_PREFIX)) {
                System.out.println("Errore: il nome non può iniziare con \"CPU\", è riservato agli avversari.");
            } else {
                return name;
            }
        }
    }

    /** Il menu principale, all'avvio e dopo ogni partita: vero se si vuole giocare, falso per uscire. Con l'input chiuso esce. */
    public boolean askNewGame() {
        System.out.println();
        System.out.println("══ Menu ══");
        List<String> labels = List.of("Nuova partita", "Esci");
        int choice = askChoice("Cosa vuoi fare?", labels, 1);
        return choice == 0;
    }

    /** Chiede le impostazioni una alla volta. Con l'input chiuso (Ctrl+D) sceglie la prima voce di ogni menu. */
    public GameSettings askSettings() {
        System.out.println();
        System.out.println("══ Nuova partita ══");
        GameMode mode = askMode();
        int opponents = askOpponents();
        GameGoal goal = askGoal(mode);
        return new GameSettings(mode, opponents, goal);
    }

    private GameMode askMode() {
        GameMode[] modes = GameMode.values();
        List<String> labels = new ArrayList<>();
        for (GameMode mode : modes) {
            labels.add(modeLabel(mode));
        }
        return modes[askChoice("Modalità:", labels, 0)];
    }

    private int askOpponents() {
        String question = "Quante CPU? (da " + GameSettings.MIN_OPPONENTS + " a " + GameSettings.MAX_OPPONENTS + ")";
        return askNumber(question, GameSettings.MIN_OPPONENTS, GameSettings.MAX_OPPONENTS,
                GameSettings.MIN_OPPONENTS);
    }

    // I punti degli obiettivi cambiano con la modalità, per questo la si chiede prima.
    private GameGoal askGoal(GameMode mode) {
        GameGoal[] goals = GameGoal.values();
        List<String> labels = new ArrayList<>();
        for (GameGoal goal : goals) {
            labels.add(goalLabel(goal, mode));
        }
        return goals[askChoice("Obiettivo:", labels, 0)];
    }

    // Un menu numerato da 1: restituisce la posizione nella lista della voce scelta,
    // oppure whenClosed se l'input è chiuso.
    private int askChoice(String question, List<String> labels, int whenClosed) {
        StringBuilder menu = new StringBuilder(question);
        for (int i = 0; i < labels.size(); i++) {
            menu.append("  ").append(i + 1).append(") ").append(labels.get(i));
        }
        return askNumber(menu.toString(), 1, labels.size(), whenClosed + 1) - 1;
    }

    // Ripete la domanda finché non arriva un numero tra min e max. Con l'input chiuso restituisce whenClosed.
    private int askNumber(String question, int min, int max, int whenClosed) {
        while (true) {
            System.out.println(question);
            System.out.print("> ");
            if (!in.hasNextLine()) {
                return whenClosed;
            }
            String line = in.nextLine().trim();
            try {
                int choice = Integer.parseInt(line);
                if (choice >= min && choice <= max) {
                    return choice;
                }
            } catch (NumberFormatException e) {
                // non è un numero: si ripete la domanda
            }
            System.out.println("Errore: scrivi un numero da " + min + " a " + max + ".");
        }
    }

    private String modeLabel(GameMode mode) {
        if (mode == GameMode.NO_MERCY) {
            return "No Mercy";
        }
        return "Classica";
    }

    private String goalLabel(GameGoal goal, GameMode mode) {
        if (goal == GameGoal.SINGLE_HAND) {
            return "Mano singola";
        }
        return goal.points(mode) + " punti";
    }
}

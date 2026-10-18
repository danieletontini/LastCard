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

    private final Scanner in;

    public ConsoleMenu(Scanner in) {
        this.in = Objects.requireNonNull(in, "in");
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

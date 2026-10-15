package lastcard.view.gui;

import java.util.EnumMap;
import java.util.Map;

import lastcard.controller.GameGoal;
import lastcard.controller.GameMode;

// I testi mostrati per le scelte di modalità e obiettivo. Stanno nella vista, non nel controller.
final class ChoiceLabels {

    static final Map<GameMode, String> MODES = Map.of(
            GameMode.CLASSIC, "Classica",
            GameMode.NO_MERCY, "No Mercy");

    static final Map<GameMode, String> MODE_DESCRIPTIONS = Map.of(
            GameMode.CLASSIC, "108 carte, regole ufficiali. Le carte pesca non si sommano.",
            GameMode.NO_MERCY, "168 carte, penalità che si sommano, eliminazione a 25 carte.");

    private ChoiceLabels() {
    }

    /** "Mano singola" oppure "100 punti", "1000 punti"...: i punti cambiano con la modalità. */
    static String goal(GameGoal goal, GameMode mode) {
        if (goal == GameGoal.SINGLE_HAND) {
            return "Mano singola";
        }
        return goal.points(mode) + " punti";
    }

    /** Le etichette di tutti gli obiettivi per la modalità data, per i pulsanti di Nuova partita. */
    static Map<GameGoal, String> goals(GameMode mode) {
        Map<GameGoal, String> labels = new EnumMap<>(GameGoal.class);
        for (GameGoal goal : GameGoal.values()) {
            labels.put(goal, goal(goal, mode));
        }
        return labels;
    }

    static String goalDescription(GameGoal goal, GameMode mode) {
        if (goal == GameGoal.SINGLE_HAND) {
            return "Vince chi resta per primo senza carte.";
        }
        return "Si giocano più mani: vince chi arriva per primo a " + goal.points(mode) + " punti.";
    }
}

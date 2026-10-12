package lastcard.view.gui;

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

    static final Map<GameGoal, String> GOALS = Map.of(
            GameGoal.SINGLE_HAND, "Mano singola",
            GameGoal.POINTS_100, "100 punti",
            GameGoal.POINTS_500, "500 punti");

    static final Map<GameGoal, String> GOAL_DESCRIPTIONS = Map.of(
            GameGoal.SINGLE_HAND, "Vince chi resta per primo senza carte.",
            GameGoal.POINTS_100, "Si giocano più mani: vince chi arriva per primo a 100 punti.",
            GameGoal.POINTS_500, "Si giocano più mani: vince chi arriva per primo a 500 punti.");

    private ChoiceLabels() {
    }
}

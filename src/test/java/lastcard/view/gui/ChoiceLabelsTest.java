package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import lastcard.controller.GameGoal;
import lastcard.controller.GameMode;

/** I testi di Nuova partita: i punti dell'obiettivo seguono la modalità scelta. */
class ChoiceLabelsTest {

    @Test
    void theGoalButtonsShowThePointsOfTheChosenMode() {
        assertEquals(Map.of(GameGoal.SINGLE_HAND, "Mano singola", GameGoal.SHORT, "100 punti",
                GameGoal.LONG, "500 punti"), ChoiceLabels.goals(GameMode.CLASSIC));
        assertEquals(Map.of(GameGoal.SINGLE_HAND, "Mano singola", GameGoal.SHORT, "1000 punti",
                GameGoal.LONG, "2000 punti"), ChoiceLabels.goals(GameMode.NO_MERCY));
    }

    @Test
    void theGoalDescriptionNamesThePointsToReach() {
        assertEquals("Si giocano più mani: vince chi arriva per primo a 2000 punti.",
                ChoiceLabels.goalDescription(GameGoal.LONG, GameMode.NO_MERCY));
        assertEquals("Vince chi resta per primo senza carte.",
                ChoiceLabels.goalDescription(GameGoal.SINGLE_HAND, GameMode.NO_MERCY));
    }
}

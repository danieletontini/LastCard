package lastcard.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import lastcard.model.GameRules;

/** Dalle scelte di Nuova partita alle regole: l'obiettivo di punti dipende dalla modalità. */
class GameGoalTest {

    private static int pointsToWin(GameMode mode, GameGoal goal) {
        return AppController.toRules(new GameSettings(mode, 1, goal)).pointsToWin();
    }

    @Test
    void classicGamesAreWonAtOneHundredOrFiveHundredPoints() {
        assertEquals(0, pointsToWin(GameMode.CLASSIC, GameGoal.SINGLE_HAND));
        assertEquals(100, pointsToWin(GameMode.CLASSIC, GameGoal.SHORT));
        assertEquals(500, pointsToWin(GameMode.CLASSIC, GameGoal.LONG));
    }

    @Test
    void noMercyGamesAreWonAtOneThousandOrTwoThousandPoints() {
        assertEquals(0, pointsToWin(GameMode.NO_MERCY, GameGoal.SINGLE_HAND));
        assertEquals(1000, pointsToWin(GameMode.NO_MERCY, GameGoal.SHORT));
        assertEquals(2000, pointsToWin(GameMode.NO_MERCY, GameGoal.LONG));
    }

    @Test
    void theGoalDoesNotChangeTheRestOfTheRules() {
        GameRules rules = AppController.toRules(new GameSettings(GameMode.NO_MERCY, 1, GameGoal.LONG));

        assertEquals(GameRules.noMercy().withPointsToWin(2000), rules);
    }
}

package lastcard.controller;

import java.util.Objects;

/** Le scelte fatte nella schermata Nuova partita: arrivano al controller quando si preme Inizia. */
public record GameSettings(GameMode mode, int opponents, GameGoal goal) {

    public static final int MIN_OPPONENTS = 1;
    public static final int MAX_OPPONENTS = 9;

    public GameSettings(GameMode mode, int opponents, GameGoal goal) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(goal, "goal");
        if (opponents < MIN_OPPONENTS || opponents > MAX_OPPONENTS) {
            throw new IllegalArgumentException(
                    "opponents must be between " + MIN_OPPONENTS + " and " + MAX_OPPONENTS + ": " + opponents);
        }
        this.mode = mode;
        this.opponents = opponents;
        this.goal = goal;
    }
}

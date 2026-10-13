package lastcard.controller;

// L'obiettivo della partita: una mano sola oppure più mani fino a un certo punteggio.
public enum GameGoal {
    SINGLE_HAND(0),
    POINTS_100(100),
    POINTS_500(500);

    private final int points;

    GameGoal(int points) {
        this.points = points;
    }

    /** I punti per vincere, come li vuole GameRules: 0 vuol dire una mano sola. */
    public int points() {
        return points;
    }
}

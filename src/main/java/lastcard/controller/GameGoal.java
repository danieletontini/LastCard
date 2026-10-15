package lastcard.controller;

// L'obiettivo della partita: una mano sola oppure più mani fino a un certo punteggio.
// I punti dipendono dalla modalità: nel No Mercy chi viene eliminato lascia almeno 25 carte a chi chiude
// la mano, quindi con gli obiettivi della classica la partita finirebbe quasi sempre alla prima mano.
public enum GameGoal {
    SINGLE_HAND(0, 0),
    /** Partita corta: 100 punti nella classica, 1000 nel No Mercy. */
    SHORT(100, 1000),
    /** Partita lunga: 500 punti nella classica, 2000 nel No Mercy. */
    LONG(500, 2000);

    private final int classicPoints;
    private final int noMercyPoints;

    GameGoal(int classicPoints, int noMercyPoints) {
        this.classicPoints = classicPoints;
        this.noMercyPoints = noMercyPoints;
    }

    /** I punti per vincere nella modalità data, come li vuole GameRules: 0 vuol dire una mano sola. */
    public int points(GameMode mode) {
        if (mode == GameMode.NO_MERCY) {
            return noMercyPoints;
        }
        return classicPoints;
    }
}

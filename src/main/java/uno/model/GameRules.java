package uno.model;

import java.util.Objects;

/**
 * Le regole di una partita, come dati: la modalità No Mercy è una configurazione diversa, non una sottoclasse di Game.
 * eliminationThreshold = 0 vuol dire nessuna eliminazione; pointsToWin = 0 vuol dire una mano sola.
 */
public record GameRules(
        boolean drawStacking,
        int eliminationThreshold,
        boolean sevenSwap,
        boolean zeroRotation,
        boolean jumpIn,
        boolean drawUntilPlayable,
        int pointsToWin,
        DeckType deck) {

    public GameRules(
            boolean drawStacking,
            int eliminationThreshold,
            boolean sevenSwap,
            boolean zeroRotation,
            boolean jumpIn,
            boolean drawUntilPlayable,
            int pointsToWin,
            DeckType deck) {
        if (eliminationThreshold < 0) {
            throw new IllegalArgumentException("eliminationThreshold cannot be negative.");
        }
        if (pointsToWin < 0) {
            throw new IllegalArgumentException("pointsToWin cannot be negative.");
        }
        this.drawStacking = drawStacking;
        this.eliminationThreshold = eliminationThreshold;
        this.sevenSwap = sevenSwap;
        this.zeroRotation = zeroRotation;
        this.jumpIn = jumpIn;
        this.drawUntilPlayable = drawUntilPlayable;
        this.pointsToWin = pointsToWin;
        this.deck = Objects.requireNonNull(deck, "deck");
    }

    /** UNO classico, una mano sola. */
    public static GameRules classic() {
        return new GameRules(false, 0, false, false, false, false, 0, DeckType.CLASSIC);
    }

    /** No Mercy: accumulo, eliminazione a 25 carte, scambio 7, rotazione 0, pesca finché giocabile. Una mano sola. */
    public static GameRules noMercy() {
        return new GameRules(true, 25, true, true, false, true, 0, DeckType.NO_MERCY);
    }

    /** Le stesse regole con un altro obiettivo di punti: serve alla schermata Nuova partita (100 o 500). */
    public GameRules withPointsToWin(int points) {
        return new GameRules(drawStacking, eliminationThreshold, sevenSwap, zeroRotation,
                jumpIn, drawUntilPlayable, points, deck);
    }
}

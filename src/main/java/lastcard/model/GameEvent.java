package lastcard.model;

import java.util.Objects;

/**
 * Qualcosa che è successo nella partita, pubblico per tutti i giocatori: chi ha giocato quale carta,
 * chi ha pescato quante carte (mai quali), chi è stato saltato. Il testo da mostrare lo decide la vista.
 * I campi che un tipo di evento non usa valgono null (o 0 per count).
 */
public record GameEvent(Type type, String playerName, Card card, CardColor color, int count, String targetName) {

    public enum Type {
        /** Partita iniziata: card è la prima carta scoperta. */
        GAME_STARTED,
        /** playerName ha giocato card. */
        CARD_PLAYED,
        /** playerName ha pescato count carte. */
        CARDS_DRAWN,
        /** playerName ha tenuto la carta pescata e passato il turno. */
        TURN_PASSED,
        /** playerName ha dichiarato color dopo un jolly (o lo ha scelto per la Roulette). */
        COLOR_CHOSEN,
        /** Il senso di gioco si è invertito. */
        DIRECTION_REVERSED,
        /** playerName perde il turno. */
        PLAYER_SKIPPED,
        /** playerName ha dichiarato Ultima! */
        LAST_CARD_CALLED,
        /** playerName non ha dichiarato Ultima! e ha pescato count carte di penalità. */
        LAST_CARD_PENALTY,
        /** playerName ha vinto. */
        GAME_WON,
        /** No Mercy: playerName ha rilanciato la penalità, che ora vale count carte in tutto. */
        PENALTY_STACKED,
        /** No Mercy: playerName ha giocato Salta Tutti, tutti gli altri saltano il turno. */
        ALL_SKIPPED,
        /** No Mercy: playerName ha scartato altre count carte di colore color (Scarta Tutto). */
        CARDS_DISCARDED,
        /** No Mercy: playerName ha scambiato la mano con targetName (scambio 7). */
        HANDS_SWAPPED,
        /** No Mercy: tutti hanno passato la mano al successivo nel senso di gioco (rotazione 0). */
        HANDS_ROTATED,
        /** No Mercy: playerName ha giocato il Jolly Rimescola Mani, le mani si sono scambiate a caso. */
        HANDS_SHUFFLED,
        /** No Mercy: playerName ha raggiunto la soglia di carte ed è eliminato. */
        PLAYER_ELIMINATED,
        /** Partita a punti: playerName ha chiuso la mano e preso count punti. */
        HAND_WON
    }

    public GameEvent {
        Objects.requireNonNull(type, "type");
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative.");
        }
    }

    static GameEvent gameStarted(Card firstCard) {
        return new GameEvent(Type.GAME_STARTED, null, firstCard, null, 0, null);
    }

    static GameEvent cardPlayed(String playerName, Card card) {
        return new GameEvent(Type.CARD_PLAYED, playerName, card, null, 0, null);
    }

    static GameEvent cardsDrawn(String playerName, int count) {
        return new GameEvent(Type.CARDS_DRAWN, playerName, null, null, count, null);
    }

    static GameEvent turnPassed(String playerName) {
        return new GameEvent(Type.TURN_PASSED, playerName, null, null, 0, null);
    }

    static GameEvent colorChosen(String playerName, CardColor color) {
        return new GameEvent(Type.COLOR_CHOSEN, playerName, null, color, 0, null);
    }

    static GameEvent directionReversed() {
        return new GameEvent(Type.DIRECTION_REVERSED, null, null, null, 0, null);
    }

    static GameEvent playerSkipped(String playerName) {
        return new GameEvent(Type.PLAYER_SKIPPED, playerName, null, null, 0, null);
    }

    static GameEvent lastCardCalled(String playerName) {
        return new GameEvent(Type.LAST_CARD_CALLED, playerName, null, null, 0, null);
    }

    static GameEvent lastCardPenalty(String playerName, int count) {
        return new GameEvent(Type.LAST_CARD_PENALTY, playerName, null, null, count, null);
    }

    static GameEvent gameWon(String playerName) {
        return new GameEvent(Type.GAME_WON, playerName, null, null, 0, null);
    }

    static GameEvent penaltyStacked(String playerName, int total) {
        return new GameEvent(Type.PENALTY_STACKED, playerName, null, null, total, null);
    }

    static GameEvent allSkipped(String playerName) {
        return new GameEvent(Type.ALL_SKIPPED, playerName, null, null, 0, null);
    }

    static GameEvent cardsDiscarded(String playerName, CardColor color, int count) {
        return new GameEvent(Type.CARDS_DISCARDED, playerName, null, color, count, null);
    }

    static GameEvent handsSwapped(String playerName, String targetName) {
        return new GameEvent(Type.HANDS_SWAPPED, playerName, null, null, 0, targetName);
    }

    static GameEvent handsRotated() {
        return new GameEvent(Type.HANDS_ROTATED, null, null, null, 0, null);
    }

    static GameEvent handsShuffled(String playerName) {
        return new GameEvent(Type.HANDS_SHUFFLED, playerName, null, null, 0, null);
    }

    static GameEvent playerEliminated(String playerName) {
        return new GameEvent(Type.PLAYER_ELIMINATED, playerName, null, null, 0, null);
    }

    static GameEvent handWon(String playerName, int points) {
        return new GameEvent(Type.HAND_WON, playerName, null, null, points, null);
    }
}

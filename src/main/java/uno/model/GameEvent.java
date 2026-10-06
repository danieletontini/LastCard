package uno.model;

import java.util.Objects;

/**
 * Qualcosa che è successo nella partita, pubblico per tutti i giocatori: chi ha giocato quale carta,
 * chi ha pescato quante carte (mai quali), chi è stato saltato. Il testo da mostrare lo decide la vista.
 * I campi che un tipo di evento non usa valgono null (o 0 per count).
 */
public record GameEvent(Type type, String playerName, Card card, CardColor color, int count) {

    public enum Type {
        /** Partita iniziata: card è la prima carta scoperta. */
        GAME_STARTED,
        /** playerName ha giocato card. */
        CARD_PLAYED,
        /** playerName ha pescato count carte. */
        CARDS_DRAWN,
        /** playerName ha tenuto la carta pescata e passato il turno. */
        TURN_PASSED,
        /** playerName ha dichiarato color dopo un jolly. */
        COLOR_CHOSEN,
        /** Il senso di gioco si è invertito. */
        DIRECTION_REVERSED,
        /** playerName perde il turno. */
        PLAYER_SKIPPED,
        /** playerName ha chiamato UNO. */
        UNO_CALLED,
        /** playerName non ha chiamato UNO e ha pescato count carte di penalità. */
        UNO_PENALTY,
        /** playerName ha vinto. */
        GAME_WON
    }

    public GameEvent {
        Objects.requireNonNull(type, "type");
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative.");
        }
    }

    static GameEvent gameStarted(Card firstCard) {
        return new GameEvent(Type.GAME_STARTED, null, firstCard, null, 0);
    }

    static GameEvent cardPlayed(String playerName, Card card) {
        return new GameEvent(Type.CARD_PLAYED, playerName, card, null, 0);
    }

    static GameEvent cardsDrawn(String playerName, int count) {
        return new GameEvent(Type.CARDS_DRAWN, playerName, null, null, count);
    }

    static GameEvent turnPassed(String playerName) {
        return new GameEvent(Type.TURN_PASSED, playerName, null, null, 0);
    }

    static GameEvent colorChosen(String playerName, CardColor color) {
        return new GameEvent(Type.COLOR_CHOSEN, playerName, null, color, 0);
    }

    static GameEvent directionReversed() {
        return new GameEvent(Type.DIRECTION_REVERSED, null, null, null, 0);
    }

    static GameEvent playerSkipped(String playerName) {
        return new GameEvent(Type.PLAYER_SKIPPED, playerName, null, null, 0);
    }

    static GameEvent unoCalled(String playerName) {
        return new GameEvent(Type.UNO_CALLED, playerName, null, null, 0);
    }

    static GameEvent unoPenalty(String playerName, int count) {
        return new GameEvent(Type.UNO_PENALTY, playerName, null, null, count);
    }

    static GameEvent gameWon(String playerName) {
        return new GameEvent(Type.GAME_WON, playerName, null, null, 0);
    }
}

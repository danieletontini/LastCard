package uno.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Game {
    private final List<Player> players;
    private int currentIndex;
    private Direction direction;
    private GameState state;

    public Game(List<String> playerNames) {
        Objects.requireNonNull(playerNames, "playerNames");
        if (playerNames.isEmpty()) {
            throw new IllegalArgumentException("A game must have at least one player.");
        }

        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
        }

        this.currentIndex = 0;
        this.direction = Direction.CLOCKWISE;
        this.state = GameState.NOT_STARTED;
    }

    public void start() {
        if (state == GameState.FINISHED) {
            throw new IllegalStateException("The game is already finished.");
        }
        this.state = GameState.IN_PROGRESS;
        this.currentIndex = 0;
    }

    public GameState getState() {
        return state;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isFinished() {
        return state == GameState.FINISHED;
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(currentIndex);
    }
}

package uno.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Game {
    private final List<Player> players;
    private int currentIndex;
    private Direction direction;
    private GameState state;
    private CardColor activeColor;
    private final Deck deck;
    private final DiscardPile discardPile;


    public Game(List<String> playerNames) {
        Objects.requireNonNull(playerNames, "playerNames");
        if (playerNames.size() < 2 || playerNames.size() > 10) {
            throw new IllegalArgumentException("A game must have between 2 and 10 players.");
        }

        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
        }

        this.currentIndex = 0;
        this.direction = Direction.CLOCKWISE;
        this.state = GameState.NOT_STARTED;
        this.activeColor = null;
        this.deck = DeckFactory.createClassic();
        this.discardPile = new DiscardPile();

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
        return players.get(currentIndex);
    }

    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    public CardColor getActiveColor() {
        return activeColor;
    }

    //passa al giocatore successivo nel senso di marcia.
    public void nextTurn() {
        ensureInProgress();
        advanceTurn();
    }

     //salta un giocatore in più
    public void skipNext() {
        ensureInProgress();
        advanceTurn();
    }

    public void reverseDirection() {
        ensureInProgress();
        direction = direction == Direction.CLOCKWISE
                ? Direction.COUNTERCLOCKWISE
                : Direction.CLOCKWISE;
    }

    public void setActiveColor(CardColor color) {
        Objects.requireNonNull(color, "color");
        if (color == CardColor.BLACK) {
            throw new IllegalArgumentException("The active color cannot be BLACK.");
        }
        activeColor = color;
    }

    private void advanceTurn() {
        int step = direction == Direction.CLOCKWISE ? 1 : -1;
        currentIndex = Math.floorMod(currentIndex + step, players.size());
    }

    private void ensureInProgress() {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("The game is not in progress.");
        }
    }

    public List<Card> drawCards(Player player, int n) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public Optional<Player> getWinner() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }


    public Player getNextPlayer() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void awaitColorChoice() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}

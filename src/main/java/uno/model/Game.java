package uno.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Game {
    private final List<Player> players;
    private int currentIndex;
    private Direction direction;
    private GameState state;
    private CardColor activeColor;
    private final Deck deck;
    private final DiscardPile discardPile;
    private int pendingDrawValue;


    public Game(List<String> playerNames) {
        Objects.requireNonNull(playerNames, "playerNames");
        if (playerNames.size() < 2 || playerNames.size() > 10) {
            throw new IllegalArgumentException("A game must have between 2 and 10 players.");
        }

        this.players = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (String name : playerNames) {
            if (!names.add(Objects.requireNonNull(name, "player name"))) {
                throw new IllegalArgumentException("Player names must be unique.");
            }
            this.players.add(new Player(name));
        }

        this.currentIndex = 0;
        this.direction = Direction.CLOCKWISE;
        this.state = GameState.NOT_STARTED;
        this.activeColor = null;
        this.deck = DeckFactory.createClassic();
        this.discardPile = new DiscardPile();
        this.pendingDrawValue = 0;

    }

    public void start() {
        if (state != GameState.NOT_STARTED) {
            throw new IllegalStateException("The game has already started.");
        }

        deck.shuffle();
        this.currentIndex = 0;
        for (Player player : players) {
            drawCards(player, 7);
        }

        Card initialCard = drawOneCard()
                .orElseThrow(() -> new IllegalStateException("No card is available to start the discard pile."));
        discardPile.discard(initialCard);

        if (initialCard.getColor() == CardColor.BLACK) {
            pendingDrawValue = initialCard.drawValue();
            state = GameState.AWAITING_COLOR;
        } else {
            activeColor = initialCard.getColor();
            state = GameState.IN_PROGRESS;
        }
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
        if (state != GameState.AWAITING_COLOR) {
            throw new IllegalStateException("The game is not waiting for a color choice.");
        }
        if (color == CardColor.BLACK) {
            throw new IllegalArgumentException("The active color cannot be BLACK.");
        }
        activeColor = color;
        state = GameState.IN_PROGRESS;
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
        Objects.requireNonNull(player, "player");
        if (!players.contains(player)) {
            throw new IllegalArgumentException("The player does not belong to this game.");
        }
        if (n < 0) {
            throw new IllegalArgumentException("The number of cards to draw cannot be negative.");
        }

        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Optional<Card> card = drawOneCard();
            if (card.isEmpty()) {
                break;
            }
            player.addCard(card.get());
            drawnCards.add(card.get());
        }
        return List.copyOf(drawnCards);
    }

    public void execute(Move move) {
        Objects.requireNonNull(move, "move");

        if (move instanceof ChooseColor chooseColor) {
            executeColorChoice(chooseColor);
            return;
        }

        ensureInProgress();
        if (move instanceof PlayCard playCard) {
            executePlayCard(playCard);
        } else if (move instanceof Draw draw) {
            Player player = requireCurrentPlayer(draw.playerName());
            drawCards(player, 1);
            nextTurn();
        } else if (move instanceof CallUno callUno) {
            executeCallUno(callUno);
        } else {
            throw new IllegalArgumentException("Unsupported move type: " + move.getClass().getName());
        }
    }

    private void executePlayCard(PlayCard move) {
        Player player = requireCurrentPlayer(move.playerName());
        List<Card> hand = player.getHand();
        if (move.handIndex() >= hand.size()) {
            throw new IllegalArgumentException("The selected hand index is out of range.");
        }

        Card card = hand.get(move.handIndex());
        if (discardPile.isEmpty() || !card.isPlayable(discardPile.top(), activeColor)) {
            throw new IllegalArgumentException("The selected card is not playable.");
        }

        player.removeCard(move.handIndex());
        player.resetUnoCall();
        discardPile.discard(card);

        if (player.hasWon()) {
            state = GameState.FINISHED;
            return;
        }

        if (card.getColor() == CardColor.BLACK) {
            pendingDrawValue = card.drawValue();
            awaitColorChoice();
            return;
        }

        activeColor = card.getColor();
        card.applyEffect(this);
        if (card.drawValue() > 0) {
            drawCards(getNextPlayer(), card.drawValue());
            skipNext();
        }
        nextTurn();
    }

    private void executeColorChoice(ChooseColor move) {
        ensureAwaitingColor();
        requireCurrentPlayer(move.playerName());
        setActiveColor(move.color());

        if (pendingDrawValue > 0) {
            drawCards(getNextPlayer(), pendingDrawValue);
            skipNext();
            pendingDrawValue = 0;
        }
        nextTurn();
    }

    private void executeCallUno(CallUno move) {
        Player player = findPlayer(move.playerName());
        if (player.getHand().size() != 1) {
            throw new IllegalStateException("UNO can only be called with exactly one card in hand.");
        }
        player.callUno();
    }

    private Player requireCurrentPlayer(String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        Player currentPlayer = getCurrentPlayer();
        if (!currentPlayer.getName().equals(playerName)) {
            throw new IllegalArgumentException("It is not " + playerName + "'s turn.");
        }
        return currentPlayer;
    }

    private Player findPlayer(String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        return players.stream()
                .filter(player -> player.getName().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + playerName));
    }

    private void ensureAwaitingColor() {
        if (state != GameState.AWAITING_COLOR) {
            throw new IllegalStateException("The game is not waiting for a color choice.");
        }
    }

    private Optional<Card> drawOneCard() {
        if (deck.isEmpty()) {
            deck.refillFrom(discardPile);
        }
        if (deck.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deck.draw());
    }

    public Optional<Player> getWinner() {
        return players.stream().filter(Player::hasWon).findFirst();
    }


    public Player getNextPlayer() {
        ensureInProgress();
        int step = direction == Direction.CLOCKWISE ? 1 : -1;
        int nextIndex = Math.floorMod(currentIndex + step, players.size());
        return players.get(nextIndex);
    }

    public void awaitColorChoice() {
        ensureInProgress();
        state = GameState.AWAITING_COLOR;
    }

}

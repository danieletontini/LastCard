package uno.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Game {
    private static final int UNO_PENALTY = 2;

    private final GameRules rules;
    private final List<Player> players;
    private int currentIndex;
    private Direction direction;
    private GameState state;
    private CardColor activeColor;
    private final Deck deck;
    private final DiscardPile discardPile;
    private Player lastPlayer;
    // Carta appena pescata dal giocatore di turno, se giocabile: può giocare solo questa o passare.
    private Card drawnCard;
    private final boolean shuffleOnStart;
    private final List<GameObserver> observers;

    /** Partita con le regole classiche. */
    public Game(List<String> playerNames) {
        this(playerNames, GameRules.classic());
    }

    /** Partita con le regole date: il mazzo è quello indicato da rules.deck(). */
    public Game(List<String> playerNames, GameRules rules) {
        this(playerNames, Objects.requireNonNull(rules, "rules"), DeckFactory.create(rules), true);
    }

    // Per i test: regole classiche, un mazzo preparato a mano e, se shuffleOnStart è falso, pescato nell'ordine dato.
    Game(List<String> playerNames, Deck deck, boolean shuffleOnStart) {
        this(playerNames, GameRules.classic(), deck, shuffleOnStart);
    }

    // Per i test: come sopra, con regole a scelta.
    Game(List<String> playerNames, GameRules rules, Deck deck, boolean shuffleOnStart) {
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

        this.rules = Objects.requireNonNull(rules, "rules");
        this.currentIndex = 0;
        this.direction = Direction.CLOCKWISE;
        this.state = GameState.NOT_STARTED;
        this.activeColor = null;
        this.deck = Objects.requireNonNull(deck, "deck");
        this.discardPile = new DiscardPile();
        this.lastPlayer = null;
        this.drawnCard = null;
        this.shuffleOnStart = shuffleOnStart;
        this.observers = new ArrayList<>();
    }

    public void addObserver(GameObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(GameEvent event) {
        for (GameObserver observer : List.copyOf(observers)) {
            observer.update(event);
        }
    }

    public void start() {
        if (state != GameState.NOT_STARTED) {
            throw new IllegalStateException("The game has already started.");
        }

        if (shuffleOnStart) {
            deck.shuffle();
        }
        for (Player player : players) {
            drawCards(player, 7);
        }

        Card initialCard = drawOneCard()
                .orElseThrow(() -> new IllegalStateException("No card is available to start the discard pile."));
        discardPile.discard(initialCard);

        // La prima carta vale come giocata dal mazziere (l'ultimo giocatore): i suoi effetti colpiscono il primo.
        int dealerIndex = players.size() - 1;
        this.currentIndex = dealerIndex;
        this.state = GameState.IN_PROGRESS;
        notifyObservers(GameEvent.gameStarted(initialCard));
        resolveCard(getCurrentPlayer(), initialCard);

        // Jolly iniziale: il colore lo sceglie il primo giocatore. Con un +4 il turno è già passato
        // a chi ha pescato, e il colore resta al mazziere.
        if (state == GameState.AWAITING_COLOR && currentIndex == dealerIndex) {
            lastPlayer = players.get(0);
        }
    }

    public GameState getState() {
        return state;
    }

    public GameRules getRules() {
        return rules;
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

    public VisibleState getVisibleState() {
        Player acting = getActingPlayer();
        List<PlayerSummary> summaries = new ArrayList<>();
        for (Player player : players) {
            summaries.add(new PlayerSummary(player.getName(), player.getHand().size()));
        }
        Card top = discardPile.isEmpty() ? null : discardPile.top();
        return new VisibleState(top, activeColor, acting.getName(), acting.getHand(), summaries, state, drawnCard != null);
    }

    // Durante la scelta del colore deve agire chi ha giocato il jolly, non il giocatore di turno (dopo un +4 sono diversi).
    private Player getActingPlayer() {
        return state == GameState.AWAITING_COLOR ? lastPlayer : getCurrentPlayer();
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
        notifyObservers(GameEvent.playerSkipped(getCurrentPlayer().getName()));
    }

    // L'effetto della carta Inverti: in due giocatori invertire non cambia l'ordine, quindi per regola vale come Salta.
    public void reverseDirection() {
        flipDirection();
        if (players.size() == 2) {
            skipNext();
        }
    }

    // Inverte soltanto il senso di gioco, anche in due giocatori. Serve alle carte che invertono e poi
    // fanno pescare il nuovo successivo (Jolly Cambia Giro Pesca 4): con il salto di reverseDirection
    // la penalità finirebbe a chi ha giocato la carta.
    public void flipDirection() {
        ensureInProgress();
        direction = direction == Direction.CLOCKWISE
                ? Direction.COUNTERCLOCKWISE
                : Direction.CLOCKWISE;
        notifyObservers(GameEvent.directionReversed());
    }

    // No Mercy, Salta Tutti: tutti gli altri saltano il turno e tocca di nuovo a chi l'ha giocata.
    public void skipAll() {
        throw new UnsupportedOperationException("skipAll is not implemented yet.");
    }

    // No Mercy, Scarta Tutto: chi l'ha giocata scarta anche tutte le carte di quel colore che ha in mano.
    public void discardAllOfColor(CardColor color) {
        throw new UnsupportedOperationException("discardAllOfColor is not implemented yet.");
    }

    // No Mercy, Jolly Roulette Colore: il successivo sceglie un colore, scopre carte finché esce, le tiene e salta il turno.
    public void startColorRoulette() {
        throw new UnsupportedOperationException("startColorRoulette is not implemented yet.");
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

        switch (move) {
            case PlayCard playCard -> executePlayCard(playCard);
            case Draw draw -> executeDraw(draw);
            case Pass pass -> executePass(pass);
            case CallUno callUno -> executeCallUno(callUno);
            case ChooseColor chooseColor -> executeColorChoice(chooseColor);
        }
    }

    private void executePlayCard(PlayCard move) {
        ensureInProgress();
        Player player = requireCurrentPlayer(move.playerName());
        List<Card> hand = player.getHand();
        if (move.handIndex() >= hand.size()) {
            throw new IllegalArgumentException("The selected hand index is out of range.");
        }

        Card card = hand.get(move.handIndex());
        if (drawnCard != null && !card.equals(drawnCard)) {
            throw new IllegalArgumentException("After drawing, only the drawn card can be played.");
        }
        if (discardPile.isEmpty() || !card.isPlayable(discardPile.top(), activeColor)) {
            throw new IllegalArgumentException("The selected card is not playable.");
        }

        player.removeCard(move.handIndex());
        drawnCard = null;
        discardPile.discard(card);
        notifyObservers(GameEvent.cardPlayed(player.getName(), card));

        if (player.hasWon()) {
            state = GameState.FINISHED;
            notifyObservers(GameEvent.gameWon(player.getName()));
            return;
        }

        resolveCard(player, card);
        // Se si aspetta il colore il turno non è finito: il controllo dell'UNO avviene dopo la scelta.
        if (state == GameState.IN_PROGRESS) {
            endTurnAfterPlay(player);
        }
    }

    // Si pesca una carta: se è giocabile il giocatore resta di turno e può giocarla o passare.
    private void executeDraw(Draw move) {
        ensureInProgress();
        Player player = requireCurrentPlayer(move.playerName());
        if (drawnCard != null) {
            throw new IllegalStateException("A card has already been drawn this turn.");
        }

        List<Card> drawn = drawCards(player, 1);
        notifyObservers(GameEvent.cardsDrawn(player.getName(), drawn.size()));
        if (!drawn.isEmpty() && drawn.get(0).isPlayable(discardPile.top(), activeColor)) {
            drawnCard = drawn.get(0);
            return;
        }
        player.resetUnoCall();
        nextTurn();
    }

    private void executePass(Pass move) {
        ensureInProgress();
        Player player = requireCurrentPlayer(move.playerName());
        if (drawnCard == null) {
            throw new IllegalStateException("You can only pass after drawing a playable card.");
        }
        drawnCard = null;
        player.resetUnoCall();
        notifyObservers(GameEvent.turnPassed(player.getName()));
        nextTurn();
    }

    // Il turno di chi ha giocato è finito: se è rimasto con una carta senza aver chiamato UNO, paga la penalità.
    private void endTurnAfterPlay(Player player) {
        if (player.getHand().size() == 1 && !player.hasCalledUno()) {
            List<Card> penalty = drawCards(player, UNO_PENALTY);
            notifyObservers(GameEvent.unoPenalty(player.getName(), penalty.size()));
        }
        player.resetUnoCall();
    }

    // L'effetto lo decide la carta; se non resta in attesa del colore, il turno passa normalmente.
    private void resolveCard(Player player, Card card) {
        activeColor = card.getColor();
        lastPlayer = player;
        card.applyEffect(this);
        if (state == GameState.IN_PROGRESS) {
            nextTurn();
        }
    }

    private void executeColorChoice(ChooseColor move) {
        ensureAwaitingColor();
        // Sceglie chi ha giocato il jolly: dopo un +4 il turno corrente è già passato a chi ha pescato.
        if (!lastPlayer.getName().equals(move.playerName())) {
            throw new IllegalArgumentException("It is not " + move.playerName() + "'s turn to choose the color.");
        }
        setActiveColor(move.color());
        notifyObservers(GameEvent.colorChosen(lastPlayer.getName(), move.color()));
        nextTurn();
        endTurnAfterPlay(lastPlayer);
    }

    // UNO si chiama durante il proprio turno, prima di giocare la penultima carta o mentre si sceglie il colore.
    private void executeCallUno(CallUno move) {
        if (state != GameState.IN_PROGRESS && state != GameState.AWAITING_COLOR) {
            throw new IllegalStateException("The game is not in progress.");
        }
        Player player = getActingPlayer();
        if (!player.getName().equals(move.playerName())) {
            throw new IllegalArgumentException("It is not " + move.playerName() + "'s turn.");
        }
        if (player.getHand().size() > 2) {
            throw new IllegalStateException("UNO can only be called with two or fewer cards in hand.");
        }
        player.callUno();
        notifyObservers(GameEvent.unoCalled(player.getName()));
    }

    private Player requireCurrentPlayer(String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        Player currentPlayer = getCurrentPlayer();
        if (!currentPlayer.getName().equals(playerName)) {
            throw new IllegalArgumentException("It is not " + playerName + "'s turn.");
        }
        return currentPlayer;
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

    // Il prossimo giocatore pesca n carte e perde il turno. Con l'accumulo del No Mercy cambierà solo qui.
    public void penalizeNext(int n) {
        Player next = getNextPlayer();
        List<Card> drawn = drawCards(next, n);
        notifyObservers(GameEvent.cardsDrawn(next.getName(), drawn.size()));
        skipNext();
    }

    // Per i test: le carte in mani, mazzo e scarti. Deve restare costante per tutta la partita.
    int totalCardCount() {
        int inHands = players.stream().mapToInt(player -> player.getHand().size()).sum();
        return inHands + deck.getCardCount() + discardPile.getCardCount();
    }
}

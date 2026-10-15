package lastcard.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class Game {
    private static final int LAST_CARD_PENALTY = 2;

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
    // No Mercy: penalità accumulata che il giocatore di turno deve rilanciare o pescare; 0 se nessuna.
    private int pendingDraw;
    // No Mercy, Roulette: chi deve scegliere il colore e scoprire le carte. Null fuori dalla Roulette.
    private Player rouletteVictim;
    // Chi ha chiuso la mano: ha finito le carte oppure è l'ultimo rimasto dopo le eliminazioni.
    private Player winner;
    // Partita a punti: i punti delle carte che gli eliminati avevano in mano quando sono usciti dalla mano.
    // Le loro carte tornano subito negli scarti, quindi vanno contati prima: li prende chi chiude la mano.
    private int eliminatedPoints;
    private final boolean shuffleOnStart;
    private final List<GameObserver> observers;

    /** Partita con le regole classiche. */
    public Game(List<String> playerNames) {
        this(playerNames, GameRules.classic());
    }

    /** Partita con le regole date: il mazzo è quello indicato da rules.deck(). */
    public Game(List<String> playerNames, GameRules rules) {
        this(playerNames, rules, new Random());
    }

    /** Con lo stesso Random (per esempio new Random(42)) la partita si ripete identica: serve a test e replay. */
    public Game(List<String> playerNames, GameRules rules, Random random) {
        this(playerNames, Objects.requireNonNull(rules, "rules"),
                DeckFactory.create(rules, Objects.requireNonNull(random, "random")), true);
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
        this.pendingDraw = 0;
        this.rouletteVictim = null;
        this.winner = null;
        this.eliminatedPoints = 0;
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
        dealHand();
    }

    /** Partita a punti: dopo una mano chiusa senza raggiungere l'obiettivo, si rimescola e si gioca la successiva. */
    public void startNextHand() {
        if (state != GameState.HAND_OVER) {
            throw new IllegalStateException("The hand is not over.");
        }
        collectCards();
        direction = Direction.CLOCKWISE;
        activeColor = null;
        lastPlayer = null;
        drawnCard = null;
        pendingDraw = 0;
        rouletteVictim = null;
        winner = null;
        eliminatedPoints = 0;
        dealHand();
    }

    // Le mani di tutti e gli scarti, cima compresa, tornano nel mazzo. Il punteggio resta.
    // Gli eliminati della mano chiusa rientrano: l'eliminazione vale per una mano sola.
    private void collectCards() {
        for (Player player : players) {
            deck.addCards(player.takeHand());
            player.resetLastCardCall();
            player.rejoin();
        }
        deck.addCards(discardPile.takeAll());
    }

    // Mescola, dà 7 carte a testa e scopre la prima carta: serve sia alla prima mano sia alle successive.
    private void dealHand() {
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

    /** Lo stato visto da chi deve agire: è quello che serve per decidere la prossima mossa. */
    public VisibleState getVisibleState() {
        return getVisibleStateFor(getActingPlayer().getName());
    }

    /**
     * Lo stato visto da un giocatore preciso, anche quando non tocca a lui: serve al tavolo grafico,
     * che mostra sempre la mano del giocatore umano mentre agiscono le CPU.
     * canPass è vero solo se chi guarda è anche chi deve agire.
     */
    public VisibleState getVisibleStateFor(String viewerName) {
        Objects.requireNonNull(viewerName, "viewerName");
        Player viewer = players.stream()
                .filter(player -> player.getName().equals(viewerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + viewerName));
        Player acting = getActingPlayer();
        List<PlayerSummary> summaries = new ArrayList<>();
        for (Player player : players) {
            summaries.add(new PlayerSummary(player.getName(), player.getHand().size(),
                    player.isEliminated(), player.getScore()));
        }
        Card top = discardPile.isEmpty() ? null : discardPile.top();
        return new VisibleState(top, activeColor, acting.getName(), viewer.getHand(), summaries, state,
                drawnCard != null && viewer == acting, pendingDraw);
    }

    // Durante la scelta del colore o del bersaglio dello scambio deve agire chi ha giocato la carta,
    // non il giocatore di turno (dopo un +4 sono diversi). Con la Roulette sceglie chi la subisce.
    private Player getActingPlayer() {
        if (state == GameState.AWAITING_COLOR || state == GameState.AWAITING_SWAP_TARGET) {
            return lastPlayer;
        }
        if (state == GameState.AWAITING_ROULETTE_COLOR) {
            return rouletteVictim;
        }
        return getCurrentPlayer();
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
        if (activePlayerCount() == 2) {
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
    // Il turno si sposta sul giocatore che lo precede: il passaggio di turno normale lo riporta a lui.
    public void skipAll() {
        ensureInProgress();
        currentIndex = stepFrom(players.indexOf(lastPlayer), -turnStep());
        notifyObservers(GameEvent.allSkipped(lastPlayer.getName()));
    }

    // No Mercy, Scarta Tutto: chi l'ha giocata scarta anche tutte le carte di quel colore che ha in mano.
    // Vanno sotto la cima, così in cima resta Scarta Tutto. Se resta senza carte ha chiuso la mano.
    public void discardAllOfColor(CardColor color) {
        Objects.requireNonNull(color, "color");
        ensureInProgress();
        List<Card> kept = new ArrayList<>();
        List<Card> discarded = new ArrayList<>();
        for (Card card : lastPlayer.takeHand()) {
            if (card.getColor() == color) {
                discarded.add(card);
            } else {
                kept.add(card);
            }
        }
        lastPlayer.addCards(kept);
        discardPile.addUnderTop(discarded);
        notifyObservers(GameEvent.cardsDiscarded(lastPlayer.getName(), color, discarded.size()));
        if (lastPlayer.hasWon()) {
            finishHand(lastPlayer);
        }
    }

    // No Mercy, Jolly Roulette Colore: il successivo sceglie un colore, scopre carte finché esce, le tiene e salta il turno.
    // Qui si resta in attesa del colore; il resto avviene in executeRoulette.
    public void startColorRoulette() {
        ensureInProgress();
        rouletteVictim = getNextPlayer();
        state = GameState.AWAITING_ROULETTE_COLOR;
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
        currentIndex = stepFrom(currentIndex, turnStep());
    }

    private int turnStep() {
        return direction == Direction.CLOCKWISE ? 1 : -1;
    }

    // L'indice del giocatore che viene dopo from nel verso indicato, saltando gli eliminati.
    private int stepFrom(int from, int step) {
        int index = from;
        for (int i = 0; i < players.size(); i++) {
            index = Math.floorMod(index + step, players.size());
            if (!players.get(index).isEliminated()) {
                return index;
            }
        }
        return from;
    }

    private int activePlayerCount() {
        return (int) players.stream().filter(player -> !player.isEliminated()).count();
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
            case CallLastCard callLastCard -> executeCallLastCard(callLastCard);
            case ChooseColor chooseColor -> executeColorChoice(chooseColor);
            case SwapHands swapHands -> executeSwapHands(swapHands);
        }
    }

    // No Mercy, scambio 7: chi ha giocato il 7 scambia la mano con un avversario ancora in gioco.
    private void executeSwapHands(SwapHands move) {
        if (state != GameState.AWAITING_SWAP_TARGET) {
            throw new IllegalStateException("The game is not waiting for a swap target.");
        }
        if (!lastPlayer.getName().equals(move.playerName())) {
            throw new IllegalArgumentException("It is not " + move.playerName() + "'s turn to swap.");
        }
        Player target = players.stream()
                .filter(player -> player.getName().equals(move.targetName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + move.targetName()));
        if (target.isEliminated()) {
            throw new IllegalArgumentException(move.targetName() + " has been eliminated.");
        }

        List<Card> mine = lastPlayer.takeHand();
        lastPlayer.addCards(target.takeHand());
        target.addCards(mine);
        notifyObservers(GameEvent.handsSwapped(lastPlayer.getName(), target.getName()));

        state = GameState.IN_PROGRESS;
        nextTurn();
        resetAllLastCardCalls();
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
        if (discardPile.isEmpty() || !PlayRules.canPlay(card, discardPile.top(), activeColor, pendingDraw)) {
            throw new IllegalArgumentException(pendingDraw > 0
                    ? "A penalty is pending: only a draw card of equal or higher value can be played."
                    : "The selected card is not playable.");
        }

        player.removeCard(move.handIndex());
        drawnCard = null;
        discardPile.discard(card);
        notifyObservers(GameEvent.cardPlayed(player.getName(), card));

        if (player.hasWon()) {
            finishHand(player);
            return;
        }

        boolean handsMoved = resolveCard(player, card);
        // Se si aspetta il colore il turno non è finito: il controllo di Ultima! avviene dopo la scelta.
        if (state == GameState.IN_PROGRESS) {
            if (handsMoved) {
                resetAllLastCardCalls();
            } else {
                endTurnAfterPlay(player);
            }
        }
    }

    // Si pesca una carta: se è giocabile il giocatore resta di turno e può giocarla o passare.
    // Con una penalità in corso, pescare vuol dire prendere tutto il totale e perdere il turno.
    // Con la regola drawUntilPlayable si pesca finché esce una carta giocabile (o finché il mazzo finisce).
    private void executeDraw(Draw move) {
        ensureInProgress();
        Player player = requireCurrentPlayer(move.playerName());
        if (drawnCard != null) {
            throw new IllegalStateException("A card has already been drawn this turn.");
        }

        if (pendingDraw > 0) {
            List<Card> penalty = drawCards(player, pendingDraw);
            // Fallback del §8.4: se mancano carte si pesca quel che c'è e il resto della penalità decade.
            pendingDraw = 0;
            notifyObservers(GameEvent.cardsDrawn(player.getName(), penalty.size()));
            player.resetLastCardCall();
            eliminateIfOverThreshold(player);
            if (state == GameState.IN_PROGRESS) {
                nextTurn();
            }
            return;
        }

        List<Card> drawn = new ArrayList<>();
        while (true) {
            List<Card> one = drawCards(player, 1);
            if (one.isEmpty()) {
                break;
            }
            drawn.add(one.get(0));
            if (!rules.drawUntilPlayable() || one.get(0).isPlayable(discardPile.top(), activeColor)) {
                break;
            }
        }
        notifyObservers(GameEvent.cardsDrawn(player.getName(), drawn.size()));

        if (eliminateIfOverThreshold(player)) {
            if (state == GameState.IN_PROGRESS) {
                nextTurn();
            }
            return;
        }
        Card last = drawn.isEmpty() ? null : drawn.get(drawn.size() - 1);
        if (last != null && last.isPlayable(discardPile.top(), activeColor)) {
            drawnCard = last;
            return;
        }
        player.resetLastCardCall();
        nextTurn();
    }

    private void executePass(Pass move) {
        ensureInProgress();
        Player player = requireCurrentPlayer(move.playerName());
        if (drawnCard == null) {
            throw new IllegalStateException("You can only pass after drawing a playable card.");
        }
        drawnCard = null;
        player.resetLastCardCall();
        notifyObservers(GameEvent.turnPassed(player.getName()));
        nextTurn();
    }

    // Il turno di chi ha giocato è finito: se è rimasto con una carta senza aver dichiarato Ultima!, paga la penalità.
    private void endTurnAfterPlay(Player player) {
        if (isHandClosed()) {
            return;
        }
        if (player.getHand().size() == 1 && !player.hasCalledLastCard()) {
            List<Card> penalty = drawCards(player, LAST_CARD_PENALTY);
            notifyObservers(GameEvent.lastCardPenalty(player.getName(), penalty.size()));
            eliminateIfOverThreshold(player);
        }
        player.resetLastCardCall();
    }

    // Dopo uno scambio o una rotazione le mani hanno cambiato proprietario: chi ora ha una carta sola
    // non l'ha avuta giocando, quindi non c'è penalità, e le dichiarazioni fatte prima non valgono più.
    private void resetAllLastCardCalls() {
        for (Player player : players) {
            player.resetLastCardCall();
        }
    }

    // L'effetto lo decide la carta; se non resta in attesa del colore, il turno passa normalmente.
    // Le regole No Mercy dei numeri (0 e 7) dipendono dalle regole, non dalla carta, quindi stanno qui.
    // Restituisce vero se le mani hanno cambiato proprietario (rotazione dello 0).
    private boolean resolveCard(Player player, Card card) {
        activeColor = card.getColor();
        lastPlayer = player;
        card.applyEffect(this);
        if (state != GameState.IN_PROGRESS) {
            return false;
        }
        if (rules.sevenSwap() && isNumber(card, 7)) {
            state = GameState.AWAITING_SWAP_TARGET;
            return false;
        }
        boolean handsMoved = false;
        if (rules.zeroRotation() && isNumber(card, 0)) {
            rotateHands();
            handsMoved = true;
        }
        nextTurn();
        return handsMoved;
    }

    // Un numero si riconosce dal simbolo, senza instanceof: "7" e "0" li hanno solo le carte numero.
    private static boolean isNumber(Card card, int number) {
        return card.getColor() != CardColor.BLACK && card.symbol().equals(String.valueOf(number));
    }

    // No Mercy, rotazione 0: ogni giocatore ancora in gioco passa la mano al successivo nel senso di gioco.
    private void rotateHands() {
        List<Player> active = new ArrayList<>();
        List<List<Card>> hands = new ArrayList<>();
        int index = currentIndex;
        for (int i = 0; i < activePlayerCount(); i++) {
            Player player = players.get(index);
            active.add(player);
            hands.add(player.takeHand());
            index = stepFrom(index, turnStep());
        }
        for (int i = 0; i < active.size(); i++) {
            active.get((i + 1) % active.size()).addCards(hands.get(i));
        }
        notifyObservers(GameEvent.handsRotated());
    }

    private void executeColorChoice(ChooseColor move) {
        if (state == GameState.AWAITING_ROULETTE_COLOR) {
            executeRoulette(move);
            return;
        }
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

    // Roulette: chi la subisce sceglie il colore, scopre carte finché ne esce una di quel colore (i jolly
    // non contano), le tiene tutte e perde il turno. Il colore scelto diventa quello attivo.
    private void executeRoulette(ChooseColor move) {
        if (!rouletteVictim.getName().equals(move.playerName())) {
            throw new IllegalArgumentException("It is not " + move.playerName() + "'s turn to choose the color.");
        }
        if (move.color() == CardColor.BLACK) {
            throw new IllegalArgumentException("The active color cannot be BLACK.");
        }
        Player victim = rouletteVictim;
        rouletteVictim = null;
        activeColor = move.color();
        state = GameState.IN_PROGRESS;
        notifyObservers(GameEvent.colorChosen(victim.getName(), move.color()));

        int revealed = 0;
        while (true) {
            List<Card> one = drawCards(victim, 1);
            if (one.isEmpty()) {
                break;
            }
            revealed++;
            if (one.get(0).getColor() == move.color()) {
                break;
            }
        }
        notifyObservers(GameEvent.cardsDrawn(victim.getName(), revealed));

        // Il turno va oltre chi ha subito la Roulette, anche se è appena stato eliminato.
        currentIndex = players.indexOf(victim);
        eliminateIfOverThreshold(victim);
        if (state != GameState.IN_PROGRESS) {
            return;
        }
        nextTurn();
        endTurnAfterPlay(lastPlayer);
    }

    // No Mercy, senza pietà: chi arriva alla soglia di carte esce dalla mano e le sue carte tornano negli scarti.
    // Se resta un solo giocatore, la mano è sua. Restituisce vero se il giocatore è stato eliminato.
    private boolean eliminateIfOverThreshold(Player player) {
        int threshold = rules.eliminationThreshold();
        if (threshold == 0 || player.isEliminated() || player.getHand().size() < threshold) {
            return false;
        }
        eliminatedPoints = eliminatedPoints + player.handPoints();
        discardPile.addUnderTop(player.eliminate());
        notifyObservers(GameEvent.playerEliminated(player.getName()));
        if (activePlayerCount() == 1) {
            finishHand(players.stream().filter(p -> !p.isEliminated()).findFirst().orElseThrow());
        }
        return true;
    }

    // Con una mano sola la partita finisce qui. Nella partita a punti chi chiude la mano prende i punti
    // delle carte rimaste agli avversari, più quelli delle mani degli eliminati contati quando sono usciti:
    // se raggiunge l'obiettivo la partita è finita, altrimenti si attende la prossima mano.
    private void finishHand(Player handWinner) {
        winner = handWinner;
        pendingDraw = 0;
        drawnCard = null;
        rouletteVictim = null;

        if (rules.pointsToWin() == 0) {
            state = GameState.FINISHED;
            notifyObservers(GameEvent.gameWon(handWinner.getName()));
            return;
        }

        int points = eliminatedPoints;
        for (Player player : players) {
            if (player != handWinner) {
                points = points + player.handPoints();
            }
        }
        handWinner.addScore(points);
        notifyObservers(GameEvent.handWon(handWinner.getName(), points));

        if (handWinner.getScore() >= rules.pointsToWin()) {
            state = GameState.FINISHED;
            notifyObservers(GameEvent.gameWon(handWinner.getName()));
        } else {
            state = GameState.HAND_OVER;
        }
    }

    // La mano è chiusa: o è finita la partita, o si attende la prossima mano.
    private boolean isHandClosed() {
        return state == GameState.FINISHED || state == GameState.HAND_OVER;
    }

    // Ultima! si dichiara durante il proprio turno, prima di giocare la penultima carta o mentre si sceglie il colore.
    private void executeCallLastCard(CallLastCard move) {
        if (state != GameState.IN_PROGRESS && state != GameState.AWAITING_COLOR) {
            throw new IllegalStateException("The game is not in progress.");
        }
        Player player = getActingPlayer();
        if (!player.getName().equals(move.playerName())) {
            throw new IllegalArgumentException("It is not " + move.playerName() + "'s turn.");
        }
        if (player.getHand().size() > 2) {
            throw new IllegalStateException("The last card can only be called with two or fewer cards in hand.");
        }
        player.callLastCard();
        notifyObservers(GameEvent.lastCardCalled(player.getName()));
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
        return Optional.ofNullable(winner);
    }


    public Player getNextPlayer() {
        ensureInProgress();
        return players.get(stepFrom(currentIndex, turnStep()));
    }

    public void awaitColorChoice() {
        // Una penalità può aver eliminato l'ultimo avversario: la mano è già chiusa, non c'è colore da scegliere.
        if (isHandClosed()) {
            return;
        }
        ensureInProgress();
        state = GameState.AWAITING_COLOR;
    }

    // Il prossimo giocatore pesca n carte e perde il turno.
    // Con l'accumulo del No Mercy invece la penalità si somma e il prossimo deve rilanciarla o pescare il totale.
    public void penalizeNext(int n) {
        Player next = getNextPlayer();
        if (rules.drawStacking()) {
            pendingDraw += n;
            // L'evento nomina chi ha giocato la carta Pesca, non chi dovrà rispondere.
            notifyObservers(GameEvent.penaltyStacked(lastPlayer.getName(), pendingDraw));
            return;
        }
        List<Card> drawn = drawCards(next, n);
        notifyObservers(GameEvent.cardsDrawn(next.getName(), drawn.size()));
        // Se è stato eliminato è già fuori dal giro: saltarlo farebbe perdere il turno anche al successivo.
        if (!eliminateIfOverThreshold(next) && state == GameState.IN_PROGRESS) {
            skipNext();
        }
    }

    // Per i test: le carte in mani, mazzo e scarti. Deve restare costante per tutta la partita.
    // Le mani degli eliminati tornano negli scarti, quindi sono già contate lì.
    int totalCardCount() {
        int inHands = players.stream().mapToInt(player -> player.getHand().size()).sum();
        return inHands + deck.getCardCount() + discardPile.getCardCount();
    }
}

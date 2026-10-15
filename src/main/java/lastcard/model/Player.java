package lastcard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player {
    private final String name;
    private final List<Card> hand;
    private boolean lastCardCalled;
    // No Mercy: chi arriva alla soglia di carte esce dalla mano.
    private boolean eliminated;
    // Partita a punti: i punti presi chiudendo le mani.
    private int score;

    public Player(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.hand = new ArrayList<>();
        this.lastCardCalled = false;
        this.eliminated = false;
        this.score = 0;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public int getScore() {
        return score;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    public void addCard(Card card) {
        hand.add(Objects.requireNonNull(card, "card"));
    }

    public Card removeCard(int handIndex) {
        return hand.remove(handIndex);
    }

    public List<Card> playableCards(Card top, CardColor activeColor) {
        Objects.requireNonNull(top, "top");
        Objects.requireNonNull(activeColor, "activeColor");

        List<Card> playable = new ArrayList<>();
        for (Card card : hand) {
            if (card.isPlayable(top, activeColor)) {
                playable.add(card);
            }
        }
        return List.copyOf(playable);
    }

    // Chi è stato eliminato ha la mano vuota ma non ha vinto.
    public boolean hasWon() {
        return !eliminated && hand.isEmpty();
    }

    // No Mercy: il giocatore esce dalla mano e restituisce le sue carte, che tornano negli scarti.
    List<Card> eliminate() {
        eliminated = true;
        return takeHand();
    }

    // Partita a punti: l'eliminazione vale per una mano sola, alla mano successiva si rientra in gioco.
    void rejoin() {
        eliminated = false;
    }

    // Toglie e restituisce tutta la mano: serve allo scambio del 7 e alla rotazione dello 0.
    List<Card> takeHand() {
        List<Card> cards = new ArrayList<>(hand);
        hand.clear();
        return cards;
    }

    void addCards(List<Card> cards) {
        for (Card card : cards) {
            addCard(card);
        }
    }

    // Partita a punti: i punti presi chiudendo una mano si sommano a quelli delle mani precedenti.
    void addScore(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("points cannot be negative.");
        }
        score = score + points;
    }

    public int handPoints() {
        return hand.stream().mapToInt(Card::points).sum();
    }

    public void callLastCard() {
        lastCardCalled = true;
    }

    public boolean hasCalledLastCard() {
        return lastCardCalled;
    }

    public void resetLastCardCall() {
        lastCardCalled = false;
    }
}

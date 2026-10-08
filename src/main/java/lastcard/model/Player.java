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

    public boolean hasWon() {
        return hand.isEmpty();
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

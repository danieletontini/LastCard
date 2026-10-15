package lastcard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Deck {

    private final List<Card> cards;
    private final Random random;

    public Deck(List<Card> cards) {
        this(cards, new Random());
    }

    // Con un Random dato da fuori (per esempio con un seme fisso) i mescolamenti si ripetono uguali: partite riproducibili.
    public Deck(List<Card> cards, Random random) {
        this.cards = new ArrayList<>(cards);
        this.random = Objects.requireNonNull(random, "random");
    }

    public List<Card> getCards() {
        return List.copyOf(cards);
    }

    public void shuffle() {
        Collections.shuffle(cards, random);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("The deck is empty: there are no cards to draw.");
        }
        return cards.remove(cards.size() - 1);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int getCardCount() {
        return cards.size();
    }

    // Rimette nel mazzo gli scarti, tranne la carta in cima, e rimescola.
    public void refillFrom(DiscardPile discardPile) {
        cards.addAll(discardPile.takeAllButTop());
        shuffle();
    }

    // Partita a punti: a fine mano le carte delle mani e degli scarti tornano nel mazzo. Non mescola.
    public void addCards(List<Card> added) {
        Objects.requireNonNull(added, "added");
        for (Card card : added) {
            cards.add(Objects.requireNonNull(card, "card"));
        }
    }

}

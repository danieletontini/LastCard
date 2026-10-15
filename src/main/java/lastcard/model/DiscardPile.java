package lastcard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiscardPile {

    /** La carta in cima è l'ultima della lista. */
    private final List<Card> cards;

    public DiscardPile() {
        this.cards = new ArrayList<>();
    }

    public void discard(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    /**
     * Mette le carte sotto la cima, che resta la stessa: servono al No Mercy per le carte di Scarta Tutto
     * e per la mano di chi viene eliminato, che così tornano in gioco alla prossima rigenerazione del mazzo.
     */
    public void addUnderTop(List<Card> added) {
        Objects.requireNonNull(added, "added");
        int position = cards.isEmpty() ? 0 : cards.size() - 1;
        for (Card card : added) {
            cards.add(position, Objects.requireNonNull(card, "card"));
            position++;
        }
    }

    public Card top() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("The discard pile is empty.");
        }
        return cards.get(cards.size() - 1);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int getCardCount() {
        return cards.size();
    }

    /** Toglie e restituisce tutte le carte tranne la cima, che resta sulla pila. */
    public List<Card> takeAllButTop() {
        if (cards.size() <= 1) {
            return new ArrayList<>();
        }
        Card top = cards.remove(cards.size() - 1);
        List<Card> rest = new ArrayList<>(cards);
        cards.clear();
        cards.add(top);
        return rest;
    }

    /** Toglie e restituisce tutte le carte, cima compresa: serve a fine mano nella partita a punti. */
    public List<Card> takeAll() {
        List<Card> all = new ArrayList<>(cards);
        cards.clear();
        return all;
    }
}

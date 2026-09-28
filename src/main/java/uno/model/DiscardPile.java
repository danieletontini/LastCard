package uno.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiscardPile {

    /** La carta in cima è l'ultima della lista. */
    private final List<Card> cards = new ArrayList<>();

    public void discard(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    public Card top() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("La pila degli scarti è vuota.");
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
}

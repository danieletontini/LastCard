package uno.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DeckFactory {

    private DeckFactory() {
    }

    /** Il mazzo indicato dalle regole. La scelta è sul tipo di mazzo, non sul tipo di carta. */
    public static Deck create(GameRules rules) {
        return create(rules, new Random());
    }

    /** Come sopra, con il Random che il mazzo userà per mescolare. */
    public static Deck create(GameRules rules, Random random) {
        if (rules.deck() == DeckType.NO_MERCY) {
            return createNoMercy(random);
        }
        return createClassic(random);
    }

    // Mazzo No Mercy da 168 carte: si scrive quando ci sono le carte nuove.
    public static Deck createNoMercy() {
        return createNoMercy(new Random());
    }

    public static Deck createNoMercy(Random random) {
        throw new UnsupportedOperationException("Il mazzo No Mercy non è ancora implementato.");
    }

    // Mazzo classico da 108 carte
    public static Deck createClassic() {
        return createClassic(new Random());
    }

    public static Deck createClassic(Random random) {
        List<Card> cards = new ArrayList<>();

        for (CardColor color : CardColor.values()) {
            if (color == CardColor.BLACK) {
                continue;
            }
            cards.add(new NumberCard(color, 0));
            for (int i = 0; i < 2; i++) {
                for (int number = 1; number <= 9; number++) {
                    cards.add(new NumberCard(color, number));
                }
                cards.add(new Skip(color));
                cards.add(new Reverse(color));
                cards.add(new DrawTwo(color));
            }
        }

        for (int j = 0; j < 4; j++) {
            cards.add(new Wild());
            cards.add(new WildDrawFour());
        }

        return new Deck(cards, random);
    }
}

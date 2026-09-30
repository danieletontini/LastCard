package uno.model;

import java.util.ArrayList;
import java.util.List;

public final class DeckFactory {

    private DeckFactory() {
    }

    // Mazzo classico da 108 carte
    public static Deck createClassic() {
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

        return new Deck(cards);
    }
}

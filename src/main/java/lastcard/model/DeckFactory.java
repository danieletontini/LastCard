package lastcard.model;

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

    
    private static final int NO_MERCY_NUMBER_COPIES = 2;        // ogni numero da 0 a 9: 80 carte
    private static final int NO_MERCY_SKIP = 3;                 // 12
    private static final int NO_MERCY_REVERSE = 3;              // 12
    private static final int NO_MERCY_DRAW_TWO = 3;             // 12
    private static final int NO_MERCY_DRAW_FOUR = 2;            // 8, Pesca 4 colorato
    private static final int NO_MERCY_SKIP_EVERYONE = 2;        // 8
    private static final int NO_MERCY_DISCARD_ALL = 3;          // 12
   
    private static final int NO_MERCY_WILD_REVERSE_DRAW_FOUR = 8;
    private static final int NO_MERCY_WILD_DRAW_SIX = 4;
    private static final int NO_MERCY_WILD_DRAW_TEN = 4;
    private static final int NO_MERCY_WILD_COLOR_ROULETTE = 8;

    public static Deck createNoMercy() {
        return createNoMercy(new Random());
    }

    public static Deck createNoMercy(Random random) {
        List<Card> cards = new ArrayList<>();

        for (CardColor color : CardColor.values()) {
            if (color == CardColor.BLACK) {
                continue;
            }
            for (int copy = 0; copy < NO_MERCY_NUMBER_COPIES; copy++) {
                for (int number = 0; number <= 9; number++) {
                    cards.add(new NumberCard(color, number));
                }
            }
            for (int i = 0; i < NO_MERCY_SKIP; i++) {
                cards.add(new Skip(color));
            }
            for (int i = 0; i < NO_MERCY_REVERSE; i++) {
                cards.add(new Reverse(color));
            }
            for (int i = 0; i < NO_MERCY_DRAW_TWO; i++) {
                cards.add(new DrawTwo(color));
            }
            for (int i = 0; i < NO_MERCY_DRAW_FOUR; i++) {
                cards.add(new ColorDrawFour(color));
            }
            for (int i = 0; i < NO_MERCY_SKIP_EVERYONE; i++) {
                cards.add(new SkipEveryone(color));
            }
            for (int i = 0; i < NO_MERCY_DISCARD_ALL; i++) {
                cards.add(new DiscardAll(color));
            }
        }

        for (int i = 0; i < NO_MERCY_WILD_REVERSE_DRAW_FOUR; i++) {
            cards.add(new WildReverseDrawFour());
        }
        for (int i = 0; i < NO_MERCY_WILD_DRAW_SIX; i++) {
            cards.add(new WildDrawSix());
        }
        for (int i = 0; i < NO_MERCY_WILD_DRAW_TEN; i++) {
            cards.add(new WildDrawTen());
        }
        // Quando ci sarà il Jolly Rimescola Mani (una copia) prenderà il posto di una Roulette, per restare a 168.
        for (int i = 0; i < NO_MERCY_WILD_COLOR_ROULETTE; i++) {
            cards.add(new WildColorRoulette());
        }

        return new Deck(cards, random);
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

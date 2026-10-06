package uno.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Prepara partite con carte decise dal test: le mani di 7 carte, la prima carta scoperta
 * e le carte che si pescheranno dopo, in quest'ordine. Il mazzo non viene mescolato.
 */
final class GameSetup {

    /** Riempie le mani fino a 7 carte. Gialla: i test usano rosso e blu, così non è mai giocabile per caso. */
    static final Card FILLER = new NumberCard(CardColor.YELLOW, 9);

    private static final int HAND_SIZE = 7;
    private static final int DRAW_PILE_SIZE = 30;

    private GameSetup() {
    }

    /** Una mano di 7 carte: quelle date, poi riempitivi. */
    static List<Card> hand(Card... cards) {
        List<Card> hand = new ArrayList<>(List.of(cards));
        while (hand.size() < HAND_SIZE) {
            hand.add(FILLER);
        }
        return hand;
    }

    /** Partita non ancora avviata, per poter registrare un observer prima di start(). */
    static Game create(List<String> names, List<List<Card>> hands, Card firstCard) {
        List<Card> drawOrder = new ArrayList<>();
        for (List<Card> hand : hands) {
            if (hand.size() != HAND_SIZE) {
                throw new IllegalArgumentException("Every hand must have " + HAND_SIZE + " cards.");
            }
            drawOrder.addAll(hand);
        }
        drawOrder.add(firstCard);
        for (int i = 0; i < DRAW_PILE_SIZE; i++) {
            drawOrder.add(FILLER);
        }
        // Deck pesca dalla fine della lista: va rovesciata perché la prima carta pescata sia la prima della lista.
        Collections.reverse(drawOrder);
        return new Game(names, new Deck(drawOrder), false);
    }

    static Game start(List<String> names, List<List<Card>> hands, Card firstCard) {
        Game game = create(names, hands, firstCard);
        game.start();
        return game;
    }

    static int handSize(Game game, String name) {
        return game.getPlayers().stream()
                .filter(player -> player.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getHand()
                .size();
    }
}

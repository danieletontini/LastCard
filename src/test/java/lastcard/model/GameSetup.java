package lastcard.model;

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

    /** Partita non ancora avviata, per poter registrare un observer prima di start(). Si pescano riempitivi. */
    static Game create(List<String> names, List<List<Card>> hands, Card firstCard) {
        return create(names, hands, firstCard, Collections.nCopies(DRAW_PILE_SIZE, FILLER));
    }

    /** Come sopra, ma le carte da pescare dopo la distribuzione sono esattamente drawPile (anche nessuna). */
    static Game create(List<String> names, List<List<Card>> hands, Card firstCard, List<Card> drawPile) {
        List<Card> drawOrder = new ArrayList<>();
        for (List<Card> hand : hands) {
            if (hand.size() != HAND_SIZE) {
                throw new IllegalArgumentException("Every hand must have " + HAND_SIZE + " cards.");
            }
            drawOrder.addAll(hand);
        }
        drawOrder.add(firstCard);
        drawOrder.addAll(drawPile);
        // Deck pesca dalla fine della lista: va rovesciata perché la prima carta pescata sia la prima della lista.
        Collections.reverse(drawOrder);
        return new Game(names, new Deck(drawOrder), false);
    }

    static Game start(List<String> names, List<List<Card>> hands, Card firstCard) {
        Game game = create(names, hands, firstCard);
        game.start();
        return game;
    }

    static Game start(List<String> names, List<List<Card>> hands, Card firstCard, List<Card> drawPile) {
        Game game = create(names, hands, firstCard, drawPile);
        game.start();
        return game;
    }

    static Player player(Game game, String name) {
        return game.getPlayers().stream()
                .filter(player -> player.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    static int handSize(Game game, String name) {
        return player(game, name).getHand().size();
    }

    /**
     * Toglie carte dalla fine della mano finché ne restano size: serve per le situazioni con poche carte,
     * perché start() ne distribuisce sempre 7. Le carte tolte escono dal gioco, quindi dopo non vale l'invariante.
     */
    static void trimHand(Game game, String name, int size) {
        Player player = player(game, name);
        while (player.getHand().size() > size) {
            player.removeCard(player.getHand().size() - 1);
        }
    }
}

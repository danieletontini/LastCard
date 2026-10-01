package uno.view.console;

import java.util.List;
import java.util.Map;

import uno.model.Card;
import uno.model.CardColor;

/** Trasforma carte e colori in testo per il terminale: colore ANSI più lettera, mai solo il colore. */
public final class CardFormatter {

    private static final String RESET = "\u001B[0m";

    private static final Map<CardColor, String> ANSI = Map.of(
            CardColor.RED, "\u001B[31m",
            CardColor.YELLOW, "\u001B[33m",
            CardColor.GREEN, "\u001B[32m",
            CardColor.BLUE, "\u001B[34m",
            CardColor.BLACK, "\u001B[1m");

    private static final Map<CardColor, String> NAMES = Map.of(
            CardColor.RED, "Rosso",
            CardColor.YELLOW, "Giallo",
            CardColor.GREEN, "Verde",
            CardColor.BLUE, "Blu",
            CardColor.BLACK, "Nero");

    // Testo mostrato per i simboli interni delle carte; numeri e +2 restano come sono.
    private static final Map<String, String> LABELS = Map.of(
            "SKIP", "Salta",
            "REVERSE", "Inverti",
            "WILD", "Jolly",
            "+4", "Jolly +4");

    /** Per esempio [R 5] in rosso, [B Salta] in blu, [Jolly +4]. */
    public String format(Card card) {
        String label = LABELS.getOrDefault(card.symbol(), card.symbol());
        String text = card.getColor() == CardColor.BLACK
                ? "[" + label + "]"
                : "[" + initial(card.getColor()) + " " + label + "]";
        return paint(card.getColor(), text);
    }

    /** La mano con gli indici da digitare: 0:[R 5]  1:[B Salta]  2:[Jolly]. */
    public String formatHand(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(i).append(':').append(format(hand.get(i)));
        }
        return sb.toString();
    }

    /** Il nome del colore, colorato: serve per il colore attivo e per la scelta dopo un jolly. */
    public String formatColor(CardColor color) {
        return paint(color, NAMES.get(color));
    }

    private static String initial(CardColor color) {
        return NAMES.get(color).substring(0, 1);
    }

    private String paint(CardColor color, String text) {
        return ANSI.get(color) + text + RESET;
    }
}

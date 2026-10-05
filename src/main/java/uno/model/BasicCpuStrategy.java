package uno.model;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategia base: gioca sempre se può, tiene i jolly per ultimi e tra le altre carte
 * si libera prima di quelle che valgono più punti. Dopo un jolly dichiara il colore che ha di più.
 */
public class BasicCpuStrategy implements CpuStrategy {

    private static final List<CardColor> CHOOSABLE_COLORS = List.of(
            CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE);

    @Override
    public Optional<Card> choose(List<Card> playable) {
        Optional<Card> colored = playable.stream()
                .filter(card -> card.getColor() != CardColor.BLACK)
                .max(Comparator.comparingInt(Card::points));
        if (colored.isPresent()) {
            return colored;
        }
        return playable.stream().findFirst();
    }

    @Override
    public CardColor chooseColor(List<Card> hand) {
        Map<CardColor, Integer> counts = new EnumMap<>(CardColor.class);
        for (Card card : hand) {
            if (card.getColor() != CardColor.BLACK) {
                counts.merge(card.getColor(), 1, Integer::sum);
            }
        }
        // A parità vince il primo nell'ordine dei colori; con solo jolly in mano, il rosso.
        CardColor best = CHOOSABLE_COLORS.get(0);
        for (CardColor color : CHOOSABLE_COLORS) {
            if (counts.getOrDefault(color, 0) > counts.getOrDefault(best, 0)) {
                best = color;
            }
        }
        return best;
    }
}

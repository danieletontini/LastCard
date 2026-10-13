package lastcard.model;

import java.util.Objects;

/**
 * Quando una carta si può giocare, tenendo conto della penalità accumulata del No Mercy.
 * È in un punto solo perché la usano sia Game, per validare le mosse, sia la CPU, per sceglierle:
 * se le due regole divergessero, la CPU proverebbe mosse che Game rifiuta.
 */
public final class PlayRules {

    private PlayRules() {
    }

    /**
     * Senza penalità in corso vale la regola della carta (colore, simbolo, jolly).
     * Con una penalità in corso si può solo rilanciare, con una carta Pesca di valore uguale o superiore
     * a quella in cima, di qualunque colore: tutte le altre carte sono bloccate finché non si pesca il totale.
     */
    public static boolean canPlay(Card card, Card top, CardColor activeColor, int pendingDraw) {
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(top, "top");
        if (pendingDraw > 0) {
            return card.drawValue() > 0 && card.drawValue() >= top.drawValue();
        }
        return card.isPlayable(top, activeColor);
    }
}

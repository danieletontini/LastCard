package lastcard.model;

import java.util.Objects;

/**
 * Tutto ciò che serve per disegnare una carta, e nient'altro.
 *
 * <p>È il contratto fra modello e vista: la carta sa descriversi, la vista sa
 * disegnare una descrizione. Il modello non importa niente dalla vista.</p>
 *
 * @param color  colore della carta; {@link CardColor#BLACK} per jolly e pesca quattro
 * @param symbol cosa mostra al centro
 * @param label  cosa scrivere negli angoli ("7", "+2", "+4"); vuota se il simbolo
 *               non ha testo, come inverti, salta turno e jolly
 */
public record CardFace(CardColor color, FaceSymbol symbol, String label) {

    public CardFace {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(symbol, "symbol");
        if (label == null) {
            label = "";
        }
    }

    /** Scorciatoia per i simboli che non portano testo negli angoli. */
    public static CardFace of(CardColor color, FaceSymbol symbol) {
        return new CardFace(color, symbol, "");
    }
}

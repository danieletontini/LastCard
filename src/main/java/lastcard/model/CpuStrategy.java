package lastcard.model;

import java.util.List;
import java.util.Optional;

/** Come gioca una CPU: quale carta scegliere e quale colore dichiarare dopo un jolly. */
public interface CpuStrategy {

    /** Sceglie una carta tra quelle giocabili, oppure nessuna per pescare (o passare dopo aver pescato). */
    Optional<Card> choose(List<Card> playable);

    /** Il colore da dichiarare dopo un jolly, mai BLACK. */
    CardColor chooseColor(List<Card> hand);

    /** No Mercy, scambio 7: il nome dell'avversario con cui scambiare la mano, scelto tra opponents (mai vuota). */
    String chooseSwapTarget(List<PlayerSummary> opponents);
}

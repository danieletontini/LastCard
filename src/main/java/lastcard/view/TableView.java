package lastcard.view;

import java.util.List;

import lastcard.controller.TableActions;
import lastcard.model.GameObserver;
import lastcard.model.PlayerSummary;
import lastcard.model.VisibleState;

/**
 * Ciò che il controller della partita chiede al tavolo grafico. Il tavolo riceve anche gli eventi della
 * partita, come observer: li usa per raccontare cosa è successo e per sapere il senso di gioco.
 * A differenza di GameView le scelte non restituiscono un valore: il tavolo apre il form e la risposta
 * arriva più tardi a TableActions, quando il giocatore preme un pulsante.
 */
public interface TableView extends GameObserver {

    /** Una partita nuova: il tavolo si azzera, mostra la mano di viewerName e manda le azioni a actions. */
    void reset(String viewerName, TableActions actions);

    /** Lo stato visto dal giocatore umano, dopo ogni mossa di chiunque. */
    void showState(VisibleState state);

    /** Apre il form della scelta del colore. Con roulette vero, il colore lo si sceglie per la Roulette di chi precede. */
    void askColor(boolean roulette);

    /** No Mercy, scambio 7: apre il form con gli avversari con cui si può scambiare la mano (mai vuoto). */
    void askSwapTarget(List<PlayerSummary> opponents);

    /** Una mossa rifiutata, con il motivo in italiano. */
    void showError(String message);

    /** La partita è finita. */
    void showWinner(String winnerName);
}

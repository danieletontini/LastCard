package lastcard.view;

import java.util.List;

import lastcard.model.CardColor;
import lastcard.model.VisibleState;

/** Ciò che il controller chiede a una vista, console o grafica che sia. */
public interface GameView {

    void showState(VisibleState state);

    CardColor askColor();

    /** No Mercy, scambio 7: con chi scambiare la mano, scelto fra opponents (mai vuota). */
    String askSwapTarget(List<String> opponents);

    void showError(String message);

    void showWinner(String name);
}

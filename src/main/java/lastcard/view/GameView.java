package lastcard.view;

import lastcard.model.CardColor;
import lastcard.model.VisibleState;

/** Ciò che il controller chiede a una vista, console o grafica che sia. */
public interface GameView {

    void showState(VisibleState state);

    CardColor askColor();

    void showError(String message);

    void showWinner(String name);
}

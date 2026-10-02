package uno.view;

import uno.model.CardColor;
import uno.model.VisibleState;

/** Ciò che il controller chiede a una vista, console o grafica che sia. */
public interface UnoView {

    void showState(VisibleState state);

    CardColor askColor();

    void showError(String message);

    void showWinner(String name);
}

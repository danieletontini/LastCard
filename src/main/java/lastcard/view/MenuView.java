package lastcard.view;

import lastcard.controller.GameSettings;

/** Ciò che il controller del menu chiede alla vista grafica. */
public interface MenuView {

    /** Mostra il tavolo della partita appena creata con queste impostazioni. */
    void showTable(GameSettings settings);
}

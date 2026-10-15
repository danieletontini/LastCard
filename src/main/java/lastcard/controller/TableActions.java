package lastcard.controller;

import lastcard.model.CardColor;

/**
 * Le azioni che il tavolo grafico può chiedere al controller della partita, una per gesto del giocatore.
 * Il tavolo non vede mai Game: manda queste richieste e riceve lo stato visibile e gli eventi.
 */
public interface TableActions {

    /** Gioca la carta in posizione index della propria mano. */
    void onCardSelected(int index);

    /** Pesca (con una penalità in corso, la prende tutta). */
    void onDraw();

    /** Tiene la carta appena pescata e passa il turno. */
    void onPass();

    /** Dichiara Ultima! */
    void onLastCardCalled();

    /** Il colore scelto nel form, dopo un proprio jolly o per la Roulette di chi precede. */
    void onColorChosen(CardColor color);

    /** No Mercy, scambio 7: l'avversario scelto nel form. */
    void onSwapTargetChosen(String targetName);

    /** Partita a punti: dopo i punteggi di fine mano, si distribuisce la mano successiva. */
    void onNextHand();

    /** Si lascia il tavolo: la partita si ferma e le CPU smettono di giocare. */
    void onLeaveTable();
}

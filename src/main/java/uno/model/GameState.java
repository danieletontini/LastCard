package uno.model;

public enum GameState {
    NOT_STARTED,
    IN_PROGRESS,
    AWAITING_COLOR,
    /** No Mercy: chi ha giocato un 7 deve scegliere con chi scambiare la mano (mossa SwapHands). */
    AWAITING_SWAP_TARGET,
    /** No Mercy: dopo una Roulette il giocatore successivo deve scegliere il colore (mossa ChooseColor). */
    AWAITING_ROULETTE_COLOR,
    FINISHED
}

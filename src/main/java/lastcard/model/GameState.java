package lastcard.model;

public enum GameState {
    NOT_STARTED,
    IN_PROGRESS,
    AWAITING_COLOR, 
    AWAITING_SWAP_TARGET,//mod no mercy, mossa SwapHands
    AWAITING_ROULETTE_COLOR,//mod no mercy, mossa ChooseColor
    
    HAND_OVER,//mod a punti, mano terminata ma punteggio non raggiunto
    FINISHED
}

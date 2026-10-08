package uno.model;

public sealed interface Move permits PlayCard, Draw, Pass, CallUno, ChooseColor, SwapHands {
    String playerName();
}

package lastcard.model;

public sealed interface Move permits PlayCard, Draw, Pass, CallLastCard, ChooseColor, SwapHands {
    String playerName();
}

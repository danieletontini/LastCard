package uno.model;

public sealed interface Move permits PlayCard, Draw, CallUno, ChooseColor {
    String playerName();
}

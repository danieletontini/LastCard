package uno.model;

public sealed interface Move permits PlayCard, Draw, Pass, CallUno, ChooseColor {
    String playerName();
}

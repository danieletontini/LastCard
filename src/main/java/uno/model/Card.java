package uno.model;

public abstract class Card {

    private final CardColor color;

    public Card(CardColor color) {
        this.color = color;
    }

    public CardColor getColor() {
        return color;
    }

    public abstract String symbol();

    public abstract int points();
}

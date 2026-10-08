package lastcard.model;

public class NumberCard extends Card {

    private final int number;

    public NumberCard(CardColor color, int number) {
        super(color);
        if (number < 0 || number > 9) {
            throw new IllegalArgumentException("The card number must be between 0 and 9.");
        }
        if (color == CardColor.BLACK) {
            throw new IllegalArgumentException("A number card cannot be BLACK.");
       }

        this.number = number;
    }

    @Override
    public String symbol() {
        return String.valueOf(number);
    }

    @Override
    public int points() {
        return number;
    }

}

package uno.model;

public class NumberCard extends Card {

    private final int number;

    public NumberCard(CardColor color, int number) {
        super(color);
        if (number < 0 || number > 9) {
            throw new IllegalArgumentException("Il numero della carta deve essere compreso tra 0 e 9.");
        }
        if (color == CardColor.BLACK) {
            throw new IllegalArgumentException("Una carta numero non può essere nera.");
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

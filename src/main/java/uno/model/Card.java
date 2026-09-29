package uno.model;

import java.util.Objects;

public abstract class Card {

    private final CardColor color;

    public Card(CardColor color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    public CardColor getColor() {
        return color;
    }

    public abstract String symbol();

    public abstract int points();

    /** Giocabile se ha il colore attivo o lo stesso simbolo della carta in cima. I jolly la ridefiniscono. */
    public boolean isPlayable(Card top, CardColor activeColor) {
        return color == activeColor || symbol().equals(top.symbol());
    }

    /** Effetto della carta una volta giocata: di base nessuno. */
    public void applyEffect(Game game) {
    }
   /* verra fatto override con i valori giusti quando implementeremo l*/
    public int drawValue() {
        return 0;
    }

    /*due carte sono uguali se hanno la stessa classe,colore,simbolo */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Card other = (Card) o;
        return color == other.color && symbol().equals(other.symbol());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getClass(), color, symbol());
    }

    @Override
    public String toString() {
        return color + " " + symbol();
    }
}

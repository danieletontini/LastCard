package uno.model;

public class WildDrawFour extends WildCard {

    public WildDrawFour() {
        super();
    }

    public int drawValue() {
        return 4;
    }

    @Override
    public String symbol() {
        return "+4";
    }

    //implementare applyEffect() per far pescare 4 carte al giocatore successivo

}

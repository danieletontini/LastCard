package uno.model;

public class WildDrawFour extends WildCard {

    public WildDrawFour() {
        super();
    }

    @Override
    public int drawValue() {
        return 4;
    }

    @Override
    public String symbol() {
        return "+4";
    }

    @Override
    public void applyEffect(Game game) {
        game.penalizeNext(drawValue());
        game.awaitColorChoice();
    }

}

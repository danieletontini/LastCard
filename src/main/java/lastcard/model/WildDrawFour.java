package lastcard.model;

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

    @Override
    public CardFace face() {
        return new CardFace(CardColor.BLACK, FaceSymbol.WILD_DRAW_FOUR, "+4");
    }
}

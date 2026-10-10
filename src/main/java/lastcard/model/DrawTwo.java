package lastcard.model;

public class DrawTwo extends ActionCard {
    public DrawTwo(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "+2";
    }

    @Override 
    public int drawValue() {
        return 2;
    }

    @Override
    public void applyEffect(Game game) {
        game.penalizeNext(drawValue());
    }

    @Override
    public CardFace face() {
        return new CardFace(getColor(), FaceSymbol.DRAW_TWO, "+2");
    }
}

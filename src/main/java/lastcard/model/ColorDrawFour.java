package lastcard.model;

// No Mercy: un Pesca 4 colorato. Il giocatore successivo pesca quattro carte e salta il turno.
public class ColorDrawFour extends ActionCard {

    public ColorDrawFour(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "DRAW_FOUR";
    }

    @Override
    public int drawValue() {
        return 4;
    }

    @Override
    public void applyEffect(Game game) {
        game.penalizeNext(drawValue());
    }

    @Override
    public CardFace face() {
        return new CardFace(getColor(), FaceSymbol.DRAW_FOUR, "+4");
    }
}

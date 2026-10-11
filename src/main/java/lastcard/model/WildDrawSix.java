package lastcard.model;

// No Mercy: chi lo gioca sceglie il colore; il giocatore successivo pesca sei carte e salta il turno.
public class WildDrawSix extends WildCard {

    public WildDrawSix() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD_DRAW_SIX";
    }

    @Override
    public int drawValue() {
        return 6;
    }

    @Override
    public void applyEffect(Game game) {
        game.penalizeNext(drawValue());
        game.awaitColorChoice();
    }

    @Override
    public CardFace face() {
        return new CardFace(CardColor.BLACK, FaceSymbol.WILD_DRAW_SIX, "+6");
    }
}

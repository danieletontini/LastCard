package lastcard.model;

// No Mercy: chi lo gioca sceglie il colore; il giocatore successivo pesca dieci carte e salta il turno.
public class WildDrawTen extends WildCard {

    public WildDrawTen() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD_DRAW_TEN";
    }

    @Override
    public int drawValue() {
        return 10;
    }

    @Override
    public void applyEffect(Game game) {
        game.penalizeNext(drawValue());
        game.awaitColorChoice();
    }

    @Override
    public CardFace face() {
        return new CardFace(CardColor.BLACK, FaceSymbol.WILD_DRAW_TEN, "+10");
    }
}

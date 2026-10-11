package lastcard.model;

/**
 * No Mercy: chi lo gioca sceglie il colore e inverte il senso di gioco; il nuovo giocatore successivo
 * pesca quattro carte e salta il turno.
 */
public class WildReverseDrawFour extends WildCard {

    public WildReverseDrawFour() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD_REVERSE_DRAW_FOUR";
    }

    @Override
    public int drawValue() {
        return 4;
    }

    @Override
    public void applyEffect(Game game) {
        game.flipDirection();
        game.penalizeNext(drawValue());
        game.awaitColorChoice();
    }

    @Override
    public CardFace face() {
        return new CardFace(CardColor.BLACK, FaceSymbol.WILD_REVERSE_DRAW_FOUR, "+4");
    }
}

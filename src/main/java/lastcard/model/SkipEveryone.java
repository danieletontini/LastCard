package lastcard.model;

// No Mercy: tutti gli altri giocatori saltano il turno, e tocca di nuovo a chi l'ha giocata.
public class SkipEveryone extends ActionCard {

    public SkipEveryone(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "SKIP_EVERYONE";
    }

    @Override
    public void applyEffect(Game game) {
        game.skipAll();
    }

    @Override
    public CardFace face() {
        return CardFace.of(getColor(), FaceSymbol.SKIP_EVERYONE);
    }
}

package lastcard.model;

// No Mercy: chi la gioca scarta insieme a lei tutte le carte del suo colore che ha in mano.
public class DiscardAll extends ActionCard {

    public DiscardAll(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "DISCARD_ALL";
    }

    @Override
    public void applyEffect(Game game) {
        game.discardAllOfColor(getColor());
    }

    @Override
    public CardFace face() {
        return CardFace.of(getColor(), FaceSymbol.DISCARD_ALL);
    }
}

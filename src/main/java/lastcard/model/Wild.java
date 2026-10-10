package lastcard.model;

public class Wild extends WildCard {

    public Wild() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD";
    }

    @Override
    public void applyEffect(Game game) {
        game.awaitColorChoice();
    }

    @Override
    public CardFace face() {
        return CardFace.of(CardColor.BLACK, FaceSymbol.WILD);
    }
}

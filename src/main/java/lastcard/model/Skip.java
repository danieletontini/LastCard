package lastcard.model;

public class Skip extends ActionCard {
    public Skip(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "SKIP";
    }
    //salta il giocatore
    @Override 
    public void applyEffect(Game game){
        game.skipNext();
    }

    @Override
    public CardFace face() {
        return CardFace.of(getColor(), FaceSymbol.SKIP);
    }
}

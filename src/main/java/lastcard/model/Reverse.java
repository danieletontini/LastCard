package lastcard.model;

public class Reverse extends ActionCard {
    public Reverse(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "REVERSE";
    }
    
    //cambia il senso di gioco
    @Override 
    public void applyEffect(Game game){
        game.reverseDirection();
    }

    @Override
    public CardFace face() {
        return CardFace.of(getColor(), FaceSymbol.REVERSE);
    }
}

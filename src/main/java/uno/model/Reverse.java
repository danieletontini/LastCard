package uno.model;

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

}

package uno.model;

public class DrawTwo extends ActionCard {
    public DrawTwo(CardColor color) {
        super(color);
    }

    @Override
    public String symbol() {
        return "+2";
    }

    @Override 
    public int drawValue() {
        return 2;
    }
    //implementare applyEffect() per far pescare 2 carte al giocatore successivo
    
    
}

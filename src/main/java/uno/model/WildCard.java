package uno.model;

public abstract class WildCard extends Card {

    private static final int POINTS = 50;

    public WildCard() {
        super(CardColor.BLACK);
    }

    @Override
    public int points() {
        return POINTS;
    }
    
    @Override 
    public boolean isPlayable(Card top, CardColor activeColor) {
        return true; // I jolly sono sempre giocabili
    }

    public abstract String symbol();

}

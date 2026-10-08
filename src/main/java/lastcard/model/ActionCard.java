package lastcard.model;

public abstract class ActionCard extends Card {

    private static final int POINTS = 20;

    public ActionCard(CardColor color) {
        super(color);
        /*le carte non possono essere nere */
        if (color == CardColor.BLACK) {
            throw new IllegalArgumentException("An action card cannot be BLACK.");
        }
        
    }

    /*ogni carta azione andra a fare l'override di questo metodo */
    /*le classi sono:salta turno,cambiogiro,pesca 2 e pesca 4 e il cambio colore*/ 
    public abstract String symbol();

    @Override
    public int points(){

        return POINTS;
    };

}

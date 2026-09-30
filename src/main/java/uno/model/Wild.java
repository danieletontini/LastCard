package uno.model;

public class Wild extends WildCard {

    public Wild() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD";
    }
    //implementare applyEffect() per far passare allo stato AWAITING_COLOR e far cambiare il colore al giocatore successivo
}

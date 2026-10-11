package lastcard.model;

// No Mercy: il giocatore successivo sceglie un colore e scopre carte dal mazzo finché non esce quel colore.
public class WildColorRoulette extends WildCard {

    public WildColorRoulette() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD_COLOR_ROULETTE";
    }

    @Override
    public void applyEffect(Game game) {
        game.startColorRoulette();
    }

    @Override
    public CardFace face() {
        return CardFace.of(CardColor.BLACK, FaceSymbol.WILD_COLOR_ROULETTE);
    }
}

package lastcard.model;

// No Mercy, carta nostra: le mani dei giocatori ancora in gioco si scambiano a caso, senza che nessuno
// riceva la propria. Poi chi l'ha giocata sceglie il colore.
public class WildShuffleHands extends WildCard {

    public WildShuffleHands() {
        super();
    }

    @Override
    public String symbol() {
        return "WILD_SHUFFLE_HANDS";
    }

    @Override
    public void applyEffect(Game game) {
        game.shuffleHands();
        game.awaitColorChoice();
    }

    @Override
    public CardFace face() {
        return CardFace.of(CardColor.BLACK, FaceSymbol.WILD_SHUFFLE_HANDS);
    }
}

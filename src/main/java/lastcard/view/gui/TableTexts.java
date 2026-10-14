package lastcard.view.gui;

import java.util.EnumMap;
import java.util.Map;

import lastcard.model.Card;
import lastcard.model.CardColor;
import lastcard.model.CardFace;
import lastcard.model.FaceSymbol;
import lastcard.model.GameEvent;

/**
 * I testi in italiano del tavolo: il nome delle carte e dei colori, e la frase che racconta l'ultimo evento.
 * Quando l'evento riguarda chi guarda il tavolo si usa la seconda persona ("Peschi 2 carte").
 * Sono testi della vista: il modello manda solo eventi e dati, mai frasi.
 */
final class TableTexts {

    private static final Map<CardColor, String> COLORS = new EnumMap<>(Map.of(
            CardColor.RED, "rosso",
            CardColor.YELLOW, "giallo",
            CardColor.GREEN, "verde",
            CardColor.BLUE, "blu",
            CardColor.BLACK, "nero"));

    private static final Map<FaceSymbol, String> SYMBOLS = new EnumMap<>(FaceSymbol.class);

    static {
        SYMBOLS.put(FaceSymbol.DRAW_TWO, "+2");
        SYMBOLS.put(FaceSymbol.REVERSE, "Inverti");
        SYMBOLS.put(FaceSymbol.SKIP, "Salta");
        SYMBOLS.put(FaceSymbol.WILD, "Jolly");
        SYMBOLS.put(FaceSymbol.WILD_DRAW_FOUR, "Jolly +4");
        SYMBOLS.put(FaceSymbol.DRAW_FOUR, "+4");
        SYMBOLS.put(FaceSymbol.WILD_DRAW_SIX, "Jolly +6");
        SYMBOLS.put(FaceSymbol.WILD_DRAW_TEN, "Jolly +10");
        SYMBOLS.put(FaceSymbol.WILD_REVERSE_DRAW_FOUR, "Jolly Cambia Giro +4");
        SYMBOLS.put(FaceSymbol.SKIP_EVERYONE, "Salta tutti");
        SYMBOLS.put(FaceSymbol.DISCARD_ALL, "Scarta tutto");
        SYMBOLS.put(FaceSymbol.WILD_COLOR_ROULETTE, "Jolly Roulette");
    }

    private TableTexts() {
    }

    /**
     * Il testo in maiuscolo per il font a pixel. In Press Start 2P le vocali accentate maiuscole sono disegnate
     * male (la À uguale alla A, la Ù come una ù minuscola), quindi si scrive l'apostrofo come si fa nelle
     * scritte in stampatello: "PENALITA'", "PIU'", "E'".
     */
    static String upper(String text) {
        return text.toUpperCase()
                .replace("À", "A'")
                .replace("È", "E'")
                .replace("É", "E'")
                .replace("Ì", "I'")
                .replace("Ò", "O'")
                .replace("Ù", "U'");
    }

    /** "rosso", "giallo"...; per i colori dei tag in maiuscolo basta toUpperCase. */
    static String color(CardColor color) {
        return COLORS.getOrDefault(color, "");
    }

    /** Il nome della carta: "7 rosso", "+2 blu", "Jolly +4". I jolly non dicono il colore, che è sempre nero. */
    static String card(Card card) {
        CardFace face = card.face();
        String name = face.symbol() == FaceSymbol.NUMBER ? face.label() : SYMBOLS.getOrDefault(face.symbol(), face.label());
        return face.color() == CardColor.BLACK ? name : name + " " + color(face.color());
    }

    /** "1 carta", "3 carte". */
    static String cards(int count) {
        return count == 1 ? "1 carta" : count + " carte";
    }

    /** La frase che racconta l'evento, oppure null se non c'è nulla da raccontare. */
    static String describe(GameEvent event, String viewer) {
        String name = event.playerName();
        boolean me = name != null && name.equals(viewer);
        return switch (event.type()) {
            case GAME_STARTED -> "Prima carta: " + card(event.card());
            case CARD_PLAYED -> me ? "Giochi " + card(event.card()) : name + " gioca " + card(event.card());
            case CARDS_DRAWN -> event.count() == 0 ? null
                    : me ? "Peschi " + cards(event.count()) : name + " pesca " + cards(event.count());
            case TURN_PASSED -> me ? "Passi il turno" : name + " passa il turno";
            case COLOR_CHOSEN -> me ? "Scegli il " + color(event.color()) : name + " sceglie il " + color(event.color());
            case DIRECTION_REVERSED -> "Il giro cambia senso";
            case PLAYER_SKIPPED -> me ? "Salti il turno" : name + " salta il turno";
            case LAST_CARD_CALLED -> me ? "Dichiari Ultima!" : name + ": Ultima!";
            case LAST_CARD_PENALTY -> me ? "Non hai detto Ultima!: peschi " + cards(event.count())
                    : name + " non ha detto Ultima!: pesca " + cards(event.count());
            case GAME_WON -> me ? "Hai vinto!" : name + " vince!";
            case PENALTY_STACKED -> (me ? "Rilanci" : name + " rilancia") + ": la penalità sale a " + cards(event.count());
            case ALL_SKIPPED -> me ? "Fai saltare il turno a tutti" : name + " fa saltare il turno a tutti";
            case CARDS_DISCARDED -> (me ? "Scarti" : name + " scarta") + " altre " + cards(event.count()) + " "
                    + color(event.color());
            case HANDS_SWAPPED -> swapped(event, viewer);
            case HANDS_ROTATED -> "Tutte le mani passano al giocatore successivo";
            case PLAYER_ELIMINATED -> me ? "Sei eliminato!" : name + " è eliminato";
            case HAND_WON -> (me ? "Chiudi la mano" : name + " chiude la mano") + ": " + event.count() + " punti";
        };
    }

    private static String swapped(GameEvent event, String viewer) {
        if (event.playerName().equals(viewer)) {
            return "Scambi la mano con " + event.targetName();
        }
        if (event.targetName().equals(viewer)) {
            return event.playerName() + " scambia la mano con te";
        }
        return event.playerName() + " scambia la mano con " + event.targetName();
    }
}

package lastcard.view.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lastcard.model.Card;
import lastcard.model.CardFace;
import lastcard.model.GameEvent;

/**
 * Il popup che per un secondo fa notare al tavolo una carta speciale o un effetto: "CAMBIO GIRO", "+4", "JOLLY",
 * "SALTA TUTTI". Serve solo ad accorgersi che è successo: chi pesca, quanto e che colore viene scelto restano
 * nella riga degli eventi. Fa eccezione la penalità del No Mercy, di cui si mostra il totale che sale.
 *
 * Una mossa può avere più effetti (il Jolly Cambia Giro Pesca 4 fa pescare e cambia il giro): il primo è il titolo,
 * gli altri stanno sotto, più piccoli.
 */
record EffectPopup(String title, List<String> lines) {

    EffectPopup {
        lines = List.copyOf(lines);
    }

    /** Il popup per gli eventi di una mossa, oppure niente se nessuno va fatto notare. */
    static Optional<EffectPopup> of(List<GameEvent> events, String viewer) {
        // Chi salta il turno per una penalità ha pescato: il salto lo dice già la carta Pesca.
        boolean someoneDrew = events.stream()
                .anyMatch(event -> event.type() == GameEvent.Type.CARDS_DRAWN && event.count() > 0);
        List<String> effects = new ArrayList<>();
        for (GameEvent event : events) {
            String effect = effect(event, viewer, someoneDrew);
            if (effect != null && !effects.contains(effect)) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EffectPopup(effects.get(0), effects.subList(1, effects.size())));
    }

    private static String effect(GameEvent event, String viewer, boolean someoneDrew) {
        boolean me = event.playerName() != null && event.playerName().equals(viewer);
        return switch (event.type()) {
            // anche la prima carta scoperta ha il suo effetto, come se l'avesse giocata il mazziere
            case CARD_PLAYED, GAME_STARTED -> card(event.card());
            case DIRECTION_REVERSED -> "CAMBIO GIRO";
            case PLAYER_SKIPPED -> someoneDrew ? null : "SALTO DEL TURNO";
            case ALL_SKIPPED -> "SALTA TUTTI";
            case PENALTY_STACKED -> TableTexts.upper("Penalità: " + TableTexts.cards(event.count()));
            case HANDS_SWAPPED -> "SCAMBIO DI MANI";
            case HANDS_ROTATED -> "ROTAZIONE DELLE MANI";
            case HANDS_SHUFFLED -> "MANI RIMESCOLATE";
            case PLAYER_ELIMINATED -> me ? "SEI ELIMINATO" : event.playerName().toUpperCase() + " ELIMINATO";
            default -> null;
        };
    }

    // Le carte che si fanno notare da sole. Le altre speciali (Inverti, Salta, Salta Tutti, Rimescola Mani,
    // 7 e 0 del No Mercy) si notano dall'evento del loro effetto, che arriva subito dopo.
    private static String card(Card card) {
        if (card == null) {
            return null;
        }
        CardFace face = card.face();
        return switch (face.symbol()) {
            case DRAW_TWO, DRAW_FOUR, WILD_DRAW_FOUR, WILD_DRAW_SIX, WILD_DRAW_TEN, WILD_REVERSE_DRAW_FOUR -> face.label();
            case WILD -> "JOLLY";
            case WILD_COLOR_ROULETTE -> "ROULETTE";
            case DISCARD_ALL -> "SCARTA TUTTO";
            default -> null;
        };
    }
}

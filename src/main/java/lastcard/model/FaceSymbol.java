package lastcard.model;

/**
 * Che cosa mostra una carta, in termini di dominio e non di grafica.
 *
 * Serve alla vista per sapere quale disegno usare senza interrogare il tipo
 * della carta con {@code instanceof}: ogni carta dichiara il proprio simbolo,
 * la vista lo traduce in un disegno.
 *
 * 
 */
public enum FaceSymbol {
    NUMBER,
    DRAW_TWO,
    REVERSE,
    SKIP,
    WILD,
    WILD_DRAW_FOUR,



    DRAW_FOUR,
    WILD_DRAW_SIX,
    WILD_DRAW_TEN,
    WILD_REVERSE_DRAW_FOUR,
    SKIP_EVERYONE,
    DISCARD_ALL,
    WILD_COLOR_ROULETTE
}

package lastcard.model;

/**
 * Che cosa mostra una carta, in termini di dominio e non di grafica.
 *
 * Serve alla vista per sapere quale disegno usare senza interrogare il tipo
 * della carta con {@code instanceof}: ogni carta dichiara il proprio simbolo,
 * la vista lo traduce in un disegno.
 *
 * I simboli del No Mercy si aggiungono qui insieme alle loro carte.
 */
public enum FaceSymbol {
    NUMBER,
    DRAW_TWO,
    REVERSE,
    SKIP,
    WILD,
    WILD_DRAW_FOUR
}

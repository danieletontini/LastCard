package uno.model;

/** Chi vuole sapere cosa succede nella partita: viste, log, test. Il modello non sa chi è. */
public interface GameObserver {

    void update(GameEvent event);
}

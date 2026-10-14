package lastcard.controller;

/**
 * Esegue un compito dopo un'attesa: il controller del tavolo lo usa per distanziare le mosse delle CPU.
 * È un'interfaccia perché il controller non dipenda da Swing: nella GUI la implementa un timer di Swing,
 * nei test una versione che esegue i compiti a comando, senza aspettare davvero.
 */
public interface TurnScheduler {

    /** Esegue task dopo delayMillis, sul thread della vista. Un compito già in attesa viene sostituito. */
    void schedule(int delayMillis, Runnable task);

    /** Annulla il compito in attesa, se c'è: serve quando si lascia il tavolo. */
    void cancel();
}

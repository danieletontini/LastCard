package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Partite intere tra CPU con il mazzo vero e mescolato, senza nessuna vista: invarianti che devono valere dopo
 * ogni singola mossa. Che si giochino senza vista è anche la prova che il modello non dipende dall'interfaccia.
 */
class FullGameTest {

    private static final int CLASSIC_DECK = 108;
    private static final int NO_MERCY_DECK = 168;
    private static final int MAX_MOVES = 20_000;

    private static Map<String, CpuPlayer> cpus(List<String> names) {
        Map<String, CpuPlayer> cpus = new HashMap<>();
        for (String name : names) {
            cpus.put(name, new CpuPlayer(name, new BasicCpuStrategy()));
        }
        return cpus;
    }

    private static List<String> names(int count) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            names.add("CPU " + i);
        }
        return names;
    }

    // Gioca fino alla fine controllando dopo ogni mossa che non si perdano carte; restituisce gli eventi.
    private static List<GameEvent> playToTheEnd(Game game, Map<String, CpuPlayer> cpus, int deckSize, String label) {
        List<GameEvent> events = new ArrayList<>();
        game.addObserver(events::add);
        game.start();
        assertEquals(deckSize, game.totalCardCount(), label);
        for (int moves = 0; !game.isFinished(); moves++) {
            assertTrue(moves < MAX_MOVES, "la partita non finisce: " + label);
            VisibleState state = game.getVisibleState();
            for (Move move : cpus.get(state.currentPlayerName()).decide(state)) {
                game.execute(move);
                assertEquals(deckSize, game.totalCardCount(), "carte perse o duplicate dopo " + move + ", " + label);
            }
        }
        return events;
    }

    @Test
    void wholeGamesNeverLoseCardsAndEndWithOneWinner() {
        // Classica, da 2 a 10 giocatori.
        for (int players : new int[] { 2, 3, 4, 10 }) {
            for (int seed = 0; seed < 100; seed++) {
                List<String> names = names(players);
                Game game = new Game(names, GameRules.classic(), new Random(seed));
                List<GameEvent> events = playToTheEnd(game, cpus(names), CLASSIC_DECK, players + " giocatori, seme " + seed);

                Player winner = game.getWinner().orElseThrow();
                assertEquals(0, winner.getHand().size());
                List<GameEvent> wins = events.stream().filter(e -> e.type() == GameEvent.Type.GAME_WON).toList();
                assertEquals(1, wins.size(), "un vincitore solo");
                assertEquals(winner.getName(), wins.get(0).playerName());
                for (GameEvent event : events) {
                    if (event.type() == GameEvent.Type.CARDS_DRAWN || event.type() == GameEvent.Type.LAST_CARD_PENALTY) {
                        assertNull(event.card(), "una pescata non deve dire quale carta si è presa");
                    }
                }
            }
        }
        // No Mercy, con eliminazioni, scambi e rotazioni: il vincitore non è mai un eliminato.
        for (int seed = 0; seed < 300; seed++) {
            List<String> names = names(2 + seed % 5);
            Game game = new Game(names, GameRules.noMercy(), new Random(seed));
            playToTheEnd(game, cpus(names), NO_MERCY_DECK, "No Mercy, seme " + seed);
            assertFalse(game.getWinner().orElseThrow().isEliminated(), "seme " + seed);
        }
    }

    @Test
    void theSameSeedReplaysTheSameGame() {
        List<String> names = names(3);
        assertEquals(replay(names, 42), replay(names, 42));
        assertNotEquals(replay(names, 42), replay(names, 43));
    }

    private static List<GameEvent> replay(List<String> names, long seed) {
        return playToTheEnd(new Game(names, GameRules.classic(), new Random(seed)), cpus(names), CLASSIC_DECK, "replay");
    }
}

package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Partite No Mercy intere fra CPU, con semi fissi: nessuna mossa scelta dalla CPU deve essere rifiutata,
 * ogni partita deve finire e il numero di carte deve restare quello del mazzo dopo ogni mossa.
 * È il controllo che trova i casi limite che i test a mano non coprono (mazzo esaurito, eliminazioni a catena).
 */
class NoMercySimulationTest {

    private static final int GAMES = 300;
    private static final int MAX_MOVES = 20_000;
    private static final int NO_MERCY_DECK = 168;

    @Test
    void cpuOnlyGamesAlwaysEndAndNeverLoseCards() {
        for (int seed = 0; seed < GAMES; seed++) {
            int playerCount = 2 + seed % 5;
            List<String> names = new ArrayList<>();
            Map<String, CpuPlayer> cpus = new HashMap<>();
            for (int i = 0; i < playerCount; i++) {
                String name = "CPU " + i;
                names.add(name);
                cpus.put(name, new CpuPlayer(name, new BasicCpuStrategy()));
            }

            Game game = new Game(names, GameRules.noMercy(), new Random(seed));
            game.start();
            int moves = 0;
            while (!game.isFinished() && moves < MAX_MOVES) {
                VisibleState state = game.getVisibleState();
                for (Move move : cpus.get(state.currentPlayerName()).decide(state)) {
                    game.execute(move);
                }
                moves++;
                assertEquals(NO_MERCY_DECK, game.totalCardCount(), "carte perse o duplicate, seme " + seed);
            }

            assertTrue(game.isFinished(), "la partita con seme " + seed + " non finisce");
            Player winner = game.getWinner().orElseThrow();
            assertTrue(!winner.isEliminated(), "il vincitore non può essere eliminato, seme " + seed);
        }
    }
}

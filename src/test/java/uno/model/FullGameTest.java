package uno.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Partite intere tra CPU con mazzo vero e mescolato: invarianti che devono valere dopo ogni singola mossa. */
class FullGameTest {

    private static final int GAMES = 200;
    private static final int CLASSIC_DECK_SIZE = 108;
    private static final int MAX_MOVES = 10_000;

    @Test
    void theSameSeedReplaysTheSameGame() {
        List<String> names = List.of("CPU1", "CPU2", "CPU3");
        assertEquals(playAndRecord(names, 42), playAndRecord(names, 42));
        assertNotEquals(playAndRecord(names, 42), playAndRecord(names, 43));
    }

    // Gioca una partita CPU con il seme dato e restituisce tutti gli eventi.
    private static List<GameEvent> playAndRecord(List<String> names, long seed) {
        Map<String, CpuPlayer> cpus = new HashMap<>();
        for (String name : names) {
            cpus.put(name, new CpuPlayer(name, new BasicCpuStrategy()));
        }
        Game game = new Game(names, GameRules.classic(), new Random(seed));
        List<GameEvent> events = new ArrayList<>();
        game.addObserver(events::add);
        game.start();
        while (!game.isFinished()) {
            VisibleState state = game.getVisibleState();
            for (Move move : cpus.get(state.currentPlayerName()).decide(state)) {
                game.execute(move);
            }
        }
        return events;
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 10})
    void cardCountStaysConstantAndEveryGameEndsWithOneWinner(int playerCount) {
        for (int g = 0; g < GAMES; g++) {
            List<String> names = new ArrayList<>();
            Map<String, CpuPlayer> cpus = new HashMap<>();
            for (int i = 1; i <= playerCount; i++) {
                names.add("CPU" + i);
                cpus.put("CPU" + i, new CpuPlayer("CPU" + i, new BasicCpuStrategy()));
            }

            Game game = new Game(names);
            List<GameEvent> events = new ArrayList<>();
            game.addObserver(events::add);
            game.start();
            assertEquals(CLASSIC_DECK_SIZE, game.totalCardCount());

            int moves = 0;
            while (!game.isFinished()) {
                assertTrue(moves < MAX_MOVES, "Game did not finish within " + MAX_MOVES + " moves.");
                VisibleState state = game.getVisibleState();
                for (Move move : cpus.get(state.currentPlayerName()).decide(state)) {
                    game.execute(move);
                    moves++;
                    assertEquals(CLASSIC_DECK_SIZE, game.totalCardCount(), "Cards lost or duplicated after " + move);
                }
            }

            Player winner = game.getWinner().orElseThrow();
            assertEquals(0, winner.getHand().size());
            List<GameEvent> wins = events.stream().filter(e -> e.type() == GameEvent.Type.GAME_WON).toList();
            assertEquals(1, wins.size());
            assertEquals(winner.getName(), wins.get(0).playerName());
            for (GameEvent event : events) {
                if (event.type() == GameEvent.Type.CARDS_DRAWN || event.type() == GameEvent.Type.UNO_PENALTY) {
                    assertNull(event.card(), "A draw event must not reveal the drawn card.");
                }
            }
        }
    }
}

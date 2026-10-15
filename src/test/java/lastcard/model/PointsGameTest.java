package lastcard.model;

import static lastcard.model.GameSetup.FILLER;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;
import static lastcard.model.GameSetup.player;
import static lastcard.model.GameSetup.trimHand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * La partita a punti: chi chiude la mano prende i punti delle carte rimaste agli avversari, la partita
 * finisce quando qualcuno raggiunge l'obiettivo, altrimenti si gioca la mano successiva. Nel No Mercy
 * l'eliminazione vale per una mano sola e i punti dell'eliminato vanno a chi chiude la mano.
 * La prima carta è un rosso 5, quindi comincia A.
 */
class PointsGameTest {

    private static final List<String> AB = List.of("A", "B");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    // Riempitivo giallo 9: sei riempitivi valgono 54 punti.
    private static final int SIX_FILLERS = 6 * 9;

    // A ha una sola carta, il rosso 3, e la gioca subito chiudendo la mano. B tiene un blu 8 e sei riempitivi.
    private static Game handClosedByA(int pointsToWin, List<GameEvent> events) {
        Game game = GameSetup.create(GameRules.classic().withPointsToWin(pointsToWin), AB, List.of(
                hand(new NumberCard(CardColor.RED, 3)),
                hand(new NumberCard(CardColor.BLUE, 8))), RED_FIVE, Collections.nCopies(30, FILLER));
        game.addObserver(events::add);
        game.start();
        trimHand(game, "A", 1);
        game.execute(new PlayCard("A", 0));
        return game;
    }

    private static List<GameEvent.Type> types(List<GameEvent> events) {
        return events.stream().map(GameEvent::type).toList();
    }

    // Con due +10 di fila A arriva a 6 + 20 carte, supera la soglia di 25 ed è eliminato: B resta solo.
    private static Game handClosedByEliminatingA(int pointsToWin) {
        Game game = GameSetup.start(GameRules.noMercy().withPointsToWin(pointsToWin), AB,
                List.of(hand(new WildDrawTen()), hand(new WildDrawTen())), RED_FIVE);
        game.execute(new PlayCard("A", 0));
        game.execute(new ChooseColor("A", CardColor.YELLOW));
        game.execute(new PlayCard("B", 0));
        game.execute(new ChooseColor("B", CardColor.YELLOW));
        game.execute(new Draw("A"));
        return game;
    }

    // ------------------------------------------------------------------
    // fine della mano
    // ------------------------------------------------------------------

    @Test
    void whoClosesTheHandTakesThePointsLeftInTheOpponentsHands() {
        List<GameEvent> events = new ArrayList<>();
        Game game = handClosedByA(500, events);

        assertEquals(GameState.HAND_OVER, game.getState());
        assertEquals(8 + SIX_FILLERS, player(game, "A").getScore());
        assertEquals(0, player(game, "B").getScore());
        assertEquals("A", game.getWinner().orElseThrow().getName());

        GameEvent handWon = events.get(events.size() - 1);
        assertEquals(GameEvent.Type.HAND_WON, handWon.type());
        assertEquals("A", handWon.playerName());
        assertEquals(8 + SIX_FILLERS, handWon.count());
        assertFalse(types(events).contains(GameEvent.Type.GAME_WON), "l'obiettivo non è raggiunto");
        assertFalse(game.isFinished());
    }

    @Test
    void reachingTheGoalEndsTheGame() {
        List<GameEvent> events = new ArrayList<>();
        Game game = handClosedByA(50, events);

        assertEquals(GameState.FINISHED, game.getState());
        assertTrue(game.isFinished());
        List<GameEvent.Type> types = types(events);
        assertEquals(List.of(GameEvent.Type.HAND_WON, GameEvent.Type.GAME_WON), types.subList(types.size() - 2, types.size()));
        assertThrows(IllegalStateException.class, game::startNextHand, "a partita finita non c'è una mano successiva");
    }

    @Test
    void withASingleHandThereAreNoPoints() {
        Game game = GameSetup.start(AB, List.of(hand(new NumberCard(CardColor.RED, 3)), hand()), RED_FIVE);
        trimHand(game, "A", 1);

        game.execute(new PlayCard("A", 0));

        assertEquals(GameState.FINISHED, game.getState());
        assertEquals(0, player(game, "A").getScore());
    }

    // ------------------------------------------------------------------
    // mano successiva
    // ------------------------------------------------------------------

    @Test
    void theNextHandIsDealtAgainAndTheScoresStay() {
        Game game = handClosedByA(500, new ArrayList<>());
        int cards = game.totalCardCount();

        game.startNextHand();

        assertEquals(GameState.IN_PROGRESS, game.getState());
        assertEquals(7, handSize(game, "A"));
        assertEquals(7, handSize(game, "B"));
        assertEquals(cards, game.totalCardCount(), "mani e scarti tornano nel mazzo, nessuna carta persa");
        assertEquals(8 + SIX_FILLERS, player(game, "A").getScore());
        assertTrue(game.getWinner().isEmpty(), "la nuova mano non ha ancora un vincitore");
        assertEquals(0, game.getVisibleState().pendingDraw());
    }

    @Test
    void theNextHandStartsOnlyWhenTheHandIsOver() {
        Game game = GameSetup.start(GameRules.classic().withPointsToWin(500), AB, List.of(hand(), hand()), RED_FIVE);

        assertThrows(IllegalStateException.class, game::startNextHand);
    }

    // ------------------------------------------------------------------
    // No Mercy: eliminazione
    // ------------------------------------------------------------------

    @Test
    void whoWinsByEliminationTakesThePointsOfTheEliminatedHand() {
        Game game = handClosedByEliminatingA(1000);

        assertTrue(player(game, "A").isEliminated());
        assertEquals(GameState.HAND_OVER, game.getState());
        assertEquals("B", game.getWinner().orElseThrow().getName());
        // A aveva 6 riempitivi e ne ha pescati 20: 26 carte da 9 punti, contate prima che tornassero negli scarti
        assertEquals(26 * 9, player(game, "B").getScore());
    }

    @Test
    void theEliminatedPlayerRejoinsTheNextHand() {
        Game game = handClosedByEliminatingA(1000);
        int cards = game.totalCardCount();

        game.startNextHand();

        Player a = player(game, "A");
        assertFalse(a.isEliminated(), "l'eliminazione vale per una mano sola");
        assertFalse(game.getVisibleState().players().get(0).eliminated());
        assertEquals(7, a.getHand().size());
        assertEquals(7, handSize(game, "B"));
        assertEquals(cards, game.totalCardCount());
        assertEquals(26 * 9, player(game, "B").getScore(), "il punteggio resta");
    }

    @Test
    void eliminatedPointsAreCountedOnlyInTheHandWhereTheyHappened() {
        Game game = handClosedByEliminatingA(1000);
        game.startNextHand();
        int before = player(game, "B").getScore();

        // Seconda mano: A ha 7 carte, B le chiude giocando l'ultima. Conta solo la mano di A di adesso.
        Player a = player(game, "A");
        Player b = player(game, "B");
        while (b.getHand().size() > 1) {
            b.removeCard(b.getHand().size() - 1);
        }
        b.removeCard(0);
        b.addCard(new Wild());
        String first = game.getCurrentPlayer().getName();
        if (first.equals("A")) {
            game.execute(new Draw("A"));
            if (game.getVisibleState().canPass()) {
                game.execute(new Pass("A"));
            }
        }
        game.execute(new PlayCard("B", 0));

        assertEquals(GameState.HAND_OVER, game.getState());
        assertEquals(before + a.handPoints(), b.getScore());
    }

    // ------------------------------------------------------------------
    // partite intere
    // ------------------------------------------------------------------

    // CPU contro CPU fino all'obiettivo, mano dopo mano: nessuna eccezione, nessuna carta persa o duplicata,
    // nessun eliminato che resta fuori o tiene carte nella mano successiva.
    private static void playWholePointGames(GameRules base, int goal) {
        for (long seed = 0; seed < 60; seed++) {
            int count = 2 + (int) (seed % 5);
            List<String> names = new ArrayList<>();
            Map<String, CpuPlayer> cpus = new HashMap<>();
            for (int i = 1; i <= count; i++) {
                names.add("CPU " + i);
                cpus.put("CPU " + i, new CpuPlayer("CPU " + i, new BasicCpuStrategy()));
            }
            Game game = new Game(names, base.withPointsToWin(goal), new Random(seed));
            game.start();
            int cards = game.totalCardCount();
            int hands = 1;

            for (int step = 0; step < 100_000 && !game.isFinished(); step++) {
                if (game.getState() == GameState.HAND_OVER) {
                    game.startNextHand();
                    hands++;
                    for (Player player : game.getPlayers()) {
                        assertFalse(player.isEliminated(), "seme " + seed + ": " + player.getName() + " resta eliminato");
                    }
                    continue;
                }
                CpuPlayer cpu = cpus.get(game.getVisibleState().currentPlayerName());
                for (Move move : cpu.decide(game.getVisibleState())) {
                    if (game.isFinished() || game.getState() == GameState.HAND_OVER) {
                        break;
                    }
                    game.execute(move);
                }
                assertEquals(cards, game.totalCardCount(), "seme " + seed + ", mano " + hands);
            }

            assertTrue(game.isFinished(), "la partita con seme " + seed + " non finisce");
            Player winner = game.getWinner().orElseThrow();
            assertNotNull(winner);
            assertTrue(winner.getScore() >= goal, "seme " + seed + ": vince chi raggiunge l'obiettivo");
        }
    }

    @Test
    void wholeClassicPointGamesReachTheGoal() {
        playWholePointGames(GameRules.classic(), 500);
    }

    @Test
    void wholeNoMercyPointGamesReachTheGoal() {
        playWholePointGames(GameRules.noMercy(), 500);
    }
}

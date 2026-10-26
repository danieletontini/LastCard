package lastcard.model;

import static lastcard.model.GameSetup.FILLER;
import static lastcard.model.GameSetup.hand;
import static lastcard.model.GameSetup.handSize;
import static lastcard.model.GameSetup.player;
import static lastcard.model.GameSetup.trimHand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 */
class PointsGameTest {

    private static final List<String> AB = List.of("A", "B");
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    // B tiene un blu 8 e sei riempitivi gialli da 9: 62 punti.
    private static final int B_POINTS = 8 + 6 * 9;

    // A ha una sola carta, il rosso 3, e la gioca subito chiudendo la mano.
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

    @Test
    void closingAHandScoresTheOpponentsCardsAndTheGoalEndsTheGame() {
        List<GameEvent> events = new ArrayList<>();
        Game game = handClosedByA(500, events);
        assertEquals(GameState.HAND_OVER, game.getState(), "a 62 punti su 500 si va avanti");
        assertEquals(B_POINTS, player(game, "A").getScore());
        GameEvent handWon = events.get(events.size() - 1);
        assertEquals(GameEvent.Type.HAND_WON, handWon.type());
        assertEquals(B_POINTS, handWon.count());

        int cards = game.totalCardCount();
        game.startNextHand();
        assertEquals(GameState.IN_PROGRESS, game.getState());
        assertEquals(7, handSize(game, "A"), "la mano successiva si distribuisce da capo");
        assertEquals(cards, game.totalCardCount(), "mani e scarti tornano nel mazzo, nessuna carta persa");
        assertEquals(B_POINTS, player(game, "A").getScore(), "il punteggio resta");
        assertThrows(IllegalStateException.class, game::startNextHand, "si passa alla mano dopo solo a mano chiusa");

        List<GameEvent> goalEvents = new ArrayList<>();
        Game reached = handClosedByA(50, goalEvents);
        assertEquals(GameState.FINISHED, reached.getState(), "a 62 punti su 50 la partita è vinta");
        List<GameEvent.Type> types = goalEvents.stream().map(GameEvent::type).toList();
        assertEquals(List.of(GameEvent.Type.HAND_WON, GameEvent.Type.GAME_WON), types.subList(types.size() - 2, types.size()));
    }

    @Test
    void eliminatedPlayersGiveTheirPointsAndRejoinTheNextHand() {
        // Con due +10 di fila A arriva a 6 + 20 carte, supera la soglia di 25 ed è eliminato: B resta solo.
        Game game = GameSetup.start(GameRules.noMercy().withPointsToWin(1000), AB,
                List.of(hand(new WildDrawTen()), hand(new WildDrawTen())), RED_FIVE);
        game.execute(new PlayCard("A", 0));
        game.execute(new ChooseColor("A", CardColor.YELLOW));
        game.execute(new PlayCard("B", 0));
        game.execute(new ChooseColor("B", CardColor.YELLOW));
        game.execute(new Draw("A"));

        assertEquals(GameState.HAND_OVER, game.getState());
        assertEquals("B", game.getWinner().orElseThrow().getName());
        // 26 carte da 9 punti, contate prima che tornassero negli scarti
        assertEquals(26 * 9, player(game, "B").getScore());

        int cards = game.totalCardCount();
        game.startNextHand();
        Player a = player(game, "A");
        assertFalse(a.isEliminated(), "l'eliminazione vale per una mano sola");
        assertEquals(7, a.getHand().size());
        assertEquals(cards, game.totalCardCount());
        assertEquals(26 * 9, player(game, "B").getScore());

        // Seconda mano: B la chiude con un jolly. Contano solo le carte che A ha adesso, non quelle di prima.
        Player b = player(game, "B");
        while (!b.getHand().isEmpty()) {
            b.removeCard(0);
        }
        b.addCard(new Wild());
        if (game.getCurrentPlayer().getName().equals("A")) {
            game.execute(new Draw("A"));
            if (game.getVisibleState().canPass()) {
                game.execute(new Pass("A"));
            }
        }
        game.execute(new PlayCard("B", 0));
        assertEquals(26 * 9 + a.handPoints(), b.getScore());
    }

    @Test
    void wholePointGamesReachTheGoalHandAfterHand() {
        // CPU contro CPU fino all'obiettivo, classica e No Mercy: nessuna carta persa o duplicata, nessun eliminato
        // che resta fuori nella mano successiva, e vince chi arriva all'obiettivo.
        for (GameRules base : List.of(GameRules.classic().withPointsToWin(500), GameRules.noMercy().withPointsToWin(1000))) {
            for (long seed = 0; seed < 60; seed++) {
                int count = 2 + (int) (seed % 5);
                List<String> names = new ArrayList<>();
                Map<String, CpuPlayer> cpus = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    names.add("CPU " + i);
                    cpus.put("CPU " + i, new CpuPlayer("CPU " + i, new BasicCpuStrategy()));
                }
                Game game = new Game(names, base, new Random(seed));
                game.start();
                int cards = game.totalCardCount();

                for (int step = 0; step < 100_000 && !game.isFinished(); step++) {
                    if (game.getState() == GameState.HAND_OVER) {
                        game.startNextHand();
                        for (Player p : game.getPlayers()) {
                            assertFalse(p.isEliminated(), "seme " + seed + ": " + p.getName() + " resta eliminato");
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
                    assertEquals(cards, game.totalCardCount(), "seme " + seed);
                }

                assertTrue(game.isFinished(), "la partita con seme " + seed + " non finisce");
                assertTrue(game.getWinner().orElseThrow().getScore() >= base.pointsToWin(), "seme " + seed);
            }
        }
    }
}

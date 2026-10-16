package lastcard.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import lastcard.model.BasicCpuStrategy;
import lastcard.model.CallLastCard;
import lastcard.model.CardColor;
import lastcard.model.CpuPlayer;
import lastcard.model.Draw;
import lastcard.model.DrawTwo;
import lastcard.model.Game;
import lastcard.model.GameEvent;
import lastcard.model.GameRules;
import lastcard.model.GameState;
import lastcard.model.Move;
import lastcard.model.NumberCard;
import lastcard.model.Pass;
import lastcard.model.PlayCard;
import lastcard.model.PlayerSummary;
import lastcard.model.VisibleState;
import lastcard.model.WildReverseDrawFour;
import lastcard.view.TableView;

/**
 * Il controller del tavolo grafico, senza Swing: un tavolo finto registra cosa gli si chiede e un timer finto
 * esegue le mosse delle CPU solo quando lo dice il test. Il giocatore umano lo fa giocare una CpuPlayer,
 * passando dalle stesse azioni che userebbe il tavolo vero.
 */
class GuiGameControllerTest {

    private static final String HUMAN = "Tu";

    /** Tiene da parte il compito invece di aspettare: il test lo esegue quando vuole. */
    private static final class ManualScheduler implements TurnScheduler {
        Runnable task;
        int delay;

        @Override
        public void schedule(int delayMillis, Runnable newTask) {
            task = newTask;
            delay = delayMillis;
        }

        @Override
        public void cancel() {
            task = null;
        }

        void runPending() {
            Runnable pending = task;
            task = null;
            pending.run();
        }
    }

    /** Il tavolo finto: ricorda l'ultimo stato, le scelte chieste, gli errori e il vincitore. */
    private static final class FakeTable implements TableView {
        VisibleState state;
        boolean colorAsked;
        List<PlayerSummary> swapAsked;
        final List<String> errors = new ArrayList<>();
        final List<GameEvent> events = new ArrayList<>();
        String winner;
        String viewer;
        // Partita a punti: il form di fine mano è aperto, e quante volte è stato mostrato.
        boolean handOver;
        int handsOver;

        @Override
        public void reset(String viewerName, TableActions actions) {
            viewer = viewerName;
        }

        @Override
        public void showState(VisibleState newState) {
            state = newState;
        }

        @Override
        public void askColor(boolean roulette) {
            colorAsked = true;
        }

        @Override
        public void askSwapTarget(List<PlayerSummary> opponents) {
            swapAsked = opponents;
        }

        @Override
        public void showError(String message) {
            errors.add(message);
        }

        @Override
        public void showWinner(String winnerName) {
            winner = winnerName;
        }

        @Override
        public void showHandOver(List<PlayerSummary> players) {
            handOver = true;
            handsOver++;
        }

        @Override
        public void update(GameEvent event) {
            events.add(event);
        }
    }

    private record Setup(Game game, FakeTable table, ManualScheduler scheduler, GuiGameController controller) { }

    private static Setup start(int opponents, GameRules rules, long seed) {
        List<String> names = new ArrayList<>(List.of(HUMAN));
        List<CpuPlayer> cpus = new ArrayList<>();
        for (int i = 1; i <= opponents; i++) {
            names.add("CPU " + i);
            cpus.add(new CpuPlayer("CPU " + i, new BasicCpuStrategy()));
        }
        Game game = new Game(names, rules, new Random(seed));
        FakeTable table = new FakeTable();
        ManualScheduler scheduler = new ManualScheduler();
        GuiGameController controller = new GuiGameController(game, table, cpus, HUMAN, scheduler);
        controller.start();
        return new Setup(game, table, scheduler, controller);
    }

    // Fa giocare le CPU finché tocca all'umano.
    private static void runCpusUntilHuman(Setup s) {
        for (int i = 0; i < 200 && s.scheduler().task != null; i++) {
            s.scheduler().runPending();
        }
    }

    // Una mossa dell'umano scelta da una CpuPlayer, mandata al controller con le azioni del tavolo.
    private static void playHuman(Setup s, CpuPlayer brain) {
        FakeTable table = s.table();
        if (table.colorAsked) {
            table.colorAsked = false;
            s.controller().onColorChosen(CardColor.RED);
            return;
        }
        if (table.swapAsked != null) {
            String target = table.swapAsked.get(0).name();
            table.swapAsked = null;
            s.controller().onSwapTargetChosen(target);
            return;
        }
        for (Move move : brain.decide(table.state)) {
            switch (move) {
                case PlayCard play -> s.controller().onCardSelected(play.handIndex());
                case Draw draw -> s.controller().onDraw();
                case Pass pass -> s.controller().onPass();
                case CallLastCard call -> s.controller().onLastCardCalled();
                default -> throw new AssertionError("Unexpected move for the human: " + move);
            }
        }
    }

    @Test
    void everyCpuMoveWaitsTwoSeconds() {
        Setup s = start(3, GameRules.classic(), 1);
        CpuPlayer brain = new CpuPlayer(HUMAN, new BasicCpuStrategy());
        runCpusUntilHuman(s);
        playHuman(s, brain);

        if (s.scheduler().task != null) {
            assertEquals(2000, s.scheduler().delay);
            String waiting = s.table().state.currentPlayerName();
            assertTrue(waiting.startsWith("CPU"), "dopo la mossa dell'umano tocca a una CPU, che aspetta");
        }
    }

    @Test
    void theHumanCannotMoveDuringACpuTurn() {
        Setup s = start(3, GameRules.classic(), 2);
        runCpusUntilHuman(s);
        playHuman(s, new CpuPlayer(HUMAN, new BasicCpuStrategy()));
        if (s.scheduler().task == null) {
            return; // la mossa ha chiuso la partita o è ancora il turno dell'umano: niente da verificare
        }
        VisibleState before = s.table().state;

        s.controller().onDraw();

        assertEquals(List.of("Aspetta il tuo turno."), s.table().errors);
        assertEquals(before, s.table().state);
    }

    @Test
    void aRejectedMoveShowsAnItalianMessage() {
        Setup s = start(2, GameRules.classic(), 3);
        runCpusUntilHuman(s);

        s.controller().onCardSelected(99);

        assertEquals(List.of("Non puoi giocare questa carta."), s.table().errors);
    }

    @Test
    void withAPendingPenaltyTheErrorExplainsHowToAnswer() {
        List<PlayerSummary> players = List.of(new PlayerSummary(HUMAN, 2, false, 0),
                new PlayerSummary("CPU 1", 5, false, 0));
        VisibleState facingEight = new VisibleState(new WildReverseDrawFour(), CardColor.BLUE, HUMAN,
                List.of(new DrawTwo(CardColor.BLUE), new NumberCard(CardColor.RED, 5)), players,
                GameState.IN_PROGRESS, false, 8);
        VisibleState noPenalty = new VisibleState(new NumberCard(CardColor.BLUE, 3), CardColor.BLUE, HUMAN,
                List.of(new NumberCard(CardColor.RED, 5)), players, GameState.IN_PROGRESS, false, 0);

        assertEquals("Puoi rilanciare solo con un +4 o più, oppure pescare 8 carte.",
                GuiGameController.rejectedCardMessage(facingEight));
        assertEquals("Non puoi giocare questa carta.", GuiGameController.rejectedCardMessage(noPenalty));
    }

    @Test
    void theTableAlwaysShowsTheHumanHand() {
        Setup s = start(3, GameRules.classic(), 4);
        assertEquals(HUMAN, s.table().viewer);
        for (int i = 0; i < 10 && s.scheduler().task != null; i++) {
            VisibleState state = s.table().state;
            PlayerSummary me = state.players().stream().filter(p -> p.name().equals(HUMAN)).findFirst().orElseThrow();
            assertEquals(me.cardCount(), state.currentHand().size(), "anche nel turno di " + state.currentPlayerName());
            s.scheduler().runPending();
        }
    }

    @Test
    void leavingTheTableStopsTheCpus() {
        Setup s = start(3, GameRules.classic(), 5);
        runCpusUntilHuman(s);
        playHuman(s, new CpuPlayer(HUMAN, new BasicCpuStrategy()));
        int events = s.table().events.size();

        s.controller().onLeaveTable();

        assertNull(s.scheduler().task, "il timer è annullato");
        s.controller().onDraw();
        assertEquals(events, s.table().events.size(), "dopo l'uscita il tavolo non riceve più eventi");
    }

    @Test
    void withTheOptionsOpenTheCpusWaitAndResumeAfterwards() {
        Setup s = start(3, GameRules.classic(), 6);
        runCpusUntilHuman(s);
        playHuman(s, new CpuPlayer(HUMAN, new BasicCpuStrategy()));
        if (s.scheduler().task == null) {
            return; // tocca ancora all'umano o la partita è finita: nessuna CPU da fermare
        }
        VisibleState before = s.table().state;

        s.controller().onPause();

        assertNull(s.scheduler().task, "in pausa la mossa della CPU è annullata");
        assertEquals(before, s.table().state);

        s.controller().onResume();

        assertNotNull(s.scheduler().task, "alla ripresa la CPU di turno riparte");
        assertEquals(GuiGameController.CPU_MOVE_DELAY_MILLIS, s.scheduler().delay, "con un'attesa intera");
        assertEquals(before, s.table().state, "la pausa non ha cambiato la partita");
    }

    @Test
    void duringThePauseTheHumanCannotMove() {
        Setup s = start(2, GameRules.classic(), 7);
        runCpusUntilHuman(s);
        VisibleState before = s.table().state;

        s.controller().onPause();
        s.controller().onDraw();

        assertEquals(before, s.table().state, "in pausa la pescata non arriva alla partita");
        assertTrue(s.table().errors.isEmpty());

        s.controller().onResume();
        s.controller().onDraw();

        assertEquals(before.currentHand().size() + 1, s.table().state.currentHand().size(),
                "dopo la ripresa si gioca di nuovo");
    }

    @Test
    void resumingAChoiceAsksItAgain() {
        // Si gioca finché all'umano viene chiesto un colore, poi si apre e si chiude il form Opzioni.
        for (long seed = 0; seed < 40; seed++) {
            Setup s = start(1, GameRules.classic(), seed);
            CpuPlayer brain = new CpuPlayer(HUMAN, new BasicCpuStrategy());
            for (int step = 0; step < 2000 && s.table().winner == null && !s.table().colorAsked; step++) {
                if (s.scheduler().task != null) {
                    s.scheduler().runPending();
                } else {
                    playHuman(s, brain);
                }
            }
            if (!s.table().colorAsked) {
                continue;
            }
            s.table().colorAsked = false;

            s.controller().onPause();
            s.controller().onResume();

            assertTrue(s.table().colorAsked, "dopo la pausa il form del colore si riapre");
            return;
        }
        throw new AssertionError("in 40 partite l'umano non ha mai dovuto scegliere un colore");
    }

    @Test
    void wholeGamesPlayedFromTheTableEndWithoutErrors() {
        int colorChoices = 0;
        for (long seed = 0; seed < 60; seed++) {
            GameRules rules = seed % 2 == 0 ? GameRules.classic() : GameRules.noMercy();
            Setup s = start(1 + (int) (seed % 5), rules, seed);
            CpuPlayer brain = new CpuPlayer(HUMAN, new BasicCpuStrategy());
            for (int step = 0; step < 5000 && s.table().winner == null; step++) {
                if (s.scheduler().task != null) {
                    assertEquals(GuiGameController.CPU_MOVE_DELAY_MILLIS, s.scheduler().delay);
                    s.scheduler().runPending();
                } else {
                    colorChoices += s.table().colorAsked ? 1 : 0;
                    playHuman(s, brain);
                }
            }
            assertNotNull(s.table().winner, "la partita con seme " + seed + " non finisce");
            assertTrue(s.table().errors.isEmpty(), "mosse rifiutate con seme " + seed + ": " + s.table().errors);
        }
        assertTrue(colorChoices > 0, "in 60 partite l'umano deve aver scelto almeno un colore dal form");
    }

    @Test
    void pointGamesGoOnHandAfterHandFromTheTable() {
        int handsOver = 0;
        for (long seed = 0; seed < 30; seed++) {
            GameRules base = seed % 2 == 0 ? GameRules.classic() : GameRules.noMercy();
            Setup s = start(1 + (int) (seed % 4), base.withPointsToWin(200), seed);
            CpuPlayer brain = new CpuPlayer(HUMAN, new BasicCpuStrategy());
            for (int step = 0; step < 50_000 && s.table().winner == null; step++) {
                if (s.table().handOver) {
                    // A mano chiusa nessuno muove finché non si preme "Prossima mano".
                    assertNull(s.scheduler().task, "con il form di fine mano aperto le CPU aspettano");
                    s.table().handOver = false;
                    s.controller().onNextHand();
                } else if (s.scheduler().task != null) {
                    s.scheduler().runPending();
                } else {
                    playHuman(s, brain);
                }
            }
            assertNotNull(s.table().winner, "la partita a punti con seme " + seed + " non finisce");
            assertTrue(s.table().errors.isEmpty(), "mosse rifiutate con seme " + seed + ": " + s.table().errors);
            handsOver += s.table().handsOver;
        }
        assertTrue(handsOver > 0, "in 30 partite a 200 punti almeno una mano deve chiudersi senza raggiungere l'obiettivo");
    }
}

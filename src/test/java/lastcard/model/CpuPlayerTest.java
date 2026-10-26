package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Le mosse che sceglie la CPU con la strategia base, a partire da ciò che vede: solo lo stato visibile. */
class CpuPlayerTest {

    private static final String CPU = "CPU";
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    private static final Card RED_SEVEN = new NumberCard(CardColor.RED, 7);
    private static final Card BLUE_THREE = new NumberCard(CardColor.BLUE, 3);
    private static final Card BLUE_ONE = new NumberCard(CardColor.BLUE, 1);

    private final CpuPlayer cpu = new CpuPlayer(CPU, new BasicCpuStrategy());

    /** Quello che vede la CPU quando tocca a lei, con il 5 rosso in cima. */
    private static VisibleState stateWith(List<Card> hand, List<PlayerSummary> players, GameState state, boolean canPass) {
        return new VisibleState(RED_FIVE, CardColor.RED, CPU, hand, players, state, canPass, 0);
    }

    private static VisibleState stateWith(List<Card> hand, GameState state, boolean canPass) {
        return stateWith(hand, List.of(summary(CPU, hand.size()), summary("Tu", 7)), state, canPass);
    }

    private static VisibleState turnWith(Card... hand) {
        return stateWith(List.of(hand), GameState.IN_PROGRESS, false);
    }

    private static PlayerSummary summary(String name, int cardCount) {
        return new PlayerSummary(name, cardCount, false, 0);
    }

    @Test
    void playsAPlayableCardKeepingWildsForLastOrDraws() {
        assertEquals(List.of(new PlayCard(CPU, 1)), cpu.decide(turnWith(BLUE_THREE, RED_SEVEN, BLUE_THREE)));
        assertEquals(List.of(new Draw(CPU)), cpu.decide(turnWith(BLUE_THREE, BLUE_THREE, BLUE_THREE)),
                "senza carte giocabili pesca");
        assertEquals(List.of(new PlayCard(CPU, 1)), cpu.decide(turnWith(new Wild(), RED_SEVEN, BLUE_THREE)),
                "tiene il jolly per ultimo");
        assertEquals(List.of(new CallLastCard(CPU), new PlayCard(CPU, 0)), cpu.decide(turnWith(RED_SEVEN, BLUE_THREE)),
                "dichiara Ultima! prima di giocare la penultima carta");

        // dopo aver pescato una carta giocabile la gioca, oppure passa se la strategia la vuole tenere
        VisibleState afterDraw = stateWith(List.of(BLUE_THREE, BLUE_THREE, RED_SEVEN), GameState.IN_PROGRESS, true);
        assertEquals(List.of(new PlayCard(CPU, 2)), cpu.decide(afterDraw));
        CpuPlayer keeper = new CpuPlayer(CPU, new KeepEverything());
        assertEquals(List.of(new Pass(CPU)), keeper.decide(afterDraw));
    }

    @Test
    void choosesTheColorItHasMostOfAndSwapsWithWhoHasFewestCards() {
        VisibleState wild = stateWith(List.of(BLUE_ONE, RED_SEVEN, BLUE_THREE), GameState.AWAITING_COLOR, false);
        assertEquals(List.of(new ChooseColor(CPU, CardColor.BLUE)), cpu.decide(wild));
        VisibleState roulette = stateWith(List.of(BLUE_ONE, BLUE_THREE, RED_SEVEN), GameState.AWAITING_ROULETTE_COLOR, false);
        assertEquals(List.of(new ChooseColor(CPU, CardColor.BLUE)), cpu.decide(roulette), "anche quando subisce la Roulette");

        List<PlayerSummary> players = List.of(summary(CPU, 6), summary("Tu", 5), summary("Bea", 2),
                new PlayerSummary("Fuori", 0, true, 0));
        VisibleState seven = stateWith(List.of(RED_SEVEN), players, GameState.AWAITING_SWAP_TARGET, false);
        assertEquals(List.of(new SwapHands(CPU, "Bea")), cpu.decide(seven),
                "scambia con chi ha meno carte, mai con un eliminato");
    }

    /** Una strategia che non gioca mai: serve a vedere che dopo la pescata CpuPlayer passa. */
    private record KeepEverything() implements CpuStrategy {
        @Override
        public Optional<Card> choose(List<Card> playable) {
            return Optional.empty();
        }

        @Override
        public CardColor chooseColor(List<Card> hand) {
            return CardColor.GREEN;
        }

        @Override
        public String chooseSwapTarget(List<PlayerSummary> opponents) {
            return opponents.get(0).name();
        }
    }
}

package uno.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Le mosse che sceglie la CPU con la strategia base, a partire da ciò che vede. */
class CpuPlayerTest {

    private static final String CPU = "CPU";
    private static final Card RED_FIVE = new NumberCard(CardColor.RED, 5);
    private static final Card RED_SEVEN = new NumberCard(CardColor.RED, 7);
    private static final Card BLUE_THREE = new NumberCard(CardColor.BLUE, 3);

    private final CpuPlayer cpu = new CpuPlayer(CPU, new BasicCpuStrategy());

    /** Quello che vede la CPU quando tocca a lei, con il 5 rosso in cima. */
    private static VisibleState stateWith(List<Card> hand, GameState state, boolean canPass) {
        List<PlayerSummary> players = List.of(new PlayerSummary(CPU, hand.size()), new PlayerSummary("Tu", 7));
        return new VisibleState(RED_FIVE, CardColor.RED, CPU, hand, players, state, canPass);
    }

    private static VisibleState turnWith(Card... hand) {
        return stateWith(List.of(hand), GameState.IN_PROGRESS, false);
    }

    @Test
    void playsAPlayableCard() {
        assertEquals(List.of(new PlayCard(CPU, 1)), cpu.decide(turnWith(BLUE_THREE, RED_SEVEN, BLUE_THREE)));
    }

    @Test
    void drawsWhenNothingIsPlayable() {
        assertEquals(List.of(new Draw(CPU)), cpu.decide(turnWith(BLUE_THREE, BLUE_THREE, BLUE_THREE)));
    }

    @Test
    void keepsWildsForLast() {
        assertEquals(List.of(new PlayCard(CPU, 1)), cpu.decide(turnWith(new Wild(), RED_SEVEN, BLUE_THREE)));
    }

    @Test
    void playsAWildWhenItIsTheOnlyChoice() {
        assertEquals(List.of(new PlayCard(CPU, 1)), cpu.decide(turnWith(BLUE_THREE, new WildDrawFour(), BLUE_THREE)));
    }

    @Test
    void getsRidOfTheCardWorthMorePointsFirst() {
        Card redTwo = new NumberCard(CardColor.RED, 2);
        assertEquals(List.of(new PlayCard(CPU, 1)), cpu.decide(turnWith(redTwo, new Skip(CardColor.RED), BLUE_THREE)));
    }

    @Test
    void callsUnoBeforePlayingThePenultimateCard() {
        assertEquals(List.of(new CallUno(CPU), new PlayCard(CPU, 0)), cpu.decide(turnWith(RED_SEVEN, BLUE_THREE)));
    }

    @Test
    void afterDrawingPlaysTheDrawnCard() {
        VisibleState state = stateWith(List.of(BLUE_THREE, BLUE_THREE, RED_SEVEN), GameState.IN_PROGRESS, true);

        assertEquals(List.of(new PlayCard(CPU, 2)), cpu.decide(state));
    }

    @Test
    void afterDrawingPassesIfTheStrategyKeepsTheCard() {
        CpuPlayer keeper = new CpuPlayer(CPU, new FixedStrategy(Optional.empty()));
        VisibleState state = stateWith(List.of(BLUE_THREE, BLUE_THREE, RED_SEVEN), GameState.IN_PROGRESS, true);

        assertEquals(List.of(new Pass(CPU)), keeper.decide(state));
    }

    @Test
    void choosesTheColorItHasMostOf() {
        Card blueOne = new NumberCard(CardColor.BLUE, 1);
        VisibleState state = stateWith(List.of(blueOne, RED_SEVEN, BLUE_THREE), GameState.AWAITING_COLOR, false);

        assertEquals(List.of(new ChooseColor(CPU, CardColor.BLUE)), cpu.decide(state));
    }

    @Test
    void choosesRedWithOnlyWildsInHand() {
        VisibleState state = stateWith(List.of(new Wild(), new WildDrawFour()), GameState.AWAITING_COLOR, false);

        assertEquals(List.of(new ChooseColor(CPU, CardColor.RED)), cpu.decide(state));
    }

    @Test
    void refusesToDecideOnSomeoneElsesTurn() {
        List<PlayerSummary> players = List.of(new PlayerSummary(CPU, 1), new PlayerSummary("Tu", 1));
        VisibleState state = new VisibleState(RED_FIVE, CardColor.RED, "Tu", List.of(RED_SEVEN),
                players, GameState.IN_PROGRESS, false);

        assertThrows(IllegalArgumentException.class, () -> cpu.decide(state));
    }

    @Test
    void rejectsAStrategyThatPicksAnUnplayableCard() {
        CpuPlayer cheater = new CpuPlayer(CPU, new FixedStrategy(Optional.of(BLUE_THREE)));

        assertThrows(IllegalStateException.class, () -> cheater.decide(turnWith(RED_SEVEN, BLUE_THREE, BLUE_THREE)));
    }

    /** Una strategia che sceglie sempre la stessa cosa, per provare come CpuPlayer usa la risposta. */
    private record FixedStrategy(Optional<Card> choice) implements CpuStrategy {
        @Override
        public Optional<Card> choose(List<Card> playable) {
            return choice;
        }

        @Override
        public CardColor chooseColor(List<Card> hand) {
            return CardColor.GREEN;
        }
    }
}

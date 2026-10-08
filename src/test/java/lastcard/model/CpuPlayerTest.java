package lastcard.model;

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
        List<PlayerSummary> players = List.of(summary(CPU, hand.size()), summary("Tu", 7));
        return stateWith(hand, players, state, canPass);
    }

    private static VisibleState stateWith(List<Card> hand, List<PlayerSummary> players, GameState state, boolean canPass) {
        return new VisibleState(RED_FIVE, CardColor.RED, CPU, hand, players, state, canPass, 0);
    }

    private static PlayerSummary summary(String name, int cardCount) {
        return new PlayerSummary(name, cardCount, false, 0);
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
    void callsLastCardBeforePlayingThePenultimateCard() {
        assertEquals(List.of(new CallLastCard(CPU), new PlayCard(CPU, 0)), cpu.decide(turnWith(RED_SEVEN, BLUE_THREE)));
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
    void choosesTheRouletteColorWhenItIsTheVictim() {
        Card blueOne = new NumberCard(CardColor.BLUE, 1);
        VisibleState state = stateWith(List.of(blueOne, BLUE_THREE, RED_SEVEN), GameState.AWAITING_ROULETTE_COLOR, false);

        assertEquals(List.of(new ChooseColor(CPU, CardColor.BLUE)), cpu.decide(state));
    }

    @Test
    void swapsHandsWithTheOpponentWithFewestCards() {
        List<PlayerSummary> players = List.of(summary(CPU, 6), summary("Tu", 5), summary("Bea", 2), summary("Ugo", 9));
        VisibleState state = stateWith(List.of(RED_SEVEN), players, GameState.AWAITING_SWAP_TARGET, false);

        assertEquals(List.of(new SwapHands(CPU, "Bea")), cpu.decide(state));
    }

    @Test
    void neverSwapsWithAnEliminatedPlayer() {
        List<PlayerSummary> players = List.of(summary(CPU, 6),
                new PlayerSummary("Fuori", 0, true, 0), summary("Tu", 5));
        VisibleState state = stateWith(List.of(RED_SEVEN), players, GameState.AWAITING_SWAP_TARGET, false);

        assertEquals(List.of(new SwapHands(CPU, "Tu")), cpu.decide(state));
    }

    @Test
    void refusesToDecideOnSomeoneElsesTurn() {
        List<PlayerSummary> players = List.of(summary(CPU, 1), summary("Tu", 1));
        VisibleState state = new VisibleState(RED_FIVE, CardColor.RED, "Tu", List.of(RED_SEVEN),
                players, GameState.IN_PROGRESS, false, 0);

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

        @Override
        public String chooseSwapTarget(List<PlayerSummary> opponents) {
            return opponents.get(0).name();
        }
    }
}

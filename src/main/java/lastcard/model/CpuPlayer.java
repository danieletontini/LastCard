package lastcard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decide le mosse di un giocatore CPU guardando solo il VisibleState, cioè quello che vedrebbe un umano:
 * non può barare e funziona allo stesso modo con qualunque motore che produca quello stato.
 * Non muta nulla: chi la usa esegue le mosse restituite con Game.execute.
 */
public class CpuPlayer {

    private final String name;
    private final CpuStrategy strategy;

    public CpuPlayer(String name, CpuStrategy strategy) {
        this.name = Objects.requireNonNull(name, "name");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public String getName() {
        return name;
    }

    /**
     * Le mosse da eseguire in ordine adesso. Una sola, tranne quando gioca la penultima carta: allora prima dichiara Ultima!
     * Dopo una pescata o un jolly il turno non è finito: si richiama decide con il nuovo stato.
     */
    public List<Move> decide(VisibleState state) {
        Objects.requireNonNull(state, "state");
        if (!state.currentPlayerName().equals(name)) {
            throw new IllegalArgumentException("It is not " + name + "'s turn.");
        }

        List<Card> hand = state.currentHand();
        // Dopo un proprio jolly, o dopo la Roulette di chi precede: in entrambi i casi si dichiara un colore.
        if (state.state() == GameState.AWAITING_COLOR || state.state() == GameState.AWAITING_ROULETTE_COLOR) {
            return List.of(new ChooseColor(name, strategy.chooseColor(hand)));
        }
        if (state.state() == GameState.AWAITING_SWAP_TARGET) {
            return List.of(new SwapHands(name, strategy.chooseSwapTarget(opponents(state))));
        }
        if (state.state() != GameState.IN_PROGRESS) {
            throw new IllegalStateException("The game is not in progress.");
        }

        // Dopo aver pescato si può giocare solo la carta pescata, che è l'ultima della mano.
        int drawnIndex = hand.size() - 1;
        List<Card> playable = state.canPass() ? List.of(hand.get(drawnIndex)) : playableCards(state);

        Optional<Card> choice = strategy.choose(playable);
        if (choice.isEmpty()) {
            return List.of(state.canPass() ? new Pass(name) : new Draw(name));
        }
        if (!playable.contains(choice.get())) {
            throw new IllegalStateException("The strategy chose a card that is not playable.");
        }

        int index = state.canPass() ? drawnIndex : hand.indexOf(choice.get());
        Move play = new PlayCard(name, index);
        return hand.size() == 2 ? List.of(new CallLastCard(name), play) : List.of(play);
    }

    // Gli avversari ancora in gioco: con loro si può scambiare la mano.
    private List<PlayerSummary> opponents(VisibleState state) {
        List<PlayerSummary> opponents = new ArrayList<>();
        for (PlayerSummary player : state.players()) {
            if (!player.name().equals(name) && !player.eliminated()) {
                opponents.add(player);
            }
        }
        return opponents;
    }

    private static List<Card> playableCards(VisibleState state) {
        List<Card> playable = new ArrayList<>();
        for (Card card : state.currentHand()) {
            if (card.isPlayable(state.top(), state.activeColor())) {
                playable.add(card);
            }
        }
        return playable;
    }
}

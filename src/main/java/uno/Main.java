package uno;

import java.util.List;
import java.util.Scanner;

import uno.controller.GameController;
import uno.model.BasicCpuStrategy;
import uno.model.CpuPlayer;
import uno.model.Game;
import uno.view.console.CardFormatter;
import uno.view.console.ConsoleView;

public class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        ConsoleView view = new ConsoleView(new CardFormatter(), in);

        Game game = new Game(List.of("Tu", "CPU 1", "CPU 2"));
        List<CpuPlayer> cpus = List.of(
                new CpuPlayer("CPU 1", new BasicCpuStrategy()),
                new CpuPlayer("CPU 2", new BasicCpuStrategy()));

        GameController controller = new GameController(game, view, cpus);
        view.run(controller);
    }
}

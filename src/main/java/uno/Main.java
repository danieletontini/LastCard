package uno;

import java.util.List;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import uno.controller.GameController;
import uno.model.BasicCpuStrategy;
import uno.model.CpuPlayer;
import uno.model.Game;
import uno.view.console.CardFormatter;
import uno.view.console.ConsoleView;
import uno.view.gui.MainWindow;

public class Main {

    private Main() {
    }

    /** Sceglie la vista: --console per il terminale, --gui (o nessun argomento) per la finestra. */
    public static void main(String[] args) {
        String mode = "--gui";
        if (args.length > 0) {
            mode = args[0];
        }

        if (mode.equals("--console")) {
            runConsole();
        } else if (mode.equals("--gui")) {
            runGui();
        } else {
            System.out.println("Argomento non riconosciuto: " + mode);
            System.out.println("Uso: --console oppure --gui (predefinito)");
        }
    }

    private static void runConsole() {
        Scanner in = new Scanner(System.in);
        ConsoleView view = new ConsoleView(new CardFormatter(), in);

        Game game = new Game(List.of("Tu", "CPU 1", "CPU 2"));
        List<CpuPlayer> cpus = List.of(
                new CpuPlayer("CPU 1", new BasicCpuStrategy()),
                new CpuPlayer("CPU 2", new BasicCpuStrategy()));

        // La vista va registrata prima di avviare la partita, per ricevere anche la prima carta scoperta.
        game.addObserver(view);
        GameController controller = new GameController(game, view, cpus);
        view.run(controller);
    }

    // La finestra va creata sul thread grafico di Swing (EDT), non sul thread del main.
    private static void runGui() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow window = new MainWindow();
                window.setVisible(true);
            }
        });
    }
}

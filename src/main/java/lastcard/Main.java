package lastcard;

import java.util.List;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import lastcard.controller.GameController;
import lastcard.model.BasicCpuStrategy;
import lastcard.model.CpuPlayer;
import lastcard.model.Game;
import lastcard.view.audio.AudioPlayer;
import lastcard.view.audio.GameSounds;
import lastcard.view.console.CardFormatter;
import lastcard.view.console.ConsoleView;
import lastcard.view.gui.MainWindow;
import lastcard.view.gui.Settings;

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
        // I suoni della partita sono un altro observer: la console e il modello non sanno che esiste l'audio.
        AudioPlayer audio = new AudioPlayer();
        audio.setVolume(new Settings().effectiveVolume());
        game.addObserver(new GameSounds(audio::play));
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

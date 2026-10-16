package lastcard;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import lastcard.controller.AppController;
import lastcard.controller.GameController;
import lastcard.controller.GameSettings;
import lastcard.model.BasicCpuStrategy;
import lastcard.model.CpuPlayer;
import lastcard.model.Game;
import lastcard.view.audio.AudioPlayer;
import lastcard.view.audio.GameSounds;
import lastcard.view.console.CardFormatter;
import lastcard.view.console.ConsoleMenu;
import lastcard.view.console.ConsoleView;
import lastcard.view.gui.MainWindow;
import lastcard.view.gui.Settings;

public class Main {

    private static final String HUMAN_NAME = "Tu";

    private Main() {
    }

    /**
     * Sceglie la vista: --console per il terminale, --gui (o nessun argomento) per la finestra.
     * Nel terminale modalità, numero di CPU e obiettivo si scelgono dal menu iniziale.
     */
    public static void main(String[] args) {
        String mode = "--gui";
        if (args.length > 0) {
            mode = args[0];
        }

        if (args.length > 1) {
            printUsage();
        } else if (mode.equals("--console")) {
            runConsole();
        } else if (mode.equals("--gui")) {
            runGui();
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Argomenti non riconosciuti.");
        System.out.println("Uso: --gui (predefinito) oppure --console");
    }

    private static void runConsole() {
        Scanner in = new Scanner(System.in);
        ConsoleView view = new ConsoleView(new CardFormatter(), in);

        GameSettings settings = new ConsoleMenu(in).askSettings();
        List<String> names = new ArrayList<>();
        List<CpuPlayer> cpus = new ArrayList<>();
        names.add(HUMAN_NAME);
        for (int i = 1; i <= settings.opponents(); i++) {
            String name = "CPU " + i;
            names.add(name);
            cpus.add(new CpuPlayer(name, new BasicCpuStrategy()));
        }
        Game game = new Game(names, AppController.toRules(settings));

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

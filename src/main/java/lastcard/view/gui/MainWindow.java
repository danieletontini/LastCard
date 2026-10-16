package lastcard.view.gui;

import java.awt.Dimension;

import javax.swing.JFrame;

import lastcard.controller.AppController;
import lastcard.controller.GameSettings;
import lastcard.view.MenuView;
import lastcard.view.TableView;
import lastcard.view.audio.AudioPlayer;
import lastcard.view.audio.GameSounds;
import lastcard.view.audio.Music;
import lastcard.view.audio.SoundEffect;

/** L'unica finestra della GUI, 960x640 o a schermo intero. Il contenuto è il pannello del Navigator, con tutte le schermate. */
public class MainWindow extends JFrame implements MenuView {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    private final Navigator navigator;
    private final TablePanel table;

    public MainWindow() {
        super("Ultima Carta");
        Settings settings = new Settings();
        // Ogni avvio parte a schermo intero e con il volume a 67, qualunque scelta ci fosse alla chiusura.
        settings.applyStartupDefaults();

        // Un solo AudioPlayer per tutta la finestra, con il volume delle Opzioni.
        AudioPlayer audio = new AudioPlayer();
        audio.setVolume(settings.effectiveVolume());
        ButtonFactory.setClickSound(() -> audio.play(SoundEffect.BUTTON_CLICK));

        this.navigator = new Navigator();
        // Il tavolo passa gli eventi della partita anche ai suoni: la partita non sa che esiste l'audio.
        // Il suo form Opzioni ha le stesse opzioni della schermata Opzioni, sulle stesse impostazioni.
        this.table = new TablePanel(navigator, new GameSounds(audio::play), new OptionsControls(settings, this, audio));
        // Il controller del menu crea la partita, la affida al controller del tavolo e chiede di mostrare il tavolo.
        // Le mosse delle CPU sono distanziate da un timer di Swing.
        AppController controller = new AppController(this, new SwingTurnScheduler());
        navigator.register(Screen.MAIN_MENU, new MainMenuPanel(navigator));
        navigator.register(Screen.PLAY, new PlayPanel(navigator));
        navigator.register(Screen.NEW_GAME, new NewGamePanel(navigator, controller));
        navigator.register(Screen.TABLE, table);
        navigator.register(Screen.RULES, new RulesPanel(navigator));
        navigator.register(Screen.OPTIONS, new OptionsPanel(navigator, settings, this, audio));
        navigator.show(Screen.MAIN_MENU);
        // La musica del menu accompagna anche Regole e Opzioni, che si aprono dal menu.
        audio.playMusic(Music.MENU);

        setContentPane(navigator.getContainer());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Sotto i 960x640 le schermate non stanno: la finestra si può ingrandire, non rimpicciolire.
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setFullScreen(settings.isFullScreen());
    }

    @Override
    public TableView table() {
        return table;
    }

    @Override
    public void showTable(GameSettings settings) {
        navigator.show(Screen.TABLE);
    }

    /**
     * Schermo intero: finestra senza bordi grande quanto lo schermo, barra delle applicazioni compresa.
     * I bordi si possono togliere o rimettere solo a finestra chiusa, quindi la si chiude e la si riapre.
     */
    public void setFullScreen(boolean fullScreen) {
        boolean wasVisible = isVisible();
        dispose();
        setUndecorated(fullScreen);
        if (fullScreen) {
            setBounds(getGraphicsConfiguration().getBounds());
        } else {
            setSize(WIDTH, HEIGHT);
            setLocationRelativeTo(null);
        }
        setVisible(wasVisible);
    }
}

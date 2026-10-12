package lastcard.view.gui;

import javax.swing.JFrame;

import lastcard.view.audio.AudioPlayer;
import lastcard.view.audio.Music;
import lastcard.view.audio.SoundEffect;

/** L'unica finestra della GUI, 960x640 o a schermo intero. Il contenuto è il pannello del Navigator, con tutte le schermate. */
public class MainWindow extends JFrame {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    public MainWindow() {
        super("Ultima Carta");
        Settings settings = new Settings();

        // Un solo AudioPlayer per tutta la finestra, con il volume salvato nelle Opzioni.
        AudioPlayer audio = new AudioPlayer();
        audio.setVolume(settings.effectiveVolume());
        ButtonFactory.setClickSound(() -> audio.play(SoundEffect.BUTTON_CLICK));

        Navigator navigator = new Navigator();
        navigator.register(Screen.MAIN_MENU, new MainMenuPanel(navigator));
        navigator.register(Screen.PLAY, new PlayPanel(navigator));
        navigator.register(Screen.NEW_GAME, new NewGamePanel(navigator));
        navigator.register(Screen.RULES, new RulesPanel(navigator));
        navigator.register(Screen.OPTIONS, new OptionsPanel(navigator, settings, this, audio));
        navigator.show(Screen.MAIN_MENU);
        // La musica del menu accompagna anche Regole e Opzioni, che si aprono dal menu.
        audio.playMusic(Music.MENU);

        setContentPane(navigator.getContainer());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        // Si riparte con la scelta dell'ultima volta.
        setFullScreen(settings.isFullScreen());
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

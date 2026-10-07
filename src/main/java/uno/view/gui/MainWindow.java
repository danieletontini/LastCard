package uno.view.gui;

import javax.swing.JFrame;

/** L'unica finestra della GUI, 960x640. Il contenuto è il pannello del Navigator, con tutte le schermate. */
public class MainWindow extends JFrame {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    public MainWindow() {
        super("Ultima Carta");
        Navigator navigator = new Navigator();
        navigator.register(Screen.MAIN_MENU, new MainMenuPanel(navigator));
        navigator.register(Screen.RULES, new RulesPanel(navigator));
        navigator.show(Screen.MAIN_MENU);

        setContentPane(navigator.getContainer());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
    }
}

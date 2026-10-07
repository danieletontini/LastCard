package uno.view.gui;

import javax.swing.JFrame;

/** L'unica finestra della GUI, 960x640. Per ora mostra solo il menu principale. */
public class MainWindow extends JFrame {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    public MainWindow() {
        super("UNO");
        setContentPane(new MainMenuPanel());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
    }
}

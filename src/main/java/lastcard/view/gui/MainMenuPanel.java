package lastcard.view.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Il menu principale: titolo, poi Gioca, Regole, Opzioni, Esci, sullo sfondo a carte sparse. */
public class MainMenuPanel extends ArcadeBackgroundPanel {

    private static final int BUTTON_WIDTH = 320;
    private static final int BUTTON_GAP = 16;
    private static final int TITLE_GAP = 56;

    public MainMenuPanel(final Navigator navigator) {
        // GridBagLayout tiene titolo e pulsanti al centro della finestra, a qualunque dimensione.
        super(new GridBagLayout(), MENU_BACKGROUND);

        JButton play = ButtonFactory.createButton("Gioca", ArcadeButtonUI.Variant.PRIMARY);
        JButton rules = ButtonFactory.createButton("Regole");
        JButton options = ButtonFactory.createButton("Opzioni");
        JButton exit = ButtonFactory.createButton("Esci", ArcadeButtonUI.Variant.QUIET);

        play.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.show(Screen.PLAY);
            }
        });

        rules.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.show(Screen.RULES);
            }
        });

        options.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.show(Screen.OPTIONS);
            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });

        // Colonna dei pulsanti: tutti larghi uguali. Trasparente, così si vede lo sfondo.
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, BUTTON_GAP));
        buttons.setOpaque(false);
        buttons.add(play);
        buttons.add(rules);
        buttons.add(options);
        buttons.add(exit);
        Dimension one = play.getPreferredSize();
        buttons.setPreferredSize(new Dimension(BUTTON_WIDTH, 4 * one.height + 3 * BUTTON_GAP));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, TITLE_GAP, 0);
        add(new ArcadeTitle(52f, "ULTIMA", "CARTA"), c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        add(buttons, c);
    }

    // Chiude la finestra che contiene il menu; con EXIT_ON_CLOSE il programma termina.
    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }
}

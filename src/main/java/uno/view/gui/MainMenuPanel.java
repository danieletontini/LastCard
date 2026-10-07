package uno.view.gui;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Il menu principale: Gioca, Regole, Opzioni, Esci. Per ora funzionano Regole ed Esci. */
public class MainMenuPanel extends JPanel {

    public MainMenuPanel(final Navigator navigator) {
        // GridBagLayout senza vincoli tiene la colonna dei pulsanti al centro della finestra.
        super(new GridBagLayout());

        JPanel column = new JPanel(new GridLayout(0, 1, 0, 10));
        column.add(new JLabel("Ultima Carta", SwingConstants.CENTER));

        JButton play = ButtonFactory.createButton("Gioca");
        JButton rules = ButtonFactory.createButton("Regole");
        JButton options = ButtonFactory.createButton("Opzioni");
        JButton exit = ButtonFactory.createButton("Esci");

        // Le schermate di Gioca e Opzioni non esistono ancora.
        play.setEnabled(false);
        options.setEnabled(false);

        rules.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.show(Screen.RULES);
            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });

        column.add(play);
        column.add(rules);
        column.add(options);
        column.add(exit);
        add(column);
    }

    // Chiude la finestra che contiene il menu; con EXIT_ON_CLOSE il programma termina.
    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }
}

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

/** Il menu principale: Gioca, Regole, Opzioni, Esci. Per ora funziona solo Esci. */
public class MainMenuPanel extends JPanel {

    public MainMenuPanel() {
        // GridBagLayout senza vincoli tiene la colonna dei pulsanti al centro della finestra.
        super(new GridBagLayout());

        JPanel column = new JPanel(new GridLayout(0, 1, 0, 10));
        column.add(new JLabel("UNO", SwingConstants.CENTER));

        JButton play = new JButton("Gioca");
        JButton rules = new JButton("Regole");
        JButton options = new JButton("Opzioni");
        JButton exit = new JButton("Esci");

        // Le schermate di Gioca, Regole e Opzioni non esistono ancora.
        play.setEnabled(false);
        rules.setEnabled(false);
        options.setEnabled(false);

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

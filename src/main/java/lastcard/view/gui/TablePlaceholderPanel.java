package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import lastcard.controller.GameSettings;

/** Segnaposto del tavolo di gioco, che farà B: per ora mostra le impostazioni con cui è stata creata la partita. */
public class TablePlaceholderPanel extends JPanel {

    private final JLabel settingsLabel;

    public TablePlaceholderPanel(final Navigator navigator) {
        super(new BorderLayout());
        this.settingsLabel = new JLabel("", SwingConstants.CENTER);

        JButton menu = ButtonFactory.createButton("Torna al menu");
        menu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.show(Screen.MAIN_MENU);
            }
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(menu);

        add(new JLabel("Tavolo (segnaposto)", SwingConstants.CENTER), BorderLayout.NORTH);
        add(settingsLabel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void showSettings(GameSettings settings) {
        settingsLabel.setText("<html>Modalità: " + ChoiceLabels.MODES.get(settings.mode())
                + "<br>Avversari: " + settings.opponents()
                + "<br>Obiettivo: " + ChoiceLabels.GOALS.get(settings.goal()) + "</html>");
    }
}

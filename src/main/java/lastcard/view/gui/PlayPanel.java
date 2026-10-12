package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** La schermata Gioca: giocatore singolo o multigiocatore. */
public class PlayPanel extends JPanel {

    public PlayPanel(final Navigator navigator) {
        super(new BorderLayout());

        JButton back = ButtonFactory.createButton("Indietro");
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.back();
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.add(back, BorderLayout.WEST);
        header.add(new JLabel("Gioca", SwingConstants.CENTER), BorderLayout.CENTER);

        // Pulsanti grandi con il titolo e una riga di descrizione: l'HTML permette di andare a capo nel pulsante.
        JButton single = ButtonFactory.createButton(
                "<html><b>Giocatore singolo</b><br>Tu contro la CPU, da uno a nove avversari.</html>");
        JButton multi = ButtonFactory.createButton(
                "<html><b>Multigiocatore</b><br>Contro altre persone, sulla stessa rete locale.</html>");
        single.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.show(Screen.NEW_GAME);
            }
        });
        // La schermata Multigiocatore non esiste ancora.
        multi.setEnabled(false);

        JPanel column = new JPanel(new GridLayout(0, 1, 0, 15));
        column.add(single);
        column.add(multi);
        column.add(new JLabel("<html>Il multigiocatore richiede che tutti i computer siano collegati alla stessa rete: "
                + "uno ospita la partita, gli altri si collegano al suo indirizzo.</html>"));
        // GridBagLayout senza vincoli tiene la colonna al centro.
        JPanel center = new JPanel(new GridBagLayout());
        center.add(column);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }
}

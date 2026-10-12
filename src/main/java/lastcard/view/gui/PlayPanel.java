package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** La schermata Gioca: giocatore singolo o multigiocatore, sullo stesso sfondo del menu. */
public class PlayPanel extends ArcadeBackgroundPanel {

    public PlayPanel(final Navigator navigator) {
        super(new BorderLayout(), MENU_BACKGROUND);

        JButton back = ButtonFactory.createButton("Indietro", ArcadeButtonUI.Variant.QUIET);
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.back();
            }
        });
        // Pannelli interni trasparenti, così si vede lo sfondo.
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(back, BorderLayout.WEST);
        header.add(new ArcadeTitle(22f, "GIOCA"), BorderLayout.CENTER);
        // Uno spazio vuoto largo quanto Indietro, a destra: così il titolo sta al centro della finestra.
        header.add(Box.createHorizontalStrut(back.getPreferredSize().width), BorderLayout.EAST);

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
        column.setOpaque(false);
        column.add(single);
        column.add(multi);
        JLabel note = new JLabel("<html>Il multigiocatore richiede che tutti i computer siano collegati alla stessa rete: "
                + "uno ospita la partita, gli altri si collegano al suo indirizzo.</html>");
        note.setForeground(ArcadeButtonUI.TEXT_MUTED);
        column.add(note);
        // GridBagLayout senza vincoli tiene la colonna al centro.
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(column);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }
}

package uno.view.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/** Le regole della modalità classica e di quella No Mercy, scelte con due pulsanti a selezione esclusiva. */
public class RulesPanel extends JPanel {

    // I nomi delle due schede nel CardLayout del contenuto.
    private static final String CLASSIC = "CLASSIC";
    private static final String NO_MERCY = "NO_MERCY";

    private final CardLayout contentLayout;
    private final JPanel content;
    private final JScrollPane classicScroll;
    private final JScrollPane noMercyScroll;

    public RulesPanel(final Navigator navigator) {
        super(new BorderLayout());

        // In alto: Indietro e il titolo.
        JButton back = ButtonFactory.createButton("Indietro");
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.back();
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.add(back, BorderLayout.WEST);
        header.add(new JLabel("Regole", SwingConstants.CENTER), BorderLayout.CENTER);

        // Sotto il titolo: Classica / No Mercy, uno solo premuto alla volta.
        JToggleButton classicButton = ButtonFactory.createToggle("Classica");
        JToggleButton noMercyButton = ButtonFactory.createToggle("No Mercy");
        ButtonGroup group = new ButtonGroup();
        group.add(classicButton);
        group.add(noMercyButton);
        classicButton.setSelected(true);
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER));
        tabs.add(classicButton);
        tabs.add(noMercyButton);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(header);
        top.add(tabs);

        // Il contenuto: i due testi uno sopra l'altro, se ne vede uno alla volta.
        this.classicScroll = new JScrollPane(createTextPane(RulesLoader.CLASSIC_PATH));
        this.noMercyScroll = new JScrollPane(createTextPane(RulesLoader.NO_MERCY_PATH));
        this.contentLayout = new CardLayout();
        this.content = new JPanel(contentLayout);
        content.add(classicScroll, CLASSIC);
        content.add(noMercyScroll, NO_MERCY);

        classicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRules(CLASSIC, classicScroll);
            }
        });
        noMercyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRules(NO_MERCY, noMercyScroll);
            }
        });

        add(top, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    // Mostra una scheda e riporta il suo testo in cima.
    private void showRules(String name, JScrollPane scroll) {
        contentLayout.show(content, name);
        scroll.getVerticalScrollBar().setValue(0);
    }

    // Un testo semplice non modificabile, che va a capo da solo fra una parola e l'altra.
    // Se il file non si legge, al suo posto compare un messaggio d'errore.
    private static JTextArea createTextPane(String resourcePath) {
        String text;
        try {
            text = RulesLoader.load(resourcePath);
        } catch (IOException e) {
            text = "Impossibile caricare le regole (" + resourcePath + ").";
        }

        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        // Dopo aver messo il testo il cursore può finire in fondo e trascinare lo scroll: lo riportiamo all'inizio.
        area.setCaretPosition(0);
        return area;
    }
}

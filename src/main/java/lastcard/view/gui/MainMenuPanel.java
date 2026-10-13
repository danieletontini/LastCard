package lastcard.view.gui;

import java.awt.Component;
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

    // Le misure a grandezza normale, cioè nella finestra da 960x640: più grande, tutto cresce in proporzione.
    private static final int BUTTON_WIDTH = 320;
    private static final int BUTTON_GAP = 16;
    private static final int TITLE_GAP = 56;
    private static final float TITLE_SIZE = 52f;

    private final ArcadeTitle title;
    private final JPanel buttons;
    private final GridLayout buttonLayout;
    private final GridBagConstraints titleConstraints;
    private float scale;

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
        this.buttonLayout = new GridLayout(0, 1, 0, BUTTON_GAP);
        this.buttons = new JPanel(buttonLayout);
        buttons.setOpaque(false);
        buttons.add(play);
        buttons.add(rules);
        buttons.add(options);
        buttons.add(exit);

        this.title = new ArcadeTitle(TITLE_SIZE, "ULTIMA", "CARTA");
        this.titleConstraints = new GridBagConstraints();
        titleConstraints.gridx = 0;
        titleConstraints.gridy = 0;
        add(title, titleConstraints);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        add(buttons, c);

        this.scale = 0f;
        applyScale(1f);
        // A schermo intero la finestra è molto più grande di 960x640: titolo e pulsanti la seguono.
        ScreenScale.follow(this, this::applyScale);
    }

    // Titolo, testo dei pulsanti e spazi moltiplicati per la stessa scala: le proporzioni del menu non cambiano.
    private void applyScale(float newScale) {
        if (newScale == scale) {
            return;
        }
        scale = newScale;
        title.setFontSize(TITLE_SIZE * scale);
        int gap = Math.round(BUTTON_GAP * scale);
        buttonLayout.setVgap(gap);
        for (Component button : buttons.getComponents()) {
            button.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        }
        int buttonHeight = buttons.getComponent(0).getPreferredSize().height;
        int count = buttons.getComponentCount();
        buttons.setPreferredSize(new Dimension(Math.round(BUTTON_WIDTH * scale),
                count * buttonHeight + (count - 1) * gap));

        titleConstraints.insets = new Insets(0, 0, Math.round(TITLE_GAP * scale), 0);
        ((GridBagLayout) getLayout()).setConstraints(title, titleConstraints);
        revalidate();
        repaint();
    }

    // Chiude la finestra che contiene il menu; con EXIT_ON_CLOSE il programma termina.
    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }
}

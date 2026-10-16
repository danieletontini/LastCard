package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import lastcard.model.CardColor;
import lastcard.model.PlayerSummary;

/**
 * Il form che si apre sopra il tavolo quando serve una scelta: il colore dopo un jolly (o per la Roulette),
 * l'avversario con cui scambiare la mano dopo un 7, cosa fare a fine partita, le opzioni durante la partita.
 *
 * Copre tutto il tavolo con un velo scuro, che intercetta i clic: finché il form è aperto non si gioca.
 * Al centro c'è un riquadro con la domanda e i pulsanti; premuto un pulsante il form si chiude da solo.
 */
final class ChoiceOverlay extends JPanel {

    private static final Color VEIL = new Color(0f, 0f, 0f, 0.55f);
    private static final Color BOX = new Color(0x111416);
    private static final float TITLE_SIZE = 18f;
    private static final float SUBTITLE_SIZE = 10f;
    private static final int PADDING = 28;
    private static final int GAP = 14;
    private static final int ARC = 10;

    private final JPanel box;

    ChoiceOverlay() {
        super(new GridBagLayout());
        setOpaque(false);
        setVisible(false);
        // Un ascoltatore qualsiasi basta perché i clic sul velo non arrivino al tavolo sotto.
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });

        this.box = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double w = getWidth();
                    double h = getHeight();
                    g.setColor(BOX);
                    g.fill(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));
                    g.setStroke(new BasicStroke(2f));
                    g.setColor(ArcadeButtonUI.CREAM);
                    g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, ARC, ARC));
                } finally {
                    g.dispose();
                }
            }
        };
        box.setOpaque(false);
        add(box);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(VEIL);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** La scelta del colore: con roulette vero la si fa per la Roulette giocata da chi precede. */
    void showColors(boolean roulette, float scale, Consumer<CardColor> onChoice) {
        List<JButton> buttons = new ArrayList<>();
        for (CardColor color : ArcadePalette.playableColors()) {
            JButton button = ButtonFactory.createButton(TableTexts.color(color));
            button.putClientProperty(ArcadeButtonUI.SWATCH_KEY, ArcadePalette.field(color));
            button.addActionListener(e -> {
                close();
                onChoice.accept(color);
            });
            buttons.add(button);
        }
        String subtitle = roulette
                ? "Roulette: scoprirai carte finché esce questo colore"
                : "Il prossimo giocatore dovrà rispondere a questo colore";
        open("SCEGLI IL COLORE", subtitle, buttons, 2, scale);
    }

    /** Scambio 7: un pulsante per ogni avversario ancora in gioco, con quante carte ha. */
    void showPlayers(List<PlayerSummary> opponents, float scale, Consumer<String> onChoice) {
        List<JButton> buttons = new ArrayList<>();
        for (PlayerSummary opponent : opponents) {
            JButton button = ButtonFactory.createButton(opponent.name() + " - " + TableTexts.cards(opponent.cardCount()));
            button.addActionListener(e -> {
                close();
                onChoice.accept(opponent.name());
            });
            buttons.add(button);
        }
        open("CON CHI SCAMBI LA MANO?", "Hai giocato un 7: la tua mano va a chi scegli", buttons,
                opponents.size() > 4 ? 2 : 1, scale);
    }

    /** Un messaggio con le sue azioni, come la fine della partita. Il primo pulsante è quello principale. */
    void showMessage(String title, String subtitle, List<String> labels, List<Runnable> actions, float scale) {
        open(title, subtitle, null, actionButtons(labels, actions), labels.size(), scale);
    }

    /** Un contenuto qualsiasi sotto il titolo, come le opzioni, con le sue azioni in riga sotto. */
    void showContent(String title, JComponent content, List<String> labels, List<Runnable> actions, float scale) {
        open(title, null, content, actionButtons(labels, actions), labels.size(), scale);
    }

    // Un pulsante per azione: il primo è quello principale, e ognuno chiude il form prima di agire.
    private List<JButton> actionButtons(List<String> labels, List<Runnable> actions) {
        List<JButton> buttons = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            Runnable action = actions.get(i);
            JButton button = ButtonFactory.createButton(labels.get(i),
                    i == 0 ? ArcadeButtonUI.Variant.PRIMARY : ArcadeButtonUI.Variant.SECONDARY);
            button.addActionListener(e -> {
                close();
                action.run();
            });
            buttons.add(button);
        }
        return buttons;
    }

    void close() {
        setVisible(false);
        box.removeAll();
    }

    boolean isOpen() {
        return isVisible();
    }

    private void open(String title, String subtitle, List<JButton> buttons, int columns, float scale) {
        open(title, subtitle, null, buttons, columns, scale);
    }

    private void open(String title, String subtitle, JComponent content, List<JButton> buttons, int columns,
            float scale) {
        box.removeAll();
        int padding = Math.round(PADDING * scale);
        int gap = Math.round(GAP * scale);
        box.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, gap, 0);
        box.add(new ArcadeTitle(TITLE_SIZE * scale, title), c);

        if (subtitle != null) {
            JLabel label = new JLabel(TableTexts.upper(subtitle), SwingConstants.CENTER);
            label.setFont(ArcadeFont.of(SUBTITLE_SIZE * scale));
            label.setForeground(ArcadeButtonUI.TEXT_MUTED);
            c.gridy++;
            c.insets = new Insets(0, 0, gap * 2, 0);
            box.add(label, c);
        }

        if (content != null) {
            c.gridy++;
            c.insets = new Insets(gap, 0, gap * 3, 0);
            box.add(content, c);
        }

        JPanel grid = new JPanel(new GridLayout(0, columns, gap, gap));
        grid.setOpaque(false);
        for (JButton button : buttons) {
            button.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
            grid.add(button);
        }
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        box.add(grid, c);

        setVisible(true);
        revalidate();
        repaint();
    }
}

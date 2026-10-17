package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import lastcard.controller.GameGoal;
import lastcard.controller.GameMode;
import lastcard.controller.GameSettings;
import lastcard.controller.MenuActions;

/**
 * Nuova partita: modalità, numero di avversari e obiettivo, poi Inizia.
 * Il pannello resta lo stesso per tutta la vita della finestra, quindi le scelte restano se si torna indietro e si rientra.
 * Inizia raccoglie le scelte in un GameSettings e lo passa al controller, che crea la partita.
 *
 * Grafica come Regole e Opzioni: sfondo del cabinato, titolo a pixel, le scelte in un riquadro scuro al centro.
 * Ogni scelta ha il suo nome sopra i pulsanti e la descrizione sotto; in fondo il riepilogo e Inizia.
 */
public class NewGamePanel extends ArcadeBackgroundPanel {

    // Le misure a grandezza normale, nella finestra da 960x640: a schermo intero crescono in proporzione.
    private static final float TITLE_SIZE = 22f;
    private static final float LABEL_SIZE = 11f;
    private static final float DESCRIPTION_SIZE = 9f;
    private static final float VALUE_SIZE = 16f;
    private static final int BOX_PADDING = 24;
    private static final int GAP = 10;
    private static final int SECTION_GAP = 22;

    private final ChoiceGroup<GameMode> modeGroup;
    private final ChoiceGroup<GameGoal> goalGroup;
    private final JLabel modeDescription;
    private final JLabel goalDescription;
    private final JLabel opponentsValue;
    private final JLabel playersAtTable;
    private final JButton minus;
    private final JButton plus;
    private final JLabel summary;
    private int opponents;

    // Per la scala: i componenti di cui cambiano font o spazi.
    private final JButton back;
    private final JButton start;
    private final ArcadeTitle title;
    private final Box.Filler backBalance;
    private final JLabel[] sectionLabels;
    private final FlowLayout opponentsLayout;
    private final ArcadeBox box;
    private final GridBagLayout formLayout;
    private final BorderLayout titleLayout;

    public NewGamePanel(final Navigator navigator, final MenuActions actions) {
        super(new BorderLayout(), MENU_BACKGROUND);
        this.opponents = GameSettings.MIN_OPPONENTS;

        // In alto: Indietro a sinistra; il titolo sta al centro, sopra il riquadro.
        this.back = ButtonFactory.createButton("Indietro", ArcadeButtonUI.Variant.QUIET);
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.back();
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(back, BorderLayout.WEST);
        // Uno spazio vuoto largo quanto Indietro, a destra, come nelle altre schermate.
        Dimension balance = new Dimension(back.getPreferredSize().width, 0);
        this.backBalance = new Box.Filler(balance, balance, balance);
        header.add(backBalance, BorderLayout.EAST);
        this.title = new ArcadeTitle(TITLE_SIZE, "NUOVA PARTITA");

        // Le scelte, con i valori iniziali: Classica, 1 avversario, Mano singola.
        this.modeGroup = new ChoiceGroup<>(GameMode.values(), ChoiceLabels.MODES, GameMode.CLASSIC);
        this.goalGroup = new ChoiceGroup<>(GameGoal.values(), ChoiceLabels.goals(GameMode.CLASSIC),
                GameGoal.SINGLE_HAND);
        this.modeDescription = mutedLabel();
        this.goalDescription = mutedLabel();
        this.opponentsValue = new JLabel("", SwingConstants.CENTER);
        opponentsValue.setForeground(ArcadeButtonUI.CREAM);
        this.playersAtTable = mutedLabel();
        this.minus = ButtonFactory.createButton("-");
        this.plus = ButtonFactory.createButton("+");
        this.summary = mutedLabel();

        ActionListener refresh = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
        modeGroup.addActionListener(refresh);
        goalGroup.addActionListener(refresh);
        minus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeOpponents(-1);
            }
        });
        plus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeOpponents(1);
            }
        });

        this.opponentsLayout = new FlowLayout(FlowLayout.LEFT, GAP, 0);
        JPanel opponentsRow = new JPanel(opponentsLayout);
        opponentsRow.setOpaque(false);
        opponentsRow.add(minus);
        opponentsRow.add(opponentsValue);
        opponentsRow.add(plus);
        opponentsRow.add(playersAtTable);

        // Il riquadro: per ogni scelta il nome, i pulsanti e la descrizione; in fondo riepilogo e Inizia.
        this.formLayout = new GridBagLayout();
        this.box = new ArcadeBox(formLayout);
        this.sectionLabels = new JLabel[] { sectionLabel("Modalità"), sectionLabel("Avversari"), sectionLabel("Obiettivo") };
        addSection(0, sectionLabels[0], modeGroup, modeDescription);
        addSection(3, sectionLabels[1], opponentsRow, null);
        addSection(6, sectionLabels[2], goalGroup, goalDescription);

        this.start = ButtonFactory.createButton("Inizia", ArcadeButtonUI.Variant.PRIMARY);
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actions.startGame(currentSettings());
            }
        });
        JPanel bottom = new JPanel(new BorderLayout(GAP * 2, 0));
        bottom.setOpaque(false);
        bottom.add(summary, BorderLayout.CENTER);
        bottom.add(start, BorderLayout.EAST);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 9;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        box.add(bottom, c);

        // Il titolo appena sopra il riquadro: il blocco intero sta al centro della finestra.
        this.titleLayout = new BorderLayout(0, SECTION_GAP);
        JPanel titled = new JPanel(titleLayout);
        titled.setOpaque(false);
        titled.add(title, BorderLayout.NORTH);
        titled.add(box, BorderLayout.CENTER);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(titled);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        refresh();

        // A schermo intero la schermata è molto più grande di 960x640: testi e controlli la seguono.
        ScreenScale.follow(this, this::applyScale);
        applyScale(1f);
    }

    private static JLabel mutedLabel() {
        JLabel label = new JLabel();
        label.setForeground(ArcadeButtonUI.TEXT_MUTED);
        return label;
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(TableTexts.upper(text));
        label.setForeground(ArcadeButtonUI.CREAM);
        return label;
    }

    // Una scelta occupa tre righe della griglia: il nome, il controllo e, sotto, la sua descrizione.
    private void addSection(int gridRow, JLabel label, JComponent control, JLabel description) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = gridRow;
        box.add(label, c);
        c.gridy = gridRow + 1;
        box.add(control, c);
        if (description != null) {
            c.gridy = gridRow + 2;
            box.add(description, c);
        }
    }

    // Tutte le misure moltiplicate per la stessa scala, come nelle altre schermate.
    private void applyScale(float scale) {
        title.setFontSize(TITLE_SIZE * scale);
        back.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        Dimension balance = new Dimension(back.getPreferredSize().width, 0);
        backBalance.changeShape(balance, balance, balance);
        titleLayout.setVgap(Math.round(SECTION_GAP * scale));

        int gap = Math.round(GAP * scale);
        java.awt.Font buttonFont = ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale);
        modeGroup.setButtonFont(buttonFont, gap);
        goalGroup.setButtonFont(buttonFont, gap);
        minus.setFont(buttonFont);
        plus.setFont(buttonFont);
        start.setFont(buttonFont);
        opponentsLayout.setHgap(gap);
        opponentsValue.setFont(ArcadeFont.of(VALUE_SIZE * scale));
        // largo abbastanza per "9", così i pulsanti non si spostano cambiando numero
        opponentsValue.setPreferredSize(new Dimension(Math.round(28 * scale), Math.round(VALUE_SIZE * scale + 4)));
        for (JLabel label : sectionLabels) {
            label.setFont(ArcadeFont.of(LABEL_SIZE * scale));
        }
        java.awt.Font small = ArcadeFont.of(DESCRIPTION_SIZE * scale);
        modeDescription.setFont(small);
        goalDescription.setFont(small);
        playersAtTable.setFont(small);
        summary.setFont(small);

        // Gli spazi fra le righe del riquadro: piccoli dentro una scelta, più grandi fra una scelta e l'altra.
        int section = Math.round(SECTION_GAP * scale);
        for (java.awt.Component component : box.getComponents()) {
            GridBagConstraints c = formLayout.getConstraints(component);
            int row = c.gridy % 3;
            if (c.gridy == 9) {
                c.insets = new Insets(section, 0, 0, 0);
            } else if (row == 0) {
                c.insets = new Insets(c.gridy == 0 ? 0 : section, 0, gap, 0);
            } else if (row == 1) {
                // le righe di pulsanti hanno lo spazio del FlowLayout anche prima del primo: lo si toglie,
                // così i pulsanti partono allineati al nome della scelta
                c.insets = new Insets(0, -gap, 0, 0);
            } else {
                c.insets = new Insets(gap, 0, 0, 0);
            }
            formLayout.setConstraints(component, c);
        }
        int padding = Math.round(BOX_PADDING * scale);
        box.setBorder(new EmptyBorder(padding, padding, padding, padding));
        box.setScale(scale);
        // con la scala cambiano i font, quindi anche la misura più grande del riquadro
        fixBoxSize();
        revalidate();
        repaint();
    }

    private void changeOpponents(int delta) {
        int value = opponents + delta;
        if (value < GameSettings.MIN_OPPONENTS || value > GameSettings.MAX_OPPONENTS) {
            return;
        }
        opponents = value;
        refresh();
    }

    // Aggiorna descrizioni, numero di avversari, pulsanti ai limiti e riepilogo dopo ogni scelta.
    private void refresh() {
        showTexts(modeGroup.getSelection(), goalGroup.getSelection(), opponents);
        minus.setEnabled(opponents > GameSettings.MIN_OPPONENTS);
        plus.setEnabled(opponents < GameSettings.MAX_OPPONENTS);
    }

    // I testi che dipendono dalle scelte: descrizioni, etichette dell'obiettivo, giocatori al tavolo, riepilogo.
    private void showTexts(GameMode mode, GameGoal goal, int opponentCount) {
        modeDescription.setText(TableTexts.upper(ChoiceLabels.MODE_DESCRIPTIONS.get(mode)));
        // I punti dell'obiettivo dipendono dalla modalità: le etichette si riscrivono a ogni scelta.
        goalGroup.setLabels(ChoiceLabels.goals(mode));
        goalDescription.setText(TableTexts.upper(ChoiceLabels.goalDescription(goal, mode)));

        opponentsValue.setText(String.valueOf(opponentCount));
        playersAtTable.setText(TableTexts.upper((opponentCount + 1) + " giocatori al tavolo"));

        String opponentsText;
        if (opponentCount == 1) {
            opponentsText = "1 avversario";
        } else {
            opponentsText = opponentCount + " avversari";
        }
        summary.setText(TableTexts.upper(ChoiceLabels.MODES.get(mode) + " / " + opponentsText + " / "
                + ChoiceLabels.goal(goal, mode)));
    }

    /**
     * Il riquadro ha sempre la misura che gli serve con i testi più lunghi, qualunque scelta ci sia: così non
     * cambia grandezza passando da Classica a No Mercy (dove descrizioni e punti sono più lunghi). Prova tutte le
     * combinazioni di modalità e obiettivo, con il massimo degli avversari, poi rimette i testi della scelta attuale.
     */
    private void fixBoxSize() {
        box.setPreferredSize(null);
        int width = 0;
        int height = 0;
        for (GameMode mode : GameMode.values()) {
            for (GameGoal goal : GameGoal.values()) {
                showTexts(mode, goal, GameSettings.MAX_OPPONENTS);
                Dimension size = box.getPreferredSize();
                width = Math.max(width, size.width);
                height = Math.max(height, size.height);
            }
        }
        refresh();
        box.setPreferredSize(new Dimension(width, height));
    }

    private GameSettings currentSettings() {
        return new GameSettings(modeGroup.getSelection(), opponents, goalGroup.getSelection());
    }
}

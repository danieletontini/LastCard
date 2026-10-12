package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import lastcard.controller.GameGoal;
import lastcard.controller.GameMode;

/**
 * Nuova partita: modalità, numero di avversari e obiettivo, poi Inizia.
 * Il pannello resta lo stesso per tutta la vita della finestra, quindi le scelte restano se si torna indietro e si rientra.
 * Per ora è solo vista: Inizia è disattivato finché non c'è il controller che crea la partita.
 */
public class NewGamePanel extends JPanel {

    private static final int MIN_OPPONENTS = 1;
    private static final int MAX_OPPONENTS = 9;

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

    public NewGamePanel(final Navigator navigator) {
        super(new BorderLayout());
        this.opponents = MIN_OPPONENTS;

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
        header.add(new JLabel("Nuova partita", SwingConstants.CENTER), BorderLayout.CENTER);

        // Le scelte, con i valori iniziali: Classica, 1 avversario, Mano singola.
        this.modeGroup = new ChoiceGroup<>(GameMode.values(), ChoiceLabels.MODES, GameMode.CLASSIC);
        this.goalGroup = new ChoiceGroup<>(GameGoal.values(), ChoiceLabels.GOALS, GameGoal.SINGLE_HAND);
        this.modeDescription = new JLabel();
        this.goalDescription = new JLabel();
        this.opponentsValue = new JLabel("", SwingConstants.CENTER);
        this.playersAtTable = new JLabel();
        this.minus = ButtonFactory.createButton("-");
        this.plus = ButtonFactory.createButton("+");
        this.summary = new JLabel();

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

        JPanel opponentsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        opponentsRow.add(minus);
        opponentsRow.add(opponentsValue);
        opponentsRow.add(plus);
        opponentsRow.add(playersAtTable);

        // Il modulo: etichetta a sinistra, controllo a destra, descrizione sotto il controllo.
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Modalità", modeGroup, modeDescription);
        addRow(form, 2, "Avversari", opponentsRow, null);
        addRow(form, 4, "Obiettivo", goalGroup, goalDescription);

        // In fondo: il riepilogo a sinistra, Inizia a destra.
        JButton start = ButtonFactory.createButton("Inizia");
        // Il controller che crea la partita non c'è ancora.
        start.setEnabled(false);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(summary, BorderLayout.WEST);
        bottom.add(start, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    // Una riga del modulo occupa due righe della griglia: il controllo e, sotto, la sua descrizione.
    private static void addRow(JPanel form, int gridRow, String label, JComponent control, JLabel description) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 10, 0, 10);

        c.gridx = 0;
        c.gridy = gridRow;
        form.add(new JLabel(label), c);

        c.gridx = 1;
        form.add(control, c);

        if (description != null) {
            c.gridy = gridRow + 1;
            c.insets = new Insets(2, 15, 0, 10);
            form.add(description, c);
        }
    }

    private void changeOpponents(int delta) {
        int value = opponents + delta;
        if (value < MIN_OPPONENTS || value > MAX_OPPONENTS) {
            return;
        }
        opponents = value;
        refresh();
    }

    // Aggiorna descrizioni, numero di avversari, pulsanti ai limiti e riepilogo dopo ogni scelta.
    private void refresh() {
        GameMode mode = modeGroup.getSelection();
        GameGoal goal = goalGroup.getSelection();

        modeDescription.setText(ChoiceLabels.MODE_DESCRIPTIONS.get(mode));
        goalDescription.setText(ChoiceLabels.GOAL_DESCRIPTIONS.get(goal));

        opponentsValue.setText(String.valueOf(opponents));
        playersAtTable.setText((opponents + 1) + " giocatori al tavolo");
        minus.setEnabled(opponents > MIN_OPPONENTS);
        plus.setEnabled(opponents < MAX_OPPONENTS);

        String opponentsText;
        if (opponents == 1) {
            opponentsText = "1 avversario";
        } else {
            opponentsText = opponents + " avversari";
        }
        summary.setText(ChoiceLabels.MODES.get(mode) + " / " + opponentsText + " / " + ChoiceLabels.GOALS.get(goal));
    }
}

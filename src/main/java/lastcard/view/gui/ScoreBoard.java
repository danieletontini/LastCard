package lastcard.view.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lastcard.model.PlayerSummary;

/**
 * La classifica della partita a punti, per il form di fine mano e di fine partita: un giocatore per riga, dal
 * punteggio più alto, con accanto a chi ha chiuso la mano i punti appena presi. Fino a cinque giocatori sta su
 * una colonna, oltre su due, così il form resta dentro la finestra anche con dieci giocatori.
 */
final class ScoreBoard {

    private static final float TEXT_SIZE = 11f;
    private static final int SINGLE_COLUMN_MAX = 5;

    private ScoreBoard() {
    }

    /**
     * @param viewer chi guarda il tavolo: compare come "Tu"
     * @param handWinner chi ha chiuso l'ultima mano, oppure null
     * @param handPoints i punti che ha preso chiudendola
     */
    static JComponent of(List<PlayerSummary> players, String viewer, String handWinner, int handPoints, float scale) {
        List<PlayerSummary> ranking = new ArrayList<>(players);
        // ordine stabile: a parità di punti resta l'ordine di gioco
        ranking.sort(Comparator.comparingInt(PlayerSummary::score).reversed());

        JPanel board = new JPanel(new GridBagLayout());
        board.setOpaque(false);
        int columns = ranking.size() > SINGLE_COLUMN_MAX ? 2 : 1;
        int rows = (ranking.size() + columns - 1) / columns;
        int gap = Math.round(14 * scale);
        int rowGap = Math.round(6 * scale);

        for (int i = 0; i < ranking.size(); i++) {
            PlayerSummary player = ranking.get(i);
            int column = i / rows;
            int row = i % rows;
            boolean closedHand = player.name().equals(handWinner);
            String name = player.name().equals(viewer) ? "TU" : player.name().toUpperCase();

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            // fra una colonna e l'altra uno spazio più largo
            int left = column > 0 ? gap * 3 : 0;

            c.gridx = column * 3;
            c.insets = new Insets(row == 0 ? 0 : rowGap, left, 0, gap);
            board.add(label((i + 1) + ". " + name, closedHand ? ArcadeButtonUI.CREAM : ArcadeButtonUI.TEXT_MUTED, scale), c);

            c.gridx = column * 3 + 1;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(row == 0 ? 0 : rowGap, 0, 0, gap / 2);
            board.add(label(player.score() + " PUNTI", ArcadeButtonUI.CREAM, scale), c);

            c.gridx = column * 3 + 2;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(row == 0 ? 0 : rowGap, 0, 0, 0);
            board.add(label(closedHand ? "+" + handPoints : "", ArcadeButtonUI.ALERT_HOVER, scale), c);
        }
        return board;
    }

    private static JLabel label(String text, java.awt.Color color, float scale) {
        JLabel label = new JLabel(text);
        label.setFont(ArcadeFont.of(TEXT_SIZE * scale));
        label.setForeground(color);
        return label;
    }
}

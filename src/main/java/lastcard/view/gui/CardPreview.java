package lastcard.view.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import lastcard.model.Card;
import lastcard.model.CardColor;
import lastcard.model.CardFace;
import lastcard.model.ColorDrawFour;
import lastcard.model.DeckFactory;
import lastcard.model.DiscardAll;
import lastcard.model.SkipEveryone;
import lastcard.model.WildColorRoulette;
import lastcard.model.WildDrawSix;
import lastcard.model.WildDrawTen;
import lastcard.model.WildReverseDrawFour;

/**
 * Scrive {@code anteprima-carte.png} con tutte le facce del mazzo classico, una
 * fila di carte giocabili (bordo crema) e il dorso, e una fila con le carte
 * No Mercy. Da rilanciare ogni volta che
 * si tocca una proporzione o un colore:
 *
 * <pre>mvn -q compile exec:java -Dexec.mainClass=lastcard.view.gui.CardPreview</pre>
 */
public final class CardPreview {

    private static final int WIDTH = 100;
    private static final int GAP = 10;
    private static final int COLUMNS = 14;

    private CardPreview() {
    }

    public static void main(String[] args) throws IOException {
        String path = args.length > 0 ? args[0] : "anteprima-carte.png";

        Set<CardFace> faces = new LinkedHashSet<>();
        for (Card card : DeckFactory.createClassic().getCards()) {
            faces.add(card.face());
        }
        List<CardFace> sorted = new ArrayList<>(faces);
        sorted.sort((a, b) -> a.color() != b.color()
                ? a.color().compareTo(b.color())
                : a.symbol() != b.symbol() ? a.symbol().compareTo(b.symbol()) : a.label().compareTo(b.label()));

        // ultima fila: un esempio per tipo, evidenziato come giocabile, poi il dorso
        List<CardFace> playable = List.of(
                sorted.get(7),                    // 7 rosso
                sorted.get(13 + 11),              // inverti giallo
                sorted.get(26 + 12),              // salta turno verde
                sorted.get(39 + 10),              // +2 blu
                sorted.get(sorted.size() - 2),    // jolly
                sorted.get(sorted.size() - 1));   // +4

        int height = CardRenderer.heightFor(WIDTH);
        // ultima fila: una carta No Mercy per tipo, finché DeckFactory non ha il mazzo da 168
        List<CardFace> noMercy = List.of(
                new ColorDrawFour(CardColor.RED).face(),
                new SkipEveryone(CardColor.GREEN).face(),
                new DiscardAll(CardColor.BLUE).face(),
                new WildDrawSix().face(),
                new WildDrawTen().face(),
                new WildReverseDrawFour().face(),
                new WildColorRoulette().face());

        int rows = (sorted.size() + COLUMNS - 1) / COLUMNS + 2;
        BufferedImage image = new BufferedImage(GAP + COLUMNS * (WIDTH + GAP), GAP + rows * (height + GAP),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(ArcadePalette.TABLE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            CardImageCache cache = new CardImageCache();
            for (int i = 0; i < sorted.size(); i++) {
                cache.draw(g, sorted.get(i), x(i), y(i / COLUMNS, height), WIDTH, false);
            }
            int playableRow = rows - 2;
            for (int i = 0; i < playable.size(); i++) {
                cache.draw(g, playable.get(i), x(i), y(playableRow, height), WIDTH, true);
            }
            cache.drawBack(g, x(playable.size()), y(playableRow, height), WIDTH);
            for (int i = 0; i < noMercy.size(); i++) {
                cache.draw(g, noMercy.get(i), x(i), y(rows - 1, height), WIDTH, false);
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(path));
        System.out.println("Scritto " + path + " (" + faces.size() + " facce diverse)");
    }

    private static int x(int index) {
        return GAP + (index % COLUMNS) * (WIDTH + GAP);
    }

    private static int y(int row, int height) {
        return GAP + row * (height + GAP);
    }
}

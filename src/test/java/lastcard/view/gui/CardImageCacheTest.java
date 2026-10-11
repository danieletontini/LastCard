package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import lastcard.model.Card;
import lastcard.model.CardColor;
import lastcard.model.CardFace;
import lastcard.model.ColorDrawFour;
import lastcard.model.DeckFactory;
import lastcard.model.DiscardAll;
import lastcard.model.FaceSymbol;
import lastcard.model.NumberCard;
import lastcard.model.SkipEveryone;
import lastcard.model.WildColorRoulette;
import lastcard.model.WildDrawSix;
import lastcard.model.WildDrawTen;
import lastcard.model.WildReverseDrawFour;

class CardImageCacheTest {

    private static BufferedImage canvas() {
        return new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void equalCardsShareOneImage() {
        CardImageCache cache = new CardImageCache();
        Graphics2D g = canvas().createGraphics();
        cache.draw(g, new NumberCard(CardColor.YELLOW, 8).face(), 0, 0, 130, false);
        cache.draw(g, new NumberCard(CardColor.YELLOW, 8).face(), 50, 0, 130, false);
        assertEquals(1, cache.size());
        g.dispose();
    }

    @Test
    void sizeAndHighlightAreSeparateEntries() {
        CardImageCache cache = new CardImageCache();
        Graphics2D g = canvas().createGraphics();
        CardFace face = CardFace.of(CardColor.BLUE, FaceSymbol.SKIP);
        cache.draw(g, face, 0, 0, 130, false);
        cache.draw(g, face, 0, 0, 130, true);
        cache.draw(g, face, 0, 0, 50, false);
        cache.drawBack(g, 0, 0, 130);
        assertEquals(4, cache.size());
        g.dispose();
    }

    @Test
    void wholeClassicDeckFitsIn54Images() {
        CardImageCache cache = new CardImageCache();
        Graphics2D g = canvas().createGraphics();
        for (Card card : DeckFactory.createClassic().getCards()) {
            cache.draw(g, card.face(), 0, 0, 130, false);
        }
        assertEquals(54, cache.size());
        g.dispose();
    }

    @Test
    void aFaceWithoutLabelAndWithoutCornerDrawingIsRejected() {
        // Prima negli angoli finiva il simbolo di Salta, senza nessun errore.
        CardRenderer renderer = new CardRenderer();
        CardFace unlabelled = new CardFace(CardColor.RED, FaceSymbol.NUMBER, "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> renderer.draw(canvas().createGraphics(), unlabelled, 0, 0, 130));
        assertTrue(e.getMessage().contains("NUMBER"));
    }

    @Test
    void everySymbolHasADrawing() {
        // Se si aggiunge un simbolo a FaceSymbol e ci si dimentica di disegnarlo, questo test lo dice.
        CardRenderer renderer = new CardRenderer();
        for (FaceSymbol symbol : FaceSymbol.values()) {
            CardFace face = new CardFace(CardColor.RED, symbol, "1");
            assertDoesNotThrow(() -> renderer.draw(canvas().createGraphics(), face, 0, 0, 130), symbol.name());
        }
    }

    @Test
    void everyNoMercyCardCanBeDrawnAtEverySize() {
        // Le carte vere: Salta tutti, Scarta tutto e Roulette non hanno etichetta e usano i disegni d'angolo.
        CardImageCache cache = new CardImageCache();
        Graphics2D g = canvas().createGraphics();
        List<Card> cards = List.of(new ColorDrawFour(CardColor.YELLOW), new SkipEveryone(CardColor.GREEN),
                new DiscardAll(CardColor.BLUE), new WildDrawSix(), new WildDrawTen(),
                new WildReverseDrawFour(), new WildColorRoulette());
        for (Card card : cards) {
            for (int width : new int[] { 45, 130, 180 }) {
                assertDoesNotThrow(() -> cache.draw(g, card.face(), 0, 0, width, false), card.toString());
            }
        }
        assertEquals(cards.size() * 3, cache.size());
        g.dispose();
    }

    @Test
    void playableBorderIsCream() {
        CardRenderer renderer = new CardRenderer();
        CardFace face = CardFace.of(CardColor.RED, FaceSymbol.REVERSE);
        BufferedImage normal = canvas();
        BufferedImage playable = canvas();
        renderer.draw(normal.createGraphics(), face, 0, 0, 130, false);
        renderer.draw(playable.createGraphics(), face, 0, 0, 130, true);

        // a metà del lato sinistro, sul bordo: crema solo se giocabile
        int y = CardRenderer.heightFor(130) / 2;
        assertEquals(ArcadePalette.INK.getRGB(), playable.getRGB(1, y));
        assertNotEquals(ArcadePalette.INK.getRGB(), normal.getRGB(1, y));
    }
}

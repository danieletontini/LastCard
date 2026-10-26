package lastcard.view.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import lastcard.model.Card;
import lastcard.model.CardColor;
import lastcard.model.CardFace;
import lastcard.model.DeckFactory;
import lastcard.model.FaceSymbol;

/**
 * Le carte si disegnano a codice dalla loro faccia (CardFace): ogni simbolo deve avere un disegno, e ogni carta
 * dei due mazzi si deve poter disegnare. La vista non guarda mai il tipo concreto della carta.
 */
class CardImageCacheTest {

    @Test
    void everyCardOfBothDecksHasAFaceAndADrawing() {
        CardRenderer renderer = new CardRenderer();
        BufferedImage canvas = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);

        // Se si aggiunge un simbolo a FaceSymbol e ci si dimentica di disegnarlo, questo test lo dice.
        for (FaceSymbol symbol : FaceSymbol.values()) {
            CardFace face = new CardFace(CardColor.RED, symbol, "1");
            assertDoesNotThrow(() -> renderer.draw(canvas.createGraphics(), face, 0, 0, 130), symbol.name());
        }

        Set<CardFace> classicFaces = new HashSet<>();
        for (Card card : DeckFactory.createClassic().getCards()) {
            classicFaces.add(card.face());
        }
        // 4 colori x (0-9 + salta + inverti + pesca due) + jolly + pesca quattro
        assertEquals(54, classicFaces.size());

        for (Card card : DeckFactory.createNoMercy().getCards()) {
            assertDoesNotThrow(() -> renderer.draw(canvas.createGraphics(), card.face(), 0, 0, 130), card.toString());
        }
    }
}

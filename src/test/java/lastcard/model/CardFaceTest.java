package lastcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CardFaceTest {

    @Test
    void numberCardShowsItsNumberAndColor() {
        assertEquals(new CardFace(CardColor.YELLOW, FaceSymbol.NUMBER, "8"),
                new NumberCard(CardColor.YELLOW, 8).face());
    }

    @Test
    void actionCardsKeepTheirColor() {
        assertEquals(new CardFace(CardColor.RED, FaceSymbol.DRAW_TWO, "+2"), new DrawTwo(CardColor.RED).face());
        assertEquals(CardFace.of(CardColor.BLUE, FaceSymbol.SKIP), new Skip(CardColor.BLUE).face());
        assertEquals(CardFace.of(CardColor.GREEN, FaceSymbol.REVERSE), new Reverse(CardColor.GREEN).face());
    }

    @Test
    void wildCardsAreBlack() {
        assertEquals(CardFace.of(CardColor.BLACK, FaceSymbol.WILD), new Wild().face());
        assertEquals(new CardFace(CardColor.BLACK, FaceSymbol.WILD_DRAW_FOUR, "+4"), new WildDrawFour().face());
    }

    @Test
    void nullLabelBecomesEmpty() {
        assertEquals("", new CardFace(CardColor.RED, FaceSymbol.SKIP, null).label());
    }

    @Test
    void classicDeckHas54DistinctFaces() {
        Set<CardFace> faces = new HashSet<>();
        for (Card card : DeckFactory.createClassic().getCards()) {
            assertNotNull(card.face());
            faces.add(card.face());
        }
        // 4 colori x (0-9 + salta + inverti + pesca due) + jolly + pesca quattro
        assertEquals(54, faces.size());
    }
}

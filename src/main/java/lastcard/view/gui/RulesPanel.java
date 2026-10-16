package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Le regole della modalità classica e di quella No Mercy, scelte con due pulsanti a selezione esclusiva.
 * Sfondo del cabinato come il menu; il testo sta in un riquadro scuro che scorre.
 *
 * I file delle regole sono testo semplice: le righe in maiuscolo sono titoli, quelle che iniziano con "- "
 * sono voci di elenco, una riga che comincia in minuscolo o con spazi continua quella prima. I titoli usano
 * il font a pixel; il resto un carattere normale, perché paragrafi interi a pixel si leggono a fatica.
 */
public class RulesPanel extends ArcadeBackgroundPanel {

    // I nomi delle due schede nel CardLayout del contenuto.
    private static final String CLASSIC = "CLASSIC";
    private static final String NO_MERCY = "NO_MERCY";

    // Le misure a grandezza normale, nella finestra da 960x640: a schermo intero crescono in proporzione.
    private static final float TITLE_SIZE = 22f;
    private static final float MODE_TITLE_SIZE = 14f;
    private static final float HEADING_SIZE = 11f;
    private static final float BODY_SIZE = 15f;
    private static final int TEXT_MAX_WIDTH = 760;
    private static final int BOX_PADDING = 22;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int GAP = 14;

    private static final Color BOX_FILL = new Color(0x15, 0x16, 0x1B, 235);
    private static final Color BODY_TEXT = new Color(0xE9E4D6);
    private static final String BODY_FONT = Font.SANS_SERIF;

    /** Una riga logica del testo: il titolo della modalità, un titolo di sezione, un paragrafo o una voce. */
    enum Kind { MODE_TITLE, HEADING, PARAGRAPH, ITEM }

    record Block(Kind kind, String text) { }

    private final JButton back;
    private final ArcadeTitle title;
    private final Box.Filler backBalance;
    private final JToggleButton classicButton;
    private final JToggleButton noMercyButton;
    private final FlowLayout tabsLayout;
    private final BorderLayout topLayout;
    private final CardLayout contentLayout;
    private final JPanel content;
    private final JPanel box;
    private final JPanel boxHolder;
    private final List<RulesText> texts;
    private float scale;

    /** Un testo delle regole: i blocchi letti dal file e il riquadro che li mostra. */
    private static final class RulesText {
        final List<Block> blocks;
        final JTextPane pane;
        final JScrollPane scroll;

        RulesText(List<Block> blocks) {
            this.blocks = blocks;
            this.pane = new JTextPane();
            pane.setEditable(false);
            // Non è un campo da modificare: niente cursore lampeggiante né selezione con la tastiera.
            pane.setFocusable(false);
            pane.setOpaque(false);
            pane.setBorder(null);
            this.scroll = new JScrollPane(pane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(null);
        }
    }

    public RulesPanel(final Navigator navigator) {
        super(new BorderLayout(), MENU_BACKGROUND);
        this.scale = 1f;

        // In alto: Indietro a sinistra, il titolo al centro della finestra.
        this.back = ButtonFactory.createButton("Indietro", ArcadeButtonUI.Variant.QUIET);
        back.addActionListener(e -> navigator.back());
        this.title = new ArcadeTitle(TITLE_SIZE, "REGOLE");
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(back, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        // Uno spazio vuoto largo quanto Indietro, a destra: così il titolo sta al centro della finestra.
        Dimension balance = new Dimension(back.getPreferredSize().width, 0);
        this.backBalance = new Box.Filler(balance, balance, balance);
        header.add(backBalance, BorderLayout.EAST);

        // Sotto il titolo: Classica / No Mercy, uno solo premuto alla volta.
        this.classicButton = ButtonFactory.createToggle("Classica");
        this.noMercyButton = ButtonFactory.createToggle("No Mercy");
        ButtonGroup group = new ButtonGroup();
        group.add(classicButton);
        group.add(noMercyButton);
        classicButton.setSelected(true);
        this.tabsLayout = new FlowLayout(FlowLayout.CENTER, GAP, 0);
        JPanel tabs = new JPanel(tabsLayout);
        tabs.setOpaque(false);
        tabs.add(classicButton);
        tabs.add(noMercyButton);

        this.topLayout = new BorderLayout(0, GAP);
        JPanel top = new JPanel(topLayout);
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(tabs, BorderLayout.CENTER);

        // Il contenuto: i due testi uno sopra l'altro, se ne vede uno alla volta.
        RulesText classic = new RulesText(load(RulesLoader.CLASSIC_PATH));
        RulesText noMercy = new RulesText(load(RulesLoader.NO_MERCY_PATH));
        this.texts = List.of(classic, noMercy);
        this.contentLayout = new CardLayout();
        this.content = new JPanel(contentLayout);
        content.setOpaque(false);
        content.add(classic.scroll, CLASSIC);
        content.add(noMercy.scroll, NO_MERCY);

        classicButton.addActionListener(e -> showRules(CLASSIC, classic));
        noMercyButton.addActionListener(e -> showRules(NO_MERCY, noMercy));

        // Il riquadro scuro, largo al massimo TEXT_MAX_WIDTH: righe troppo lunghe si leggono male.
        this.box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double arc = 12 * scale;
                    g.setColor(BOX_FILL);
                    g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
                    g.setStroke(new BasicStroke(2f * scale));
                    g.setColor(ArcadeButtonUI.EDGE);
                    g.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, arc, arc));
                } finally {
                    g.dispose();
                }
            }
        };
        box.setOpaque(false);
        box.add(content, BorderLayout.CENTER);
        this.boxHolder = new JPanel(new BorderLayout());
        boxHolder.setOpaque(false);
        boxHolder.add(box, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(boxHolder, BorderLayout.CENTER);

        // Allargando la finestra il riquadro resta al centro, largo al massimo TEXT_MAX_WIDTH.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateBoxWidth();
            }
        });
        ScreenScale.follow(this, this::applyScale);
        applyScale(1f);
    }

    // Mostra una scheda e riporta il suo testo in cima.
    private void showRules(String name, RulesText text) {
        contentLayout.show(content, name);
        text.scroll.getVerticalScrollBar().setValue(0);
    }

    // Tutte le misure moltiplicate per la stessa scala, come nelle altre schermate.
    private void applyScale(float newScale) {
        this.scale = newScale;
        title.setFontSize(TITLE_SIZE * scale);
        back.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        Dimension balance = new Dimension(back.getPreferredSize().width, 0);
        backBalance.changeShape(balance, balance, balance);
        classicButton.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        noMercyButton.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        int gap = Math.round(GAP * scale);
        tabsLayout.setHgap(gap);
        topLayout.setVgap(gap);

        int padding = Math.round(BOX_PADDING * scale);
        box.setBorder(new EmptyBorder(padding, padding, padding, Math.max(4, padding / 2)));
        for (RulesText text : texts) {
            JScrollBar bar = text.scroll.getVerticalScrollBar();
            bar.setUI(new ArcadeScrollBarUI(scale));
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(Math.round(SCROLLBAR_WIDTH * scale), 0));
            bar.setUnitIncrement(Math.round(18 * scale));
            fill(text.pane, text.blocks, scale);
        }
        updateBoxWidth();
        revalidate();
        repaint();
    }

    private void updateBoxWidth() {
        int maxWidth = Math.round(TEXT_MAX_WIDTH * scale);
        int side = Math.max(0, (boxHolder.getWidth() - maxWidth) / 2);
        boxHolder.setBorder(new EmptyBorder(Math.round(GAP * 1.5f * scale), side, 0, side));
        boxHolder.revalidate();
    }

    // ------------------------------------------------------------------
    // il testo
    // ------------------------------------------------------------------

    // Il file diviso in blocchi; se non si legge, al suo posto un messaggio d'errore.
    private static List<Block> load(String resourcePath) {
        try {
            return parse(RulesLoader.load(resourcePath));
        } catch (IOException e) {
            return List.of(new Block(Kind.PARAGRAPH, "Impossibile caricare le regole (" + resourcePath + ")."));
        }
    }

    /**
     * Divide il testo delle regole in blocchi. La prima riga è il titolo della modalità; le altre righe in
     * maiuscolo sono titoli; "- " apre una voce; una riga che comincia in minuscolo o con spazi continua il
     * blocco prima; le righe vuote separano.
     */
    static List<Block> parse(String text) {
        List<Block> blocks = new ArrayList<>();
        for (String raw : text.replace("\r", "").split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            boolean continues = !blocks.isEmpty()
                    && (Character.isWhitespace(raw.charAt(0)) || Character.isLowerCase(line.charAt(0)))
                    && blocks.get(blocks.size() - 1).kind() != Kind.HEADING
                    && blocks.get(blocks.size() - 1).kind() != Kind.MODE_TITLE;
            if (continues) {
                Block previous = blocks.remove(blocks.size() - 1);
                blocks.add(new Block(previous.kind(), previous.text() + " " + line));
            } else if (line.startsWith("- ")) {
                blocks.add(new Block(Kind.ITEM, line.substring(2).strip()));
            } else if (isHeading(line)) {
                blocks.add(new Block(blocks.isEmpty() ? Kind.MODE_TITLE : Kind.HEADING, line));
            } else {
                blocks.add(new Block(Kind.PARAGRAPH, line));
            }
        }
        return blocks;
    }

    // Un titolo è una riga tutta in maiuscolo, con almeno una lettera.
    private static boolean isHeading(String line) {
        return line.equals(line.toUpperCase()) && line.chars().anyMatch(Character::isLetter);
    }

    // Scrive i blocchi nel riquadro con gli stili alla scala data.
    private static void fill(JTextPane pane, List<Block> blocks, float scale) {
        String pixelFamily = ArcadeFont.of(1f).getFamily();
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                boolean last = i == blocks.size() - 1;
                SimpleAttributeSet text = new SimpleAttributeSet();
                SimpleAttributeSet paragraph = new SimpleAttributeSet();
                String content = block.text();
                switch (block.kind()) {
                    case MODE_TITLE -> {
                        style(text, pixelFamily, MODE_TITLE_SIZE * scale, ArcadeButtonUI.SHADOW);
                        StyleConstants.setSpaceBelow(paragraph, 6 * scale);
                        content = TableTexts.upper(content);
                    }
                    case HEADING -> {
                        style(text, pixelFamily, HEADING_SIZE * scale, ArcadeButtonUI.CREAM);
                        StyleConstants.setSpaceAbove(paragraph, 16 * scale);
                        StyleConstants.setSpaceBelow(paragraph, 6 * scale);
                        content = TableTexts.upper(content);
                    }
                    case PARAGRAPH -> {
                        style(text, BODY_FONT, BODY_SIZE * scale, BODY_TEXT);
                        StyleConstants.setSpaceBelow(paragraph, 4 * scale);
                        StyleConstants.setLineSpacing(paragraph, 0.15f);
                    }
                    case ITEM -> {
                        style(text, BODY_FONT, BODY_SIZE * scale, BODY_TEXT);
                        StyleConstants.setLeftIndent(paragraph, 20 * scale);
                        StyleConstants.setFirstLineIndent(paragraph, -14 * scale);
                        StyleConstants.setSpaceBelow(paragraph, 4 * scale);
                        StyleConstants.setLineSpacing(paragraph, 0.15f);
                    }
                }
                int start = doc.getLength();
                if (block.kind() == Kind.ITEM) {
                    SimpleAttributeSet bullet = new SimpleAttributeSet(text);
                    StyleConstants.setForeground(bullet, ArcadeButtonUI.SHADOW);
                    StyleConstants.setBold(bullet, true);
                    doc.insertString(doc.getLength(), "▸ ", bullet);
                }
                doc.insertString(doc.getLength(), content + (last ? "" : "\n"), text);
                doc.setParagraphAttributes(start, doc.getLength() - start, paragraph, false);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        pane.setCaretPosition(0);
    }

    private static void style(SimpleAttributeSet text, String family, float size, Color color) {
        StyleConstants.setFontFamily(text, family);
        StyleConstants.setFontSize(text, Math.round(size));
        StyleConstants.setForeground(text, color);
    }

    // ------------------------------------------------------------------
    // la barra di scorrimento
    // ------------------------------------------------------------------

    /** Una barra sottile: il cursore crema arrotondato su una guida appena visibile, senza frecce. */
    private static final class ArcadeScrollBarUI extends BasicScrollBarUI {

        private static final Color TRACK = new Color(0xFF, 0xFF, 0xFF, 18);
        private final float scale;

        ArcadeScrollBarUI(float scale) {
            this.scale = scale;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return noButton();
        }

        private static JButton noButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g0, JComponent c, Rectangle r) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(TRACK);
                g.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, r.width, r.width));
            } finally {
                g.dispose();
            }
        }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(isThumbRollover() || isDragging ? ArcadeButtonUI.CREAM_HOVER : ArcadeButtonUI.CREAM);
                double inset = Math.max(1, 1.5 * scale);
                g.fill(new RoundRectangle2D.Double(r.x + inset, r.y + inset, r.width - 2 * inset,
                        r.height - 2 * inset, r.width, r.width));
            } finally {
                g.dispose();
            }
        }
    }
}

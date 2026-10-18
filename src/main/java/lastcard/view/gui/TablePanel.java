package lastcard.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

import lastcard.controller.TableActions;
import lastcard.model.Card;
import lastcard.model.CardColor;
import lastcard.model.GameEvent;
import lastcard.model.GameObserver;
import lastcard.model.GameState;
import lastcard.model.PlayRules;
import lastcard.model.PlayerSummary;
import lastcard.model.VisibleState;
import lastcard.view.TableView;

/**
 * Il tavolo di gioco. Mostra solo ciò che arriva dal controller: lo stato visto dal giocatore umano e gli eventi
 * della partita. Non conosce Game: ogni gesto (una carta, il mazzo, Passa, Ultima!, una scelta nel form)
 * diventa una richiesta a TableActions.
 *
 * Disposizione: gli avversari su un arco attorno al tavolo, collegati dal percorso tratteggiato del giro con le
 * frecce del senso di gioco; al centro mazzo e scarti, con il colore attivo e la penalità in corso; in basso la
 * mano a ventaglio, con le carte giocabili bordate di crema. Tutto cresce con la finestra, come le altre schermate.
 */
public class TablePanel extends JPanel implements TableView {

    /** Il panno verde del tavolo, 3840x2160. */
    public static final String TABLE_BACKGROUND = "/images/tavolo_3840x2160.jpg";

    // Le misure a grandezza normale, nella finestra da 960x640.
    private static final int MARGIN = 18;
    private static final int CENTER_CARD = 76;
    private static final int CENTER_GAP = 34;
    private static final int HAND_CARD = 84;
    private static final int HOVER_LIFT = 22;
    private static final int BOX_WIDTH = 132;
    private static final int BOX_HEIGHT = 54;
    private static final int BOX_ICON = 22;
    private static final float NAME_SIZE = 11f;
    private static final float COUNT_SIZE = 8f;
    private static final float TAG_SIZE = 9f;
    private static final float SMALL_BUTTON_SIZE = 11f;
    private static final float LAST_CARD_SIZE = 17f;
    private static final int ERROR_MILLIS = 2500;
    // Le frecce del senso di gioco: tre per tratto, raccolte intorno alla metà.
    private static final double[] ARROW_POSITIONS = { 0.4, 0.5, 0.6 };
    private static final float ARROW_SIZE = 11f;
    // Il popup degli effetti: un secondo in tutto, con una breve comparsa e una dissolvenza alla fine.
    private static final int POPUP_MILLIS = 1000;
    private static final int POPUP_FADE_IN_MILLIS = 120;
    private static final int POPUP_FADE_OUT_MILLIS = 250;
    private static final int POPUP_FRAME_MILLIS = 16;
    private static final float POPUP_TITLE_SIZE = 26f;
    private static final float POPUP_LINE_SIZE = 11f;

    private static final Color FELT = ArcadePalette.TABLE;
    private static final Color BOX_FILL = new Color(0x0D1411);
    private static final Color BOX_EDGE = new Color(0x2C3A33);
    private static final Color PATH = new Color(0xF6, 0xE3, 0xAE, 90);
    private static final Color TURN = new Color(0x7CD992);
    private static final Color ERROR = new Color(0xFF9A9A);

    private final Navigator navigator;
    private final GameObserver sounds;
    private final CardImageCache cards;
    private final JButton options;
    private final JButton pass;
    private final JButton lastCard;
    private final ChoiceOverlay overlay;
    // Il form Opzioni sta sopra quello delle scelte: chiudendolo, la scelta in corso è ancora lì.
    private final ChoiceOverlay optionsOverlay;
    private final OptionsControls optionsControls;
    private final Timer errorTimer;
    private final Timer popupTimer;
    // Gli eventi della mossa in corso: quando arriva il nuovo stato diventano, se serve, un popup.
    private final List<GameEvent> moveEvents;
    private EffectPopup popup;
    private long popupStart;

    private String viewer;
    private TableActions actions;
    private VisibleState state;
    private boolean clockwise;
    private String eventText;
    private String errorText;

    // Calcolati a ogni disegno, servono ai clic: le carte della mano nell'ordine in cui sono disegnate, e il mazzo.
    private final List<HandCard> handCards;
    private Rectangle2D deckArea;
    private int hoverCard;
    private boolean hoverDeck;

    private transient BufferedImage background;
    private transient BufferedImage scaledBackground;
    private float lastScale;

    /** Una carta della mano come è stata disegnata: dove, ruotata come, e se si può giocare. */
    private record HandCard(int index, AffineTransform transform, Rectangle2D bounds, boolean playable) { }

    /**
     * @param sounds riceve anche lui gli eventi della partita, per i suoni; può essere null
     * @param optionsControls le opzioni da mostrare nel form Opzioni, le stesse della schermata Opzioni
     */
    public TablePanel(Navigator navigator, GameObserver sounds, OptionsControls optionsControls) {
        super(null);
        this.navigator = navigator;
        this.sounds = sounds;
        this.optionsControls = optionsControls;
        this.cards = new CardImageCache();
        this.handCards = new ArrayList<>();
        this.hoverCard = -1;
        this.clockwise = true;
        this.lastScale = 0f;
        setOpaque(true);

        // I form vanno aggiunti per primi: nel layout libero il primo componente sta sopra gli altri.
        this.optionsOverlay = new ChoiceOverlay();
        add(optionsOverlay);
        this.overlay = new ChoiceOverlay();
        add(overlay);

        this.options = ButtonFactory.createButton("Opzioni", ArcadeButtonUI.Variant.SECONDARY);
        options.addActionListener(e -> openOptions());
        add(options);

        this.pass = ButtonFactory.createButton("Passa", ArcadeButtonUI.Variant.SECONDARY);
        pass.addActionListener(e -> {
            if (actions != null) {
                actions.onPass();
            }
        });
        pass.setVisible(false);
        add(pass);

        this.lastCard = ButtonFactory.createButton("Ultima!", ArcadeButtonUI.Variant.ALERT);
        lastCard.addActionListener(e -> {
            if (actions != null) {
                actions.onLastCardCalled();
            }
        });
        lastCard.setEnabled(false);
        add(lastCard);

        this.errorTimer = new Timer(ERROR_MILLIS, e -> {
            errorText = null;
            repaint();
        });
        errorTimer.setRepeats(false);

        this.moveEvents = new ArrayList<>();
        // Ridisegna il popup a ogni fotogramma finché dura, poi lo toglie.
        this.popupTimer = new Timer(POPUP_FRAME_MILLIS, e -> {
            if (popupElapsed() >= POPUP_MILLIS) {
                hidePopup();
            }
            repaint();
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateHover(-1, -1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                click(e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ------------------------------------------------------------------
    // TableView: ciò che chiede il controller
    // ------------------------------------------------------------------

    @Override
    public void reset(String viewerName, TableActions tableActions) {
        this.viewer = viewerName;
        this.actions = tableActions;
        this.state = null;
        this.clockwise = true;
        this.eventText = null;
        this.errorText = null;
        this.hoverCard = -1;
        moveEvents.clear();
        hidePopup();
        overlay.close();
        optionsOverlay.close();
        repaint();
    }

    @Override
    public void showState(VisibleState newState) {
        this.state = newState;
        // Il controller manda lo stato dopo ogni mossa: gli eventi arrivati fin qui sono quelli della mossa.
        EffectPopup.of(moveEvents, viewer).ifPresent(this::showPopup);
        moveEvents.clear();
        // Le scelte si riaprono subito dopo, se il controller le chiede: con il nuovo stato il form vecchio non vale più.
        overlay.close();
        boolean myTurn = isMyTurn();
        pass.setVisible(myTurn && newState.state() == GameState.IN_PROGRESS && newState.canPass());
        lastCard.setEnabled(myTurn && newState.currentHand().size() <= 2
                && (newState.state() == GameState.IN_PROGRESS || newState.state() == GameState.AWAITING_COLOR));
        revalidate();
        repaint();
    }

    @Override
    public void askColor(boolean roulette) {
        overlay.showColors(roulette, scale(), color -> actions.onColorChosen(color));
        repaint();
    }

    @Override
    public void askSwapTarget(List<PlayerSummary> opponents) {
        overlay.showPlayers(opponents, scale(), name -> actions.onSwapTargetChosen(name));
        repaint();
    }

    @Override
    public void showError(String message) {
        errorText = message;
        errorTimer.restart();
        repaint();
    }

    @Override
    public void showWinner(String winnerName) {
        boolean me = winnerName.equals(viewer);
        overlay.showMessage(me ? "HAI VINTO!" : winnerName.toUpperCase() + " VINCE",
                me ? "Hai chiuso la mano" : "La partita è finita",
                List.of("Nuova partita", "Menu"),
                List.of(() -> leaveTo(Screen.NEW_GAME), () -> leaveTo(Screen.MAIN_MENU)),
                scale());
        repaint();
    }

    @Override
    public void showHandOver(List<PlayerSummary> players) {
        List<String> scores = new ArrayList<>();
        for (PlayerSummary player : players) {
            String name = player.name().equals(viewer) ? "Tu" : player.name();
            scores.add(name + " " + player.score());
        }
        Runnable nextHand = new Runnable() {
            @Override
            public void run() {
                actions.onNextHand();
            }
        };
        Runnable menu = new Runnable() {
            @Override
            public void run() {
                leaveTo(Screen.MAIN_MENU);
            }
        };
        overlay.showMessage("MANO FINITA", "Punti: " + String.join(" · ", scores),
                List.of("Prossima mano", "Menu"), List.of(nextHand, menu), scale());
        repaint();
    }

    @Override
    public void update(GameEvent event) {
        if (sounds != null) {
            sounds.update(event);
        }
        // Il senso di gioco non sta nello stato visibile: lo si segue dagli eventi, per le frecce del percorso.
        // Ogni mano, anche la successiva di una partita a punti, comincia in senso orario: la prima carta scoperta
        // arriva prima del suo eventuale cambio giro, quindi qui si riparte da capo.
        if (event.type() == GameEvent.Type.GAME_STARTED) {
            clockwise = true;
        } else if (event.type() == GameEvent.Type.DIRECTION_REVERSED) {
            clockwise = !clockwise;
        }
        String text = TableTexts.describe(event, viewer);
        if (text != null) {
            eventText = text;
        }
        moveEvents.add(event);
        repaint();
    }

    // Un popup nuovo prende il posto di quello che stava ancora finendo.
    private void showPopup(EffectPopup newPopup) {
        popup = newPopup;
        popupStart = System.nanoTime();
        popupTimer.restart();
    }

    private void hidePopup() {
        popup = null;
        popupTimer.stop();
    }

    private long popupElapsed() {
        return (System.nanoTime() - popupStart) / 1_000_000;
    }

    // ------------------------------------------------------------------
    // posizione dei componenti
    // ------------------------------------------------------------------

    @Override
    public void doLayout() {
        float s = scale();
        if (s != lastScale) {
            lastScale = s;
            options.setFont(ArcadeFont.of(SMALL_BUTTON_SIZE * s));
            pass.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * s));
            lastCard.setFont(ArcadeFont.of(LAST_CARD_SIZE * s));
            optionsControls.applyScale(s);
            // Il form Opzioni resta aperto anche quando si passa a schermo intero: lo si ricostruisce alla nuova scala.
            if (optionsOverlay.isOpen()) {
                showOptionsForm(s);
            }
        }
        int w = getWidth();
        int h = getHeight();
        int m = Math.round(MARGIN * s);
        optionsOverlay.setBounds(0, 0, w, h);
        overlay.setBounds(0, 0, w, h);

        Dimension optionsSize = options.getPreferredSize();
        options.setBounds(m, m, optionsSize.width, optionsSize.height);

        Dimension lastSize = lastCard.getPreferredSize();
        lastCard.setBounds(w - m - lastSize.width, h - m - lastSize.height, lastSize.width, lastSize.height);

        Dimension passSize = pass.getPreferredSize();
        pass.setBounds(w - m - passSize.width, h - m - lastSize.height - m - passSize.height,
                passSize.width, passSize.height);
    }

    private float scale() {
        return ScreenScale.of(this);
    }

    // ------------------------------------------------------------------
    // disegno
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            paintFelt(g);
            handCards.clear();
            deckArea = null;
            if (state == null) {
                return;
            }
            float s = scale();
            List<PlayerSummary> opponents = opponentsInTurnOrder();
            List<Point2D.Double> seats = SeatLayout.opponents(opponents.size(), getWidth(), getHeight(),
                    BOX_WIDTH * s, BOX_HEIGHT * s, MARGIN * s);
            paintPath(g, seats, s);
            for (int i = 0; i < opponents.size(); i++) {
                paintOpponent(g, opponents.get(i), seats.get(i), s);
            }
            paintCenter(g, s);
            paintHand(g, s);
            paintViewerBadge(g, s);
            paintPopup(g, s);
        } finally {
            g.dispose();
        }
    }

    // Il panno copre tutto senza deformarsi; scalarlo da 3840x2160 a ogni disegno sarebbe lento, quindi si
    // tiene una copia già della misura del tavolo, rifatta solo quando la finestra cambia misura.
    private void paintFelt(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        if (background == null) {
            background = ArcadeBackgroundPanel.image(TABLE_BACKGROUND);
        }
        if (background == null) {
            g.setColor(FELT);
            g.fillRect(0, 0, w, h);
            return;
        }
        if (scaledBackground == null || scaledBackground.getWidth() != w || scaledBackground.getHeight() != h) {
            scaledBackground = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = scaledBackground.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = Math.max(w / (double) background.getWidth(), h / (double) background.getHeight());
            int dw = (int) Math.ceil(background.getWidth() * scale);
            int dh = (int) Math.ceil(background.getHeight() * scale);
            bg.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            bg.dispose();
        }
        g.drawImage(scaledBackground, 0, 0, null);
    }

    // Il percorso del giro: una curva chiusa e morbida che passa dal giocatore umano, dagli avversari in ordine e
    // torna all'umano, tratteggiata, con tre frecce per tratto nel senso di gioco. Ogni tratto è una curva di Bézier
    // calcolata come spline di Catmull-Rom: passa per tutti i posti e cambia direzione senza spigoli.
    // In due giocatori non c'è: il senso di gioco non conta.
    // Il posto dell'umano è il cartellino del turno, sopra la sua mano: la curva arriva su un lato della scritta e
    // riparte dall'altro, e in mezzo si interrompe, così sembra passarle dietro.
    private void paintPath(Graphics2D g0, List<Point2D.Double> seats, float s) {
        if (seats.size() < 2) {
            return;
        }
        double tagY = turnTagY(s);
        List<Point2D.Double> points = new ArrayList<>();
        points.add(new Point2D.Double(getWidth() / 2.0, tagY));
        points.addAll(seats);

        Rectangle2D tag = tagBounds(g0, turnLabel(), getWidth() / 2.0, tagY, false, s);
        // appena un filo di margine: la linea deve toccare il cartellino, non fermarsi prima
        double margin = 1 * s;
        Rectangle2D hidden = new Rectangle2D.Double(tag.getX() - margin, tag.getY() - margin,
                tag.getWidth() + 2 * margin, tag.getHeight() + 2 * margin);
        Area visible = new Area(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        visible.subtract(new Area(hidden));
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.clip(visible);
            paintLoop(g, points, hidden, s);
        } finally {
            g.dispose();
        }
    }

    private void paintLoop(Graphics2D g, List<Point2D.Double> points, Rectangle2D hidden, float s) {
        float dash = 6 * s;
        g.setStroke(new BasicStroke(Math.max(1f, 1.5f * s), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
                new float[] { dash, dash }, 0f));
        g.setColor(PATH);

        int n = points.size();
        List<Point2D.Double[]> segments = new ArrayList<>();
        Path2D.Double loop = new Path2D.Double();
        loop.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 0; i < n; i++) {
            Point2D.Double before = points.get((i - 1 + n) % n);
            Point2D.Double from = points.get(i);
            Point2D.Double to = points.get((i + 1) % n);
            Point2D.Double after = points.get((i + 2) % n);
            Point2D.Double c1 = new Point2D.Double(from.x + (to.x - before.x) / 6, from.y + (to.y - before.y) / 6);
            Point2D.Double c2 = new Point2D.Double(to.x - (after.x - from.x) / 6, to.y - (after.y - from.y) / 6);
            loop.curveTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y);
            segments.add(new Point2D.Double[] { from, c1, c2, to });
        }
        loop.closePath();
        g.draw(loop);

        g.setStroke(new BasicStroke(Math.max(1.5f, 2.4f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Point2D.Double[] segment : segments) {
            for (double t : ARROW_POSITIONS) {
                paintArrow(g, segment, t, ARROW_SIZE * s, hidden);
            }
        }
    }

    // Una punta di freccia aperta sul tratto di curva, al punto t (da 0 a 1), che guarda nel senso di gioco.
    // Se cadrebbe dietro il cartellino del turno non si disegna: se ne vedrebbe solo un pezzo.
    private void paintArrow(Graphics2D g, Point2D.Double[] bezier, double t, double size, Rectangle2D hidden) {
        double u = 1 - t;
        Point2D.Double p0 = bezier[0];
        Point2D.Double p1 = bezier[1];
        Point2D.Double p2 = bezier[2];
        Point2D.Double p3 = bezier[3];
        double x = u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x;
        double y = u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y;
        if (hidden.intersects(x - size, y - size, 2 * size, 2 * size)) {
            return;
        }
        // la tangente della curva in t: la direzione in cui si sta andando
        double dx = 3 * u * u * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * t * t * (p3.x - p2.x);
        double dy = 3 * u * u * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * t * t * (p3.y - p2.y);
        double angle = Math.atan2(dy, dx) + (clockwise ? 0 : Math.PI);
        Path2D.Double head = new Path2D.Double();
        head.moveTo(x - size * Math.cos(angle - Math.PI / 5), y - size * Math.sin(angle - Math.PI / 5));
        head.lineTo(x, y);
        head.lineTo(x - size * Math.cos(angle + Math.PI / 5), y - size * Math.sin(angle + Math.PI / 5));
        g.draw(head);
    }

    // Un avversario: il dorso in piccolo, il nome e quante carte ha. Chi deve agire ha il bordo crema;
    // chi ha una carta sola il bordo rosso, perché si noti; gli eliminati sono spenti.
    private void paintOpponent(Graphics2D g, PlayerSummary player, Point2D.Double center, float s) {
        double w = BOX_WIDTH * s;
        double h = BOX_HEIGHT * s;
        double x = center.x - w / 2;
        double y = center.y - h / 2;
        boolean acting = player.name().equals(state.currentPlayerName());
        boolean lastCardLeft = !player.eliminated() && player.cardCount() == 1;
        Shape box = new RoundRectangle2D.Double(x, y, w, h, 8 * s, 8 * s);

        Graphics2D gb = (Graphics2D) g.create();
        try {
            if (player.eliminated()) {
                gb.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
            }
            gb.setColor(BOX_FILL);
            gb.fill(box);
            gb.setStroke(new BasicStroke((acting || lastCardLeft ? 2.5f : 1.5f) * s));
            gb.setColor(lastCardLeft ? ArcadeButtonUI.SHADOW : acting ? ArcadeButtonUI.CREAM : BOX_EDGE);
            gb.draw(box);

            int icon = Math.round(BOX_ICON * s);
            int iconHeight = CardRenderer.heightFor(icon);
            int ix = (int) Math.round(x + 10 * s);
            int iy = (int) Math.round(center.y - iconHeight / 2.0);
            cards.drawBack(gb, ix, iy, icon);

            double tx = ix + icon + 10 * s;
            gb.setFont(ArcadeFont.of(NAME_SIZE * s));
            gb.setColor(ArcadeButtonUI.CREAM);
            gb.drawString(player.name().toUpperCase(), (float) tx, (float) (center.y - 2 * s));

            gb.setFont(ArcadeFont.of(COUNT_SIZE * s));
            FontMetrics count = gb.getFontMetrics();
            String text = player.eliminated() ? "ELIMINATO" : TableTexts.cards(player.cardCount()).toUpperCase();
            gb.setColor(lastCardLeft ? ArcadeButtonUI.ALERT_HOVER : ArcadeButtonUI.TEXT_MUTED);
            gb.drawString(text, (float) tx, (float) (center.y + 4 * s + count.getAscent()));
        } finally {
            gb.dispose();
        }
    }

    // Mazzo e scarti al centro; sotto gli scarti il colore attivo, sotto il mazzo la penalità in corso, e più in
    // basso la frase dell'ultimo evento.
    private void paintCenter(Graphics2D g, float s) {
        int cardWidth = Math.round(CENTER_CARD * s);
        int cardHeight = CardRenderer.heightFor(cardWidth);
        double cx = getWidth() / 2.0;
        double cy = getHeight() * 0.40;
        int deckX = (int) Math.round(cx - CENTER_GAP * s / 2 - cardWidth);
        int discardX = (int) Math.round(cx + CENTER_GAP * s / 2);
        int y = (int) Math.round(cy - cardHeight / 2.0);

        // il mazzo, con un secondo dorso sotto per dare spessore
        int depth = Math.max(2, Math.round(4 * s));
        cards.drawBack(g, deckX + depth, y + depth, cardWidth);
        cards.drawBack(g, deckX, y, cardWidth);
        deckArea = new Rectangle2D.Double(deckX, y, cardWidth, cardHeight);
        // Quando pescare è l'unica mossa il mazzo ha il bordo crema, come le carte giocabili; col mouse sopra
        // il bordo c'è sempre che si possa pescare, e più spesso.
        boolean onlyDraw = onlyDrawLeft();
        if (canDraw() && (onlyDraw || hoverDeck)) {
            g.setStroke(new BasicStroke((hoverDeck ? 4f : 3f) * s));
            g.setColor(hoverDeck ? ArcadeButtonUI.CREAM_HOVER : ArcadeButtonUI.CREAM);
            g.draw(new RoundRectangle2D.Double(deckX - 2 * s, y - 2 * s, cardWidth + 4 * s, cardHeight + 4 * s,
                    12 * s, 12 * s));
        }

        if (state.top() != null) {
            cards.draw(g, state.top().face(), discardX, y, cardWidth, false);
        }

        double tagY = y + cardHeight + 22 * s;
        boolean choosing = state.state() == GameState.AWAITING_COLOR
                || state.state() == GameState.AWAITING_ROULETTE_COLOR;
        CardColor active = state.activeColor();
        if (choosing || active == null || active == CardColor.BLACK) {
            paintTag(g, "DA SCEGLIERE", discardX + cardWidth / 2.0, tagY, ArcadeButtonUI.CREAM, null, s);
        } else if (state.pendingDraw() > 0) {
            // Con la penalità in corso il colore non conta: vale per chi gioca dopo che qualcuno ha pescato.
            // Il cartellino è più lungo, quindi sta al centro, e il testo è spento.
            paintTag(g, TableTexts.upper(TableTexts.color(active) + " · Dopo la penalità"), cx, tagY,
                    ArcadeButtonUI.TEXT_MUTED, ArcadePalette.field(active), s);
        } else {
            paintTag(g, TableTexts.color(active).toUpperCase(), discardX + cardWidth / 2.0, tagY,
                    ArcadeButtonUI.CREAM, ArcadePalette.field(active), s);
        }
        // la penalità sta sopra mazzo e scarti: sotto c'è già il colore attivo
        if (state.pendingDraw() > 0) {
            paintTag(g, "+" + state.pendingDraw() + " DA PESCARE", cx, y - 22 * s, ArcadeButtonUI.ALERT_HOVER, null, s);
        }
        if (eventText != null) {
            g.setFont(ArcadeFont.of(TAG_SIZE * s));
            FontMetrics fm = g.getFontMetrics();
            String text = TableTexts.upper(eventText);
            g.setColor(ArcadeButtonUI.CREAM);
            g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) (tagY + 34 * s));
        }
    }

    // Il popup dell'effetto, sopra mazzo e scarti: un riquadro scuro con il titolo grande e gli altri effetti
    // sotto. Compare ingrandendosi un poco e alla fine sfuma; non è un componente, quindi i clic passano.
    private void paintPopup(Graphics2D g, float s) {
        if (popup == null) {
            return;
        }
        long elapsed = popupElapsed();
        float alpha;
        double grow;
        if (elapsed < POPUP_FADE_IN_MILLIS) {
            float t = elapsed / (float) POPUP_FADE_IN_MILLIS;
            alpha = t;
            grow = 0.85 + 0.15 * t;
        } else if (elapsed > POPUP_MILLIS - POPUP_FADE_OUT_MILLIS) {
            alpha = Math.max(0f, (POPUP_MILLIS - elapsed) / (float) POPUP_FADE_OUT_MILLIS);
            grow = 1;
        } else {
            alpha = 1f;
            grow = 1;
        }
        if (alpha <= 0f) {
            return;
        }

        Graphics2D gp = (Graphics2D) g.create();
        try {
            double cx = getWidth() / 2.0;
            double cy = getHeight() * 0.40;
            gp.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
            gp.translate(cx, cy);
            gp.scale(grow, grow);

            java.awt.Font titleFont = ArcadeFont.of(POPUP_TITLE_SIZE * s);
            java.awt.Font lineFont = ArcadeFont.of(POPUP_LINE_SIZE * s);
            FontMetrics tm = gp.getFontMetrics(titleFont);
            FontMetrics lm = gp.getFontMetrics(lineFont);
            double lineGap = 10 * s;
            double width = tm.stringWidth(popup.title());
            double height = tm.getAscent();
            for (String line : popup.lines()) {
                width = Math.max(width, lm.stringWidth(line));
                height += lineGap + lm.getAscent();
            }
            double padX = 30 * s;
            double padY = 22 * s;
            double boxW = width + 2 * padX;
            double boxH = height + 2 * padY;
            Shape box = new RoundRectangle2D.Double(-boxW / 2, -boxH / 2, boxW, boxH, 10 * s, 10 * s);
            gp.setColor(BOX_FILL);
            gp.fill(box);
            gp.setStroke(new BasicStroke(2.5f * s));
            gp.setColor(ArcadeButtonUI.CREAM);
            gp.draw(box);

            // il titolo con l'ombra rossa, come i titoli delle schermate
            double y = -boxH / 2 + padY + tm.getAscent();
            float titleX = (float) (-tm.stringWidth(popup.title()) / 2.0);
            float shadow = Math.max(1f, 2.5f * s);
            gp.setFont(titleFont);
            gp.setColor(ArcadeButtonUI.SHADOW);
            gp.drawString(popup.title(), titleX + shadow, (float) y + shadow);
            gp.setColor(ArcadeButtonUI.CREAM);
            gp.drawString(popup.title(), titleX, (float) y);

            gp.setFont(lineFont);
            gp.setColor(ArcadeButtonUI.ALERT_HOVER);
            for (String line : popup.lines()) {
                y += lineGap + lm.getAscent();
                gp.drawString(line, (float) (-lm.stringWidth(line) / 2.0), (float) y);
            }
        } finally {
            gp.dispose();
        }
    }

    // La mano a ventaglio, in basso al centro: le carte si sovrappongono di più quante più sono, si aprono
    // leggermente a ventaglio e quella sotto il mouse, se giocabile, si alza.
    private void paintHand(Graphics2D g, float s) {
        List<Card> hand = state.currentHand();
        int n = hand.size();
        int cardWidth = Math.round(HAND_CARD * s);
        int cardHeight = CardRenderer.heightFor(cardWidth);
        double cx = getWidth() / 2.0;
        double maxSpan = getWidth() * 0.5;
        double spacing = n <= 1 ? 0 : Math.min(cardWidth * 0.62, (maxSpan - cardWidth) / (n - 1));
        double total = cardWidth + spacing * Math.max(0, n - 1);
        double baseY = getHeight() - cardHeight * 0.80;
        double maxAngle = Math.min(9, 1.5 * Math.max(0, n - 1));

        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0 : (i - (n - 1) / 2.0) / ((n - 1) / 2.0);
            double x = cx - total / 2 + i * spacing;
            boolean playable = isPlayable(i, hand.get(i));
            double lift = playable && i == hoverCard ? HOVER_LIFT * s : 0;
            double y = baseY + t * t * cardHeight * 0.07 - lift;

            AffineTransform transform = new AffineTransform();
            transform.translate(x + cardWidth / 2.0, y + cardHeight);
            transform.rotate(Math.toRadians(t * maxAngle));
            transform.translate(-cardWidth / 2.0, -cardHeight);

            Graphics2D gc = (Graphics2D) g.create();
            try {
                gc.transform(transform);
                cards.draw(gc, hand.get(i).face(), 0, 0, cardWidth, playable);
            } finally {
                gc.dispose();
            }
            handCards.add(new HandCard(i, transform, new Rectangle2D.Double(0, 0, cardWidth, cardHeight), playable));
        }

        // il cartellino del turno sopra la mano, e sopra ancora l'eventuale errore
        double tagY = turnTagY(s);
        Color color;
        if (state.state() == GameState.FINISHED || state.state() == GameState.HAND_OVER) {
            color = ArcadeButtonUI.CREAM;
        } else if (isActing()) {
            color = TURN;
        } else {
            color = ArcadeButtonUI.TEXT_MUTED;
        }
        paintTag(g, turnLabel(), cx, tagY, color, null, s);
        if (errorText != null) {
            g.setFont(ArcadeFont.of(TAG_SIZE * s));
            FontMetrics fm = g.getFontMetrics();
            String text = TableTexts.upper(errorText);
            g.setColor(ERROR);
            g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) (tagY - 26 * s));
        }
    }

    // Il cartellino del turno sopra la mano: a che altezza sta e che cosa dice.
    private double turnTagY(float s) {
        int cardHeight = CardRenderer.heightFor(Math.round(HAND_CARD * s));
        return getHeight() - cardHeight * 0.80 - 28 * s;
    }

    private String turnLabel() {
        if (state.state() == GameState.FINISHED) {
            return "PARTITA FINITA";
        }
        if (state.state() == GameState.HAND_OVER) {
            return "MANO FINITA";
        }
        if (isActing()) {
            return turnText();
        }
        return "TOCCA A " + state.currentPlayerName().toUpperCase();
    }

    private String turnText() {
        if (state.state() == GameState.AWAITING_COLOR || state.state() == GameState.AWAITING_ROULETTE_COLOR) {
            return "SCEGLI IL COLORE";
        }
        if (state.state() == GameState.AWAITING_SWAP_TARGET) {
            return "SCEGLI CON CHI SCAMBIARE";
        }
        if (state.pendingDraw() > 0) {
            return onlyDrawLeft() ? "NON PUOI RILANCIARE: PESCA " + state.pendingDraw()
                    : "RILANCIA O PESCA " + state.pendingDraw();
        }
        if (onlyDrawLeft()) {
            return "NESSUNA CARTA GIOCABILE: PESCA";
        }
        if (state.canPass()) {
            return "GIOCA LA CARTA PESCATA O PASSA";
        }
        return "TOCCA A TE";
    }

    // In basso a sinistra: chi sei e quante carte hai, come i riquadri degli avversari.
    private void paintViewerBadge(Graphics2D g, float s) {
        String text = (viewer == null ? "" : viewer.toUpperCase()) + " / "
                + TableTexts.cards(state.currentHand().size()).toUpperCase();
        g.setFont(ArcadeFont.of(TAG_SIZE * s));
        FontMetrics fm = g.getFontMetrics();
        double padX = 14 * s;
        double h = 34 * s;
        double w = fm.stringWidth(text) + 2 * padX;
        double x = MARGIN * s;
        double y = getHeight() - MARGIN * s - h;
        Shape box = new RoundRectangle2D.Double(x, y, w, h, 8 * s, 8 * s);
        g.setColor(BOX_FILL);
        g.fill(box);
        g.setStroke(new BasicStroke(1.5f * s));
        g.setColor(BOX_EDGE);
        g.draw(box);
        g.setColor(ArcadeButtonUI.CREAM);
        g.drawString(text, (float) (x + padX), (float) (y + (h - fm.getHeight()) / 2 + fm.getAscent()));
    }

    // Un cartellino centrato in (cx, cy): fondo scuro, bordo, testo; con swatch, un quadratino colorato prima.
    // Dove sta un cartellino senza disegnarlo: serve al percorso del giro per passargli dietro.
    private Rectangle2D tagBounds(Graphics2D g, String text, double cx, double cy, boolean swatch, float s) {
        FontMetrics fm = g.getFontMetrics(ArcadeFont.of(TAG_SIZE * s));
        double square = swatch ? fm.getAscent() : 0;
        double squareGap = swatch ? 8 * s : 0;
        double w = fm.stringWidth(text) + square + squareGap + 2 * (12 * s);
        double h = 26 * s;
        return new Rectangle2D.Double(cx - w / 2, cy - h / 2, w, h);
    }

    private void paintTag(Graphics2D g, String text, double cx, double cy, Color textColor, Color swatch, float s) {
        g.setFont(ArcadeFont.of(TAG_SIZE * s));
        FontMetrics fm = g.getFontMetrics();
        double square = swatch == null ? 0 : fm.getAscent();
        double squareGap = swatch == null ? 0 : 8 * s;
        double padX = 12 * s;
        Rectangle2D bounds = tagBounds(g, text, cx, cy, swatch != null, s);
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double x = bounds.getX();
        double y = bounds.getY();
        Shape box = new RoundRectangle2D.Double(x, y, w, h, 6 * s, 6 * s);
        g.setColor(BOX_FILL);
        g.fill(box);
        g.setStroke(new BasicStroke(1.5f * s));
        g.setColor(BOX_EDGE);
        g.draw(box);
        double tx = x + padX;
        float baseline = (float) (y + (h - fm.getHeight()) / 2 + fm.getAscent());
        if (swatch != null) {
            g.setColor(swatch);
            g.fill(new Rectangle2D.Double(tx, baseline - square, square, square));
            tx += square + squareGap;
        }
        g.setColor(textColor);
        g.drawString(text, (float) tx, baseline);
    }

    // ------------------------------------------------------------------
    // mouse
    // ------------------------------------------------------------------

    private void updateHover(int x, int y) {
        int card = x < 0 ? -1 : cardAt(x, y);
        boolean deck = x >= 0 && card < 0 && deckArea != null && deckArea.contains(x, y);
        if (card != hoverCard || deck != hoverDeck) {
            hoverCard = card;
            hoverDeck = deck;
            boolean clickable = (card >= 0 && handCards.stream().anyMatch(h -> h.index() == hoverCard && h.playable()))
                    || (deck && canDraw());
            setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    private void click(int x, int y) {
        if (actions == null || state == null || overlay.isOpen()) {
            return;
        }
        int card = cardAt(x, y);
        if (card >= 0) {
            actions.onCardSelected(card);
        } else if (deckArea != null && deckArea.contains(x, y) && canDraw()) {
            actions.onDraw();
        }
    }

    // La carta sotto il punto: si guardano dall'ultima disegnata, che sta sopra le altre.
    private int cardAt(int x, int y) {
        for (int i = handCards.size() - 1; i >= 0; i--) {
            HandCard card = handCards.get(i);
            try {
                Point2D local = card.transform().inverseTransform(new Point2D.Double(x, y), null);
                if (card.bounds().contains(local)) {
                    return card.index();
                }
            } catch (NoninvertibleTransformException e) {
                // una rotazione è sempre invertibile: non succede
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // stato
    // ------------------------------------------------------------------

    private boolean isActing() {
        return state != null && viewer != null && state.currentPlayerName().equals(viewer);
    }

    private boolean isMyTurn() {
        return isActing() && !overlay.isOpen();
    }

    // Tocca a me, posso pescare e nessuna carta della mano si può giocare: l'unica mossa è il mazzo.
    private boolean onlyDrawLeft() {
        if (!canDraw()) {
            return false;
        }
        List<Card> hand = state.currentHand();
        for (int i = 0; i < hand.size(); i++) {
            if (isPlayable(i, hand.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean canDraw() {
        return isMyTurn() && state.state() == GameState.IN_PROGRESS && !state.canPass();
    }

    // Una carta si può giocare con la stessa regola del modello; dopo una pescata solo la carta pescata, l'ultima.
    private boolean isPlayable(int index, Card card) {
        if (!isMyTurn() || state.state() != GameState.IN_PROGRESS || state.top() == null) {
            return false;
        }
        if (state.canPass()) {
            return index == state.currentHand().size() - 1;
        }
        return PlayRules.canPlay(card, state.top(), state.activeColor(), state.pendingDraw());
    }

    // Gli avversari nell'ordine di gioco, a partire da chi gioca dopo l'umano in senso orario.
    private List<PlayerSummary> opponentsInTurnOrder() {
        List<PlayerSummary> all = state.players();
        int me = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).name().equals(viewer)) {
                me = i;
            }
        }
        List<PlayerSummary> opponents = new ArrayList<>();
        for (int i = 1; i < all.size(); i++) {
            opponents.add(all.get((me + i) % all.size()));
        }
        return opponents;
    }

    // Il form Opzioni: la partita va in pausa finché resta aperto.
    private void openOptions() {
        if (actions != null) {
            actions.onPause();
        }
        showOptionsForm(scale());
    }

    // Le opzioni del menu principale, poi Riprendi (chiude il form e la partita riparte) e Abbandona partita.
    private void showOptionsForm(float s) {
        optionsControls.refresh();
        optionsOverlay.showContent("OPZIONI", optionsControls,
                List.of("Riprendi", "Abbandona partita"),
                List.of(this::resume, () -> leaveTo(Screen.MAIN_MENU)),
                s);
        repaint();
    }

    private void resume() {
        if (actions != null) {
            actions.onResume();
        }
        repaint();
    }

    // Si lascia il tavolo: la partita si ferma, poi si va alla schermata chiesta.
    private void leaveTo(Screen screen) {
        if (actions != null) {
            actions.onLeaveTable();
        }
        optionsOverlay.close();
        overlay.close();
        errorTimer.stop();
        hidePopup();
        navigator.show(screen);
    }
}

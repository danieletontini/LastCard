package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import lastcard.view.audio.AudioPlayer;

/**
 * Le opzioni dello schermo e dell'audio: schermo intero, muto e volume (OptionsControls, le stesse del form
 * Opzioni del tavolo). Lo sfondo è lo stesso del menu principale.
 */
public class OptionsPanel extends ArcadeBackgroundPanel {

    // Le misure a grandezza normale, nella finestra da 960x640: a schermo intero crescono in proporzione.
    private static final float TITLE_SIZE = 22f;
    /** Lo spazio fra il titolo e il riquadro sotto, come in Nuova partita. */
    private static final int TITLE_GAP = 22;
    /** Lo spazio fra il bordo del riquadro e le opzioni: più largo ai lati, come una scheda. */
    private static final int BOX_PADDING_X = 40;
    private static final int BOX_PADDING_Y = 30;

    private final ArcadeTitle title;
    private final JButton back;
    private final OptionsControls controls;
    private final ArcadeBox box;
    private final BorderLayout titleLayout;

    public OptionsPanel(final Navigator navigator, final Settings settings, final MainWindow window,
            final AudioPlayer audio) {
        super(new BorderLayout(), MENU_BACKGROUND);

        // In alto a sinistra: Indietro. Il titolo sta al centro, sopra le opzioni.
        this.back = ButtonFactory.createButton("Indietro", ArcadeButtonUI.Variant.QUIET);
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.back();
            }
        });
        // I pannelli interni sono trasparenti e i testi chiari, perché si veda lo sfondo scuro.
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(back, BorderLayout.WEST);
        this.title = new ArcadeTitle(TITLE_SIZE, "OPZIONI");

        this.controls = new OptionsControls(settings, window, audio);
        // Le opzioni nel riquadro scuro di Regole e Nuova partita.
        this.box = new ArcadeBox(new BorderLayout());
        box.add(controls, BorderLayout.CENTER);
        // Il titolo appena sopra il riquadro, staccato di TITLE_GAP: il blocco intero sta al centro della finestra.
        this.titleLayout = new BorderLayout(0, TITLE_GAP);
        JPanel titled = new JPanel(titleLayout);
        titled.setOpaque(false);
        titled.add(title, BorderLayout.NORTH);
        titled.add(box, BorderLayout.CENTER);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(titled);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Le opzioni si possono cambiare anche dal tavolo: ogni volta che la schermata compare, si riallinea.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                controls.refresh();
            }
        });
        // A schermo intero la schermata è molto più grande di 960x640: testi e controlli la seguono.
        ScreenScale.follow(this, this::applyScale);
        applyScale(1f);
    }

    // Tutte le misure moltiplicate per la stessa scala, come nel menu principale.
    private void applyScale(float scale) {
        title.setFontSize(TITLE_SIZE * scale);
        back.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        titleLayout.setVgap(Math.round(TITLE_GAP * scale));
        controls.applyScale(scale);
        int padX = Math.round(BOX_PADDING_X * scale);
        int padY = Math.round(BOX_PADDING_Y * scale);
        box.setBorder(new EmptyBorder(padY, padX, padY, padX));
        box.setScale(scale);
        revalidate();
        repaint();
    }
}

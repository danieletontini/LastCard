package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lastcard.view.audio.AudioPlayer;

/**
 * Le opzioni dello schermo e dell'audio: schermo intero, muto e volume. Con il muto attivo lo slider non si usa.
 * Lo sfondo è lo stesso del menu principale.
 */
public class OptionsPanel extends ArcadeBackgroundPanel {

    // Le misure a grandezza normale, nella finestra da 960x640: a schermo intero crescono in proporzione.
    private static final float TITLE_SIZE = 22f;
    private static final float VOLUME_LABEL_SIZE = 14f;
    /** Lo spazio fra lo slider e la scritta del volume sotto. */
    private static final int VOLUME_LABEL_GAP = 12;
    private static final int ROW_GAP = 14;
    /** Lo spazio fra il titolo e le opzioni sotto. */
    private static final int TITLE_GAP = 40;

    private final Settings settings;
    private final JSlider volume;
    private final JLabel volumeLabel;
    private final ArcadeTitle title;
    private final JButton back;
    private final JCheckBox fullScreen;
    private final JCheckBox mute;
    private final BorderLayout volumeLayout;
    private final GridLayout columnLayout;
    private final BorderLayout titleLayout;

    public OptionsPanel(final Navigator navigator, final Settings settings, final MainWindow window,
            final AudioPlayer audio) {
        super(new BorderLayout(), MENU_BACKGROUND);
        this.settings = settings;

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

        this.fullScreen = ButtonFactory.createCheckBox("Schermo intero");
        fullScreen.setSelected(settings.isFullScreen());
        fullScreen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setFullScreen(fullScreen.isSelected());
                window.setFullScreen(fullScreen.isSelected());
            }
        });

        this.mute = ButtonFactory.createCheckBox("Muto");
        mute.setSelected(settings.isMuted());

        this.volume = new JSlider(Settings.MIN_VOLUME, Settings.MAX_VOLUME, settings.getVolume());
        this.volumeLabel = new JLabel("", SwingConstants.CENTER);
        volume.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateVolumeLabel();
                // Il volume cambia subito mentre si trascina, così la musica fa sentire la differenza;
                // si salva quando il cursore viene lasciato, non a ogni pixel del trascinamento.
                audio.setVolume(volume.getValue());
                if (!volume.getValueIsAdjusting()) {
                    settings.setVolume(volume.getValue());
                }
            }
        });
        mute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setMuted(mute.isSelected());
                audio.setVolume(settings.effectiveVolume());
                updateVolumeEnabled();
            }
        });
        updateVolumeLabel();
        updateVolumeEnabled();

        // Le caselle hanno già lo stile arcade da ButtonFactory; lo slider e la sua etichetta lo prendono qui.
        volume.setUI(new ArcadeSliderUI(volume));
        volume.setOpaque(false);
        volumeLabel.setFont(ArcadeFont.of(VOLUME_LABEL_SIZE));
        volumeLabel.setForeground(ArcadeButtonUI.CREAM);

        // La scritta del volume sta sotto lo slider, staccata di VOLUME_LABEL_GAP.
        this.volumeLayout = new BorderLayout(0, VOLUME_LABEL_GAP);
        JPanel volumeBox = new JPanel(volumeLayout);
        volumeBox.setOpaque(false);
        volumeBox.add(volume, BorderLayout.CENTER);
        volumeBox.add(volumeLabel, BorderLayout.SOUTH);

        // La colonna al centro, come nel menu principale.
        this.columnLayout = new GridLayout(0, 1, 0, ROW_GAP);
        JPanel column = new JPanel(columnLayout);
        column.setOpaque(false);
        column.add(fullScreen);
        column.add(mute);
        column.add(volumeBox);
        // Il titolo appena sopra la colonna, staccato di TITLE_GAP: il blocco intero sta al centro della finestra.
        this.titleLayout = new BorderLayout(0, TITLE_GAP);
        JPanel titled = new JPanel(titleLayout);
        titled.setOpaque(false);
        titled.add(title, BorderLayout.NORTH);
        titled.add(column, BorderLayout.CENTER);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(titled);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // A schermo intero la schermata è molto più grande di 960x640: testi e controlli la seguono.
        ScreenScale.follow(this, this::applyScale);
    }

    // Tutte le misure moltiplicate per la stessa scala, come nel menu principale.
    private void applyScale(float scale) {
        title.setFontSize(TITLE_SIZE * scale);
        back.setFont(ArcadeFont.of(ArcadeButtonUI.FONT_SIZE * scale));
        titleLayout.setVgap(Math.round(TITLE_GAP * scale));

        fullScreen.setFont(ArcadeFont.of(ArcadeCheckBoxUI.FONT_SIZE * scale));
        mute.setFont(ArcadeFont.of(ArcadeCheckBoxUI.FONT_SIZE * scale));
        // Lo slider cambia scala con una nuova UI; il volume e chi lo ascolta stanno nel modello, e restano.
        volume.setUI(new ArcadeSliderUI(volume, scale));
        volume.setOpaque(false);
        volumeLabel.setFont(ArcadeFont.of(VOLUME_LABEL_SIZE * scale));
        volumeLayout.setVgap(Math.round(VOLUME_LABEL_GAP * scale));
        columnLayout.setVgap(Math.round(ROW_GAP * scale));
        revalidate();
        repaint();
    }

    private void updateVolumeLabel() {
        volumeLabel.setText("Volume: " + volume.getValue());
    }

    // Con il muto attivo lo slider è disattivato ma tiene il valore: togliendo il muto si riparte da lì.
    private void updateVolumeEnabled() {
        boolean enabled = !settings.isMuted();
        volume.setEnabled(enabled);
        // Un JLabel disattivato Swing lo disegna in rilievo grigio: qui lo si spegne solo cambiando colore.
        volumeLabel.setForeground(enabled ? ArcadeButtonUI.CREAM : ArcadeButtonUI.TEXT_DISABLED);
    }
}

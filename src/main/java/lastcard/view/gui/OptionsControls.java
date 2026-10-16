package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import lastcard.view.audio.AudioPlayer;

/**
 * Le opzioni dello schermo e dell'audio, in colonna: schermo intero, muto e volume. Con il muto attivo lo slider
 * non si usa. Le usano sia la schermata Opzioni sia il form Opzioni del tavolo: ognuna ha la sua copia, e prima
 * di mostrarla la riallinea alle impostazioni con refresh(), perché l'altra può averle cambiate.
 */
final class OptionsControls extends JPanel {

    // Le misure a grandezza normale, nella finestra da 960x640: a schermo intero crescono in proporzione.
    private static final float VOLUME_LABEL_SIZE = 14f;
    /** Lo spazio fra lo slider e la scritta del volume sotto. */
    private static final int VOLUME_LABEL_GAP = 12;
    private static final int ROW_GAP = 14;

    private final Settings settings;
    private final JCheckBox fullScreen;
    private final JCheckBox mute;
    private final JSlider volume;
    private final JLabel volumeLabel;
    private final BorderLayout volumeLayout;
    private final GridLayout columnLayout;
    // Vero mentre refresh() rimette i valori delle impostazioni: non sono gesti dell'utente da salvare.
    private boolean refreshing;

    OptionsControls(Settings settings, MainWindow window, AudioPlayer audio) {
        this.settings = settings;
        this.columnLayout = new GridLayout(0, 1, 0, ROW_GAP);
        setLayout(columnLayout);
        setOpaque(false);

        this.fullScreen = ButtonFactory.createCheckBox("Schermo intero");
        fullScreen.addActionListener(e -> {
            settings.setFullScreen(fullScreen.isSelected());
            window.setFullScreen(fullScreen.isSelected());
        });

        this.mute = ButtonFactory.createCheckBox("Muto");
        mute.addActionListener(e -> {
            settings.setMuted(mute.isSelected());
            audio.setVolume(settings.effectiveVolume());
            updateVolumeEnabled();
        });

        this.volume = new JSlider(Settings.MIN_VOLUME, Settings.MAX_VOLUME, settings.getVolume());
        this.volumeLabel = new JLabel("", SwingConstants.CENTER);
        volume.addChangeListener(e -> {
            updateVolumeLabel();
            if (refreshing) {
                return;
            }
            // Il volume cambia subito mentre si trascina, così la musica fa sentire la differenza;
            // si salva quando il cursore viene lasciato, non a ogni pixel del trascinamento.
            audio.setVolume(volume.getValue());
            if (!volume.getValueIsAdjusting()) {
                settings.setVolume(volume.getValue());
            }
        });

        // Le caselle hanno già lo stile arcade da ButtonFactory; lo slider e la sua etichetta lo prendono qui.
        volume.setUI(new ArcadeSliderUI(volume));
        volume.setOpaque(false);
        volumeLabel.setFont(ArcadeFont.of(VOLUME_LABEL_SIZE));

        // La scritta del volume sta sotto lo slider, staccata di VOLUME_LABEL_GAP.
        this.volumeLayout = new BorderLayout(0, VOLUME_LABEL_GAP);
        JPanel volumeBox = new JPanel(volumeLayout);
        volumeBox.setOpaque(false);
        volumeBox.add(volume, BorderLayout.CENTER);
        volumeBox.add(volumeLabel, BorderLayout.SOUTH);

        add(fullScreen);
        add(mute);
        add(volumeBox);
        refresh();
    }

    /** Rimette caselle e slider come dicono le impostazioni. */
    void refresh() {
        refreshing = true;
        try {
            fullScreen.setSelected(settings.isFullScreen());
            mute.setSelected(settings.isMuted());
            volume.setValue(settings.getVolume());
        } finally {
            refreshing = false;
        }
        updateVolumeLabel();
        updateVolumeEnabled();
    }

    /** Tutte le misure moltiplicate per la stessa scala, come nelle altre schermate. */
    void applyScale(float scale) {
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

package lastcard.view.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
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

    private static final float VOLUME_LABEL_SIZE = 10f;

    private final Settings settings;
    private final JSlider volume;
    private final JLabel volumeLabel;

    public OptionsPanel(final Navigator navigator, final Settings settings, final MainWindow window,
            final AudioPlayer audio) {
        super(new BorderLayout(), MENU_BACKGROUND);
        this.settings = settings;

        // In alto: Indietro e il titolo, come nella schermata delle regole.
        JButton back = ButtonFactory.createButton("Indietro", ArcadeButtonUI.Variant.QUIET);
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
        header.add(new ArcadeTitle(22f, "OPZIONI"), BorderLayout.CENTER);
        // Uno spazio vuoto largo quanto Indietro, a destra: così il titolo sta al centro della finestra.
        header.add(Box.createHorizontalStrut(back.getPreferredSize().width), BorderLayout.EAST);

        final JCheckBox fullScreen = ButtonFactory.createCheckBox("Schermo intero");
        fullScreen.setSelected(settings.isFullScreen());
        fullScreen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setFullScreen(fullScreen.isSelected());
                window.setFullScreen(fullScreen.isSelected());
            }
        });

        final JCheckBox mute = ButtonFactory.createCheckBox("Muto");
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

        JPanel volumeBox = new JPanel(new BorderLayout());
        volumeBox.setOpaque(false);
        volumeBox.add(volume, BorderLayout.CENTER);
        volumeBox.add(volumeLabel, BorderLayout.SOUTH);

        // La colonna al centro, come nel menu principale.
        JPanel column = new JPanel(new GridLayout(0, 1, 0, 10));
        column.setOpaque(false);
        column.add(fullScreen);
        column.add(mute);
        column.add(volumeBox);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(column);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
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

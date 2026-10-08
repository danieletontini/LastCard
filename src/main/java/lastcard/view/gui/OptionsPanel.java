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

/** Le opzioni dello schermo e dell'audio: schermo intero, muto e volume. Con il muto attivo lo slider non si usa. */
public class OptionsPanel extends JPanel {

    private final Settings settings;
    private final JSlider volume;
    private final JLabel volumeLabel;

    public OptionsPanel(final Navigator navigator, final Settings settings, final MainWindow window) {
        super(new BorderLayout());
        this.settings = settings;

        // In alto: Indietro e il titolo, come nella schermata delle regole.
        JButton back = ButtonFactory.createButton("Indietro");
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigator.back();
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.add(back, BorderLayout.WEST);
        header.add(new JLabel("Opzioni", SwingConstants.CENTER), BorderLayout.CENTER);

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
                // Si salva quando il cursore viene lasciato, non a ogni pixel del trascinamento.
                if (!volume.getValueIsAdjusting()) {
                    settings.setVolume(volume.getValue());
                }
            }
        });
        mute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setMuted(mute.isSelected());
                updateVolumeEnabled();
            }
        });
        updateVolumeLabel();
        updateVolumeEnabled();

        JPanel volumeBox = new JPanel(new BorderLayout());
        volumeBox.add(volume, BorderLayout.CENTER);
        volumeBox.add(volumeLabel, BorderLayout.SOUTH);

        // La colonna al centro, come nel menu principale.
        JPanel column = new JPanel(new GridLayout(0, 1, 0, 10));
        column.add(fullScreen);
        column.add(mute);
        column.add(volumeBox);
        JPanel center = new JPanel(new GridBagLayout());
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
        volumeLabel.setEnabled(enabled);
    }
}

package lastcard.view.gui;

import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

// L'unico punto in cui nascono i pulsanti della GUI: lo stile si applica in applyStyle, per tutti insieme.
public final class ButtonFactory {

    // Cosa succede a ogni pressione, oltre all'azione del pulsante: di solito il suono del click.
    private static Runnable clickSound = () -> { };

    private ButtonFactory() {
    }

    /** Il suono di tutti i pulsanti, anche di quelli già creati: lo legge a ogni pressione. */
    public static void setClickSound(Runnable sound) {
        clickSound = Objects.requireNonNull(sound, "sound");
    }

    /** Un pulsante normale: si preme e fa un'azione. */
    public static JButton createButton(String text) {
        JButton button = new JButton(text);
        applyStyle(button);
        return button;
    }

    /** Un pulsante a due stati, premuto o no: scelte esclusive e interruttori. */
    public static JToggleButton createToggle(String text) {
        JToggleButton toggle = new JToggleButton(text);
        applyStyle(toggle);
        return toggle;
    }

    /** Una casella da spuntare, con il testo a sinistra e la casella a destra: le opzioni sì/no. */
    public static JCheckBox createCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setHorizontalTextPosition(SwingConstants.LEADING);
        applyStyle(checkBox);
        return checkBox;
    }

    // Qui andrà lo stile arcade. Per ora i pulsanti restano quelli standard di Swing, con il suono del click.
    private static void applyStyle(AbstractButton button) {
        button.addActionListener(e -> clickSound.run());
    }
}

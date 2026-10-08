package lastcard.view.gui;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToggleButton;

// L'unico punto in cui nascono i pulsanti della GUI: lo stile si applica in applyStyle, per tutti insieme.
public final class ButtonFactory {

    private ButtonFactory() {
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

    // Qui andrà lo stile arcade. Per ora i pulsanti restano quelli standard di Swing.
    private static void applyStyle(AbstractButton button) {
    }
}

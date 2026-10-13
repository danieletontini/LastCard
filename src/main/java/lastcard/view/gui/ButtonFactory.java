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

    /** Un pulsante normale: si preme e fa un'azione. Ha l'aspetto SECONDARY, scuro con il bordo. */
    public static JButton createButton(String text) {
        JButton button = new JButton(text);
        applyStyle(button);
        return button;
    }

    /** Un pulsante normale con l'aspetto scelto: PRIMARY per l'azione principale della schermata. */
    public static JButton createButton(String text, ArcadeButtonUI.Variant variant) {
        JButton button = createButton(text);
        setVariant(button, variant);
        return button;
    }

    /** Cambia l'aspetto di un pulsante già creato. */
    public static void setVariant(AbstractButton button, ArcadeButtonUI.Variant variant) {
        Objects.requireNonNull(variant, "variant");
        button.putClientProperty(ArcadeButtonUI.VARIANT_KEY, variant);
        button.repaint();
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

    // Stile arcade e suono del click per tutti i pulsanti. Le caselle hanno un disegno loro: testo e riquadro.
    private static void applyStyle(AbstractButton button) {
        if (button instanceof JCheckBox) {
            button.setUI(ArcadeCheckBoxUI.instance());
        } else {
            button.setUI(ArcadeButtonUI.instance());
        }
        button.addActionListener(e -> clickSound.run());
    }
}

package lastcard.view.gui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;

/**
 * Decide se mostrare la freccetta del focus: solo a chi usa la tastiera. Col mouse il focus resta sul pulsante
 * premuto, ma la freccetta lì non serve e sembrerebbe un pulsante selezionato per sbaglio.
 *
 * Ascolta tutti gli eventi della GUI: un tasto premuto passa alla modalità tastiera, il mouse mosso o premuto
 * torna alla modalità mouse. Si parte in modalità mouse, quindi all'apertura la freccetta non c'è.
 * A ogni cambio ridisegna il componente col focus, l'unico che può mostrare la freccetta.
 */
final class FocusIndicator {

    private static final long EVENTS = AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK;

    private static boolean installed;
    private static boolean keyboard;

    private FocusIndicator() {
    }

    /** Vero se il pulsante ha il focus e lo si sta usando con la tastiera. */
    static boolean shows(AbstractButton button) {
        install();
        return keyboard && button.isEnabled() && button.isFocusOwner();
    }

    // Gli eventi arrivano sul thread di Swing, come i disegni: non servono sincronizzazioni.
    private static void install() {
        if (installed) {
            return;
        }
        installed = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            int id = event.getID();
            if (id == KeyEvent.KEY_PRESSED) {
                setKeyboard(true);
            } else if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_PRESSED
                    || id == MouseEvent.MOUSE_DRAGGED) {
                setKeyboard(false);
            }
        }, EVENTS);
    }

    private static void setKeyboard(boolean value) {
        if (keyboard == value) {
            return;
        }
        keyboard = value;
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (owner != null) {
            owner.repaint();
        }
    }
}

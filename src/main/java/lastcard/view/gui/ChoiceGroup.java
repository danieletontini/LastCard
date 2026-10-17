package lastcard.view.gui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * "Scegli uno fra N": un pulsante a due stati per ogni valore di un enum, uno solo premuto alla volta.
 * Le etichette arrivano dalla vista, così il controller e il modello non contengono testi.
 */
public class ChoiceGroup<E extends Enum<E>> extends JPanel {

    private final Map<E, JToggleButton> buttons;
    private final List<ActionListener> listeners;
    private E selection;

    public ChoiceGroup(E[] values, Map<E, String> labels, E initial) {
        super(new FlowLayout(FlowLayout.LEFT, 5, 0));
        // Trasparente: i pulsanti stanno sopra lo sfondo della schermata.
        setOpaque(false);
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(labels, "labels");
        Objects.requireNonNull(initial, "initial");
        this.buttons = new LinkedHashMap<>();
        this.listeners = new ArrayList<>();
        this.selection = initial;

        ButtonGroup group = new ButtonGroup();
        for (E value : values) {
            String label = labels.get(value);
            if (label == null) {
                throw new IllegalArgumentException("No label for " + value);
            }
            JToggleButton button = ButtonFactory.createToggle(label);
            final E choice = value;
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    choose(choice);
                }
            });
            group.add(button);
            add(button);
            buttons.put(value, button);
        }
        buttons.get(initial).setSelected(true);
    }

    public E getSelection() {
        return selection;
    }

    /** Cambia la scelta da codice, senza avvisare chi ascolta. */
    public void setSelection(E value) {
        Objects.requireNonNull(value, "value");
        buttons.get(value).setSelected(true);
        selection = value;
    }

    /** Il font dei pulsanti e lo spazio fra l'uno e l'altro: servono quando la schermata cambia scala. */
    public void setButtonFont(Font font, int gap) {
        Objects.requireNonNull(font, "font");
        for (JToggleButton button : buttons.values()) {
            button.setFont(font);
        }
        ((FlowLayout) getLayout()).setHgap(gap);
        revalidate();
    }

    /** Cambia le etichette dei pulsanti, per esempio i punti dell'obiettivo quando cambia la modalità. */
    public void setLabels(Map<E, String> labels) {
        Objects.requireNonNull(labels, "labels");
        for (Map.Entry<E, JToggleButton> entry : buttons.entrySet()) {
            String label = labels.get(entry.getKey());
            if (label == null) {
                throw new IllegalArgumentException("No label for " + entry.getKey());
            }
            entry.getValue().setText(label);
        }
    }

    /** Chi ascolta viene avvisato quando l'utente cambia scelta. */
    public void addActionListener(ActionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    // L'utente ha premuto un pulsante: se la scelta è davvero cambiata, avvisa chi ascolta.
    private void choose(E value) {
        if (value == selection) {
            return;
        }
        selection = value;
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, value.name());
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }
}

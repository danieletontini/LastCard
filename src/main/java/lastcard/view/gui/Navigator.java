package lastcard.view.gui;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.JPanel;

// Tiene le schermate una sopra l'altra (CardLayout) e ne mostra una alla volta, ricordando il percorso fatto.
public class Navigator {

    private final CardLayout layout;
    private final JPanel container;
    private final Set<Screen> registered;
    // Le schermate da cui si è arrivati: l'ultima della lista è quella a cui riporta back().
    private final List<Screen> history;
    private Screen current;

    public Navigator() {
        this.layout = new CardLayout();
        this.container = new JPanel(layout);
        this.registered = EnumSet.noneOf(Screen.class);
        this.history = new ArrayList<>();
        this.current = null;
    }

    /** Il pannello che contiene tutte le schermate: va messo nella finestra. */
    public JPanel getContainer() {
        return container;
    }

    public void register(Screen screen, JPanel panel) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(panel, "panel");
        if (registered.contains(screen)) {
            throw new IllegalArgumentException("Screen already registered: " + screen);
        }
        registered.add(screen);
        container.add(panel, screen.name());
    }

    /** Mostra una schermata e ricorda quella di partenza, per poterci tornare con back(). */
    public void show(Screen screen) {
        Objects.requireNonNull(screen, "screen");
        if (!registered.contains(screen)) {
            throw new IllegalArgumentException("Screen not registered: " + screen);
        }
        if (current != null && current != screen) {
            history.add(current);
        }
        current = screen;
        layout.show(container, screen.name());
    }

    /** Torna alla schermata precedente. Se non ce n'è una, non fa niente. */
    public void back() {
        if (history.isEmpty()) {
            return;
        }
        Screen previous = history.remove(history.size() - 1);
        current = previous;
        layout.show(container, previous.name());
    }
}

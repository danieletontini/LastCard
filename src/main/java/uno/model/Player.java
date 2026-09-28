package uno.model;

import java.util.Objects;

public class Player {
    private final String name;

    public Player(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }
}

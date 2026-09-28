package uno.model;

public abstract class Carta {

    private final Colore colore;

    public Carta(Colore colore) {
        this.colore = colore;
    }

    public Colore getColore() {
        return colore;
    }

    public abstract String simbolo();

    public abstract int valorePunti();


}
package uno.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class Mazzo {

    private final List<Carta> carte;
    private final Random random;

    public Mazzo(List<Carta> carte) {
        this.carte = new ArrayList<>(carte);
        this.random = new Random();
        
    }
    public List<Carta> getCarte() {
        return List.copyOf(carte);
    }

    public void mescola() {
        Collections.shuffle(carte, random);
    }

    public Carta pesca() {
        if (carte.isEmpty()) {
            throw new IllegalStateException("Il mazzo è vuoto. Non ci sono carte da pescare.");
        }
        return carte.remove(carte.size() - 1);
    }

    public boolean isVuoto() {
        return carte.isEmpty();
    }

    public int getNumeroCarte() {
        return carte.size();
    }  
    
    
    
}

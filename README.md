# Ultima Carta

Gioco di carte in Java ispirato alle regole ufficiali dell'UNO, contro avversari controllati dal computer.
Si gioca in una finestra in stile arcade oppure da terminale, sullo stesso modello di gioco (architettura MVC).

- **Modalità Classica**: mazzo da 108 carte e regole ufficiali.
- **Modalità No Mercy**: mazzo da 168 carte, penalità che si sommano, scambio con il 7, rotazione con lo 0,
  eliminazione a 25 carte e il Jolly Rimescola Mani.
- **Partita a punti**: una mano sola, oppure più mani fino a 100 o 500 punti (1000 o 2000 nel No Mercy).
- Da 1 a 9 avversari.

## Requisiti

- **Java 21** o successivo: il codice usa record, interfacce `sealed` e lo `switch` con i pattern.
- **Maven 3** per compilare, avviare e lanciare i test da riga di comando. In alternativa basta VS Code con
  l'Extension Pack for Java, che legge il `pom.xml` da solo.

## Avvio

Dalla cartella del progetto:

```bash
mvn compile exec:java                           # finestra
mvn compile exec:java -Dexec.args="--console"   # terminale
```

Oppure si crea il jar e lo si avvia:

```bash
mvn package
java -jar target/lastcard.jar            # finestra
java -jar target/lastcard.jar --console  # terminale
```

Da VS Code: aprire `src/main/java/lastcard/Main.java` e premere *Run*.

## Come si gioca

**Nella finestra.** Dal menu principale: *Gioca* porta a Nuova partita, dove si scelgono modalità, numero di
avversari e obiettivo. Al tavolo si clicca una carta per giocarla (quelle giocabili hanno il bordo crema) e il
mazzo per pescare; *Ultima!* si preme con due carte o meno in mano, prima di giocare la penultima. *Opzioni*, in
alto a sinistra, mette in pausa la partita: da lì si cambiano schermo intero e volume, o si abbandona la partita.
Le regole complete sono nella schermata *Regole* del menu.

**Nel terminale.** Il menu iniziale chiede modalità, numero di avversari e obiettivo. Poi, al proprio turno:

| Comando | Azione |
|:--|:--|
| un numero | gioca la carta in quella posizione della mano |
| `d` | pesca (con una penalità in corso, la si prende tutta) |
| `k` | tiene la carta appena pescata e passa il turno |
| `u` | dichiara Ultima! |
| `q` | abbandona la partita (chiede conferma) |

## Test

```bash
mvn test
```

I test JUnit 5 stanno in `src/test/java` e verificano soprattutto il modello: le regole classiche e No Mercy, la
partita a punti, le CPU, l'invariante sul numero di carte e partite intere simulate CPU contro CPU.

## Struttura

```text
src/main/java/lastcard/
├── model/        carte, mazzo, partita (Game), giocatori, regole (GameRules), mosse, eventi, CPU
├── controller/   GameController (terminale), GuiGameController e AppController (finestra)
├── view/
│   ├── console/  vista da terminale
│   ├── gui/      vista Swing: menu, tavolo, carte disegnate a codice
│   └── audio/    musica ed effetti sonori
└── Main.java     sceglie la vista e avvia
```

Il modello non conosce né la vista né il controller: notifica i cambiamenti con eventi (Observer) e mostra alle
viste solo uno stato in sola lettura, con le carte degli avversari ridotte al loro numero.

Per sviluppare le carte c'è `lastcard.view.gui.CardPreview`, che scrive `anteprima-carte.png` con tutte le facce:

```bash
mvn -q compile exec:java -Dexec.mainClass=lastcard.view.gui.CardPreview
```

## Crediti

- Font *Press Start 2P*, licenza SIL Open Font License: `src/main/resources/font/OFL-PressStart2P.txt`.
- Musica ed effetti sonori: `src/main/resources/audio/CREDITS.txt`.
- Immagini: `src/main/resources/images/CREDITS.txt`.

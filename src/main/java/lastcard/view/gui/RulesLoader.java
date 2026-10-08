package lastcard.view.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Legge i testi delle regole dai file in src/main/resources, che finiscono dentro il jar insieme alle classi.
public final class RulesLoader {

    public static final String CLASSIC_PATH = "/rules/classic.txt";
    public static final String NO_MERCY_PATH = "/rules/nomercy.txt";

    private RulesLoader() {
    }

    /** Il contenuto del file, letto in UTF-8 così le lettere accentate restano corrette. */
    public static String load(String resourcePath) throws IOException {
        InputStream in = RulesLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try {
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}

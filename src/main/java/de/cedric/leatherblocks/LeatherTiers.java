package de.cedric.leatherblocks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stufen, Leder-Werte und Zahlenformatierung.
 *
 * Bewusst ohne jede Minecraft-Abhaengigkeit, damit die Logik mit einem blanken
 * JDK testbar ist (siehe tools/CoreTest.java).
 */
public final class LeatherTiers {

    public static final String MODID = "leatherblocks";
    public static final String LEATHER = "minecraft:leather";

    /**
     * Trennzeichen fuer Chat-Ausgaben, die der Server erzeugt.
     *
     * Der Item-Tooltip laeuft auf dem Client und holt sein Trennzeichen aus der
     * Sprachdatei. Chatnachrichten baut aber der Server, und dort loest
     * Language.getInstance() immer auf en_us auf - der Client-Sprache kommt man
     * dabei nicht bei. Deshalb hier fest, passend zum Serverpublikum.
     */
    public static final String SERVER_SEPARATOR = ".";

    /** Reihenfolge = Kompressionsstufe. Index 0 wird aus 9x Vanilla-Leder gebaut. */
    public static final String[] TIER_NAMES = {
            "leather_block",
            "compressed_leather_block",
            "double_compressed_leather_block",
            "triple_compressed_leather_block",
            "quadruple_compressed_leather_block",
            "quintuple_compressed_leather_block",
            "sextuple_compressed_leather_block",
            "septuple_compressed_leather_block",
            "octuple_compressed_leather_block",
            "nonuple_compressed_leather_block"
    };

    /** Item-Id -> Leder-Gegenwert. Enthaelt auch Vanilla-Leder mit Wert 1. */
    private static final Map<String, Long> VALUES;

    static {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put(LEATHER, 1L);
        for (int tier = 0; tier < TIER_NAMES.length; tier++) {
            map.put(MODID + ":" + TIER_NAMES[tier], leatherIn(tier));
        }
        VALUES = Collections.unmodifiableMap(map);
    }

    /**
     * Wie viel Vanilla-Leder in einem Block dieser Stufe steckt: 9^(tier+1).
     * Stufe 9 sind 3.486.784.401 - passt nicht mehr in ein int, daher long.
     */
    public static long leatherIn(int tier) {
        long total = 1L;
        for (int i = 0; i <= tier; i++) {
            total *= 9L;
        }
        return total;
    }

    public static boolean accepts(String itemId) {
        return VALUES.containsKey(itemId);
    }

    /** Leder-Gegenwert eines Items, 0 wenn das Depot es nicht annimmt. */
    public static long valueOf(String itemId) {
        return VALUES.getOrDefault(itemId, 0L);
    }

    /**
     * Addition, die bei Long.MAX_VALUE stehen bleibt statt ins Negative zu kippen.
     * Praktisch unerreichbar (2,6 Mrd. Bloecke der Top-Stufe), aber ein
     * ueberlaufender Highscore waere ein besonders duemmer Bug.
     */
    public static long addSaturating(long a, long b) {
        long sum = a + b;
        if (((a ^ sum) & (b ^ sum)) < 0) {
            return a > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return sum;
    }

    /** Tausendergruppierung mit sprachabhaengigem Trennzeichen. */
    public static String group(long value, String separator) {
        String digits = Long.toString(value);
        StringBuilder out = new StringBuilder(digits.length() + digits.length() / 3);
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append(separator);
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    private LeatherTiers() {
    }
}

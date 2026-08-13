package de.cedric.leatherblocks;

import java.util.List;
import java.util.UUID;

public class CoreTest {
    static int fails = 0;

    static void eq(String label, Object actual, Object expected) {
        boolean ok = String.valueOf(actual).equals(String.valueOf(expected));
        if (!ok) fails++;
        System.out.printf("%-52s %-22s %s%n", label, actual, ok ? "ok" : "ERWARTET: " + expected);
    }

    public static void main(String[] args) {
        System.out.println("--- Werte / Formatierung ---");
        eq("valueOf(minecraft:leather)", LeatherTiers.valueOf("minecraft:leather"), 1L);
        eq("valueOf(leather_block)", LeatherTiers.valueOf("leatherblocks:leather_block"), 9L);
        eq("valueOf(nonuple)", LeatherTiers.valueOf("leatherblocks:nonuple_compressed_leather_block"), 3486784401L);
        eq("valueOf(minecraft:dirt) -> abgelehnt", LeatherTiers.valueOf("minecraft:dirt"), 0L);
        eq("accepts(minecraft:dirt)", LeatherTiers.accepts("minecraft:dirt"), false);
        eq("accepts(minecraft:leather)", LeatherTiers.accepts("minecraft:leather"), true);
        eq("group(3486784401)", LeatherTiers.group(3486784401L, "."), "3.486.784.401");
        eq("group(1000000)", LeatherTiers.group(1000000L, "."), "1.000.000");
        eq("group(0)", LeatherTiers.group(0L, "."), "0");

        System.out.println("\n--- Ueberlaufschutz ---");
        eq("addSaturating(5,7)", LeatherTiers.addSaturating(5L, 7L), 12L);
        eq("addSaturating(MAX-1, 1)", LeatherTiers.addSaturating(Long.MAX_VALUE - 1, 1L), Long.MAX_VALUE);
        eq("addSaturating(MAX, 1) klemmt", LeatherTiers.addSaturating(Long.MAX_VALUE, 1L), Long.MAX_VALUE);
        eq("addSaturating(MAX, MAX) klemmt", LeatherTiers.addSaturating(Long.MAX_VALUE, Long.MAX_VALUE), Long.MAX_VALUE);

        System.out.println("\n--- Voller Stack Top-Stufe ---");
        long stack = 64L * LeatherTiers.valueOf("leatherblocks:nonuple_compressed_leather_block");
        eq("64x nonuple", stack, 223154201664L);
        eq("passt in long", stack > 0, true);

        System.out.println("\n--- Bestenliste ---");
        LeatherScores s = new LeatherScores();
        UUID a = UUID.nameUUIDFromBytes("cedric".getBytes());
        UUID b = UUID.nameUUIDFromBytes("kollege".getBytes());
        UUID c = UUID.nameUUIDFromBytes("dritter".getBytes());
        eq("leere Liste", s.ranking().size(), 0);
        eq("rank unbekannt", s.rankOf(a), 0);
        eq("get unbekannt", s.get(a), 0L);

        s.add(a, "Cedric", 9L);
        s.add(a, "Cedric", 81L);
        eq("akkumuliert", s.get(a), 90L);
        s.add(b, "Kollege", 6561L);
        s.add(c, "Dritter", 90L);

        List<LeatherScores.Entry> top = s.top(10);
        eq("Platz 1", top.get(0).name(), "Kollege");
        eq("Gleichstand alphabetisch: Platz 2", top.get(1).name(), "Cedric");
        eq("Gleichstand alphabetisch: Platz 3", top.get(2).name(), "Dritter");
        eq("rankOf(b)", s.rankOf(b), 1);
        eq("rankOf(a)", s.rankOf(a), 2);
        eq("top(2) kuerzt", s.top(2).size(), 2);
        eq("top(99) ueberschreitet nicht", s.top(99).size(), 3);

        System.out.println("\n--- Sortierung ist deterministisch ---");
        String first = null;
        boolean stable = true;
        for (int i = 0; i < 200; i++) {
            LeatherScores t = new LeatherScores();
            t.add(a, "Cedric", 90L);
            t.add(c, "Dritter", 90L);
            t.add(b, "Kollege", 90L);
            String order = t.ranking().toString();
            if (first == null) first = order;
            else if (!first.equals(order)) stable = false;
        }
        eq("200 Durchlaeufe gleiche Reihenfolge", stable, true);

        System.out.println("\n--- Negatives / Nulleingaben ---");
        eq("add(0) aendert nichts", s.add(a, "Cedric", 0L), 90L);
        eq("add(-5) aendert nichts", s.add(a, "Cedric", -5L), 90L);

        System.out.println();
        System.out.println(fails == 0 ? "ALLE TESTS OK" : fails + " FEHLER");
        if (fails > 0) System.exit(1);
    }
}

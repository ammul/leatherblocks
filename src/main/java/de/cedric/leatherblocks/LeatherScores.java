package de.cedric.leatherblocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The actual leaderboard. Also free of Minecraft dependencies -
 * LeatherDepotData only bolts on the NBT serialization.
 */
public class LeatherScores {

    public record Entry(UUID id, String name, long total) {
    }

    private final Map<UUID, Long> totals = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    /** Credits leather to a player and returns the new total. */
    public long add(UUID id, String name, long amount) {
        if (amount <= 0L) {
            return get(id);
        }
        names.put(id, name);
        long updated = LeatherTiers.addSaturating(totals.getOrDefault(id, 0L), amount);
        totals.put(id, updated);
        return updated;
    }

    /** Only updates the name, e.g. when a player has renamed themselves. */
    public void touchName(UUID id, String name) {
        if (totals.containsKey(id)) {
            names.put(id, name);
        }
    }

    public long get(UUID id) {
        return totals.getOrDefault(id, 0L);
    }

    public String nameOf(UUID id) {
        return names.getOrDefault(id, id.toString().substring(0, 8));
    }

    public int size() {
        return totals.size();
    }

    /**
     * Sorted descending. Ties broken alphabetically, so the order stays
     * stable across restarts (HashMap iteration is not).
     */
    public List<Entry> ranking() {
        List<Entry> list = new ArrayList<>(totals.size());
        for (Map.Entry<UUID, Long> e : totals.entrySet()) {
            list.add(new Entry(e.getKey(), nameOf(e.getKey()), e.getValue()));
        }
        list.sort(Comparator.comparingLong(Entry::total).reversed()
                .thenComparing(Entry::name)
                .thenComparing(entry -> entry.id().toString()));
        return list;
    }

    public List<Entry> top(int limit) {
        List<Entry> all = ranking();
        return all.subList(0, Math.min(limit, all.size()));
    }

    /** 1-based rank, 0 if the player hasn't stored anything yet. */
    public int rankOf(UUID id) {
        if (!totals.containsKey(id)) {
            return 0;
        }
        List<Entry> all = ranking();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }

    public Map<UUID, Long> rawTotals() {
        return totals;
    }

    public Map<UUID, String> rawNames() {
        return names;
    }
}

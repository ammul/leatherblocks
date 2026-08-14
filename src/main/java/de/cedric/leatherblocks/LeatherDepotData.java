package de.cedric.leatherblocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide score plus the info on who already owns a vault.
 *
 * Deliberately attached to the Overworld, not to the respective level -
 * otherwise every dimension would have its own leaderboard.
 */
public class LeatherDepotData extends SavedData {

    private static final String FILE_NAME = "leatherblocks_depot";

    public record VaultLocation(String dimension, long packedPos) {
        public BlockPos pos() {
            return BlockPos.of(packedPos);
        }
    }

    public static final SavedData.Factory<LeatherDepotData> FACTORY =
            new SavedData.Factory<>(LeatherDepotData::new, LeatherDepotData::load, null);

    private final LeatherScores scores = new LeatherScores();
    private final Map<UUID, VaultLocation> vaults = new HashMap<>();

    public static LeatherDepotData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    public LeatherScores scores() {
        return scores;
    }

    public long addLeather(UUID owner, String ownerName, long amount) {
        long updated = scores.add(owner, ownerName, amount);
        setDirty();
        return updated;
    }

    public VaultLocation vaultOf(UUID owner) {
        return vaults.get(owner);
    }

    public void claimVault(UUID owner, String ownerName, ServerLevel level, BlockPos pos) {
        vaults.put(owner, new VaultLocation(level.dimension().location().toString(), pos.asLong()));
        scores.touchName(owner, ownerName);
        setDirty();
    }

    /** Only release if the broken vault is actually the registered one. */
    public void releaseVault(UUID owner, ServerLevel level, BlockPos pos) {
        VaultLocation known = vaults.get(owner);
        if (known != null
                && known.packedPos() == pos.asLong()
                && known.dimension().equals(level.dimension().location().toString())) {
            vaults.remove(owner);
            setDirty();
        }
    }

    // ---- Persistence --------------------------------------------------------

    private static LeatherDepotData load(CompoundTag tag, HolderLookup.Provider registries) {
        LeatherDepotData data = new LeatherDepotData();

        ListTag scoreList = tag.getList("Scores", Tag.TAG_COMPOUND);
        for (int i = 0; i < scoreList.size(); i++) {
            CompoundTag entry = scoreList.getCompound(i);
            data.scores.add(entry.getUUID("Id"), entry.getString("Name"), entry.getLong("Total"));
        }

        ListTag vaultList = tag.getList("Vaults", Tag.TAG_COMPOUND);
        for (int i = 0; i < vaultList.size(); i++) {
            CompoundTag entry = vaultList.getCompound(i);
            data.vaults.put(entry.getUUID("Id"),
                    new VaultLocation(entry.getString("Dimension"), entry.getLong("Pos")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag scoreList = new ListTag();
        for (Map.Entry<UUID, Long> e : scores.rawTotals().entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", e.getKey());
            entry.putString("Name", scores.nameOf(e.getKey()));
            entry.putLong("Total", e.getValue());
            scoreList.add(entry);
        }
        tag.put("Scores", scoreList);

        ListTag vaultList = new ListTag();
        for (Map.Entry<UUID, VaultLocation> e : vaults.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", e.getKey());
            entry.putString("Dimension", e.getValue().dimension());
            entry.putLong("Pos", e.getValue().packedPos());
            vaultList.add(entry);
        }
        tag.put("Vaults", vaultList);
        return tag;
    }
}

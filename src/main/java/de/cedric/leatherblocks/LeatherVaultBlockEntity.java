package de.cedric.leatherblocks;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class LeatherVaultBlockEntity extends BlockEntity {

    private UUID owner;
    private String ownerName = "";
    private final IItemHandler handler = new DepotHandler();

    public LeatherVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.LEATHER_VAULT_BE.get(), pos, state);
    }

    public IItemHandler getHandler() {
        return handler;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        setChanged();
    }

    public long storedLeather() {
        if (owner == null || !(level instanceof ServerLevel serverLevel)) {
            return 0L;
        }
        return LeatherDepotData.get(serverLevel).scores().get(owner);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
            ownerName = tag.getString("OwnerName");
        }
    }

    /**
     * Einbahnstrasse: nimmt Leder und Lederbloecke an, bucht den Gegenwert auf
     * den Besitzer und vernichtet die Items.
     *
     * Das Vernichten ist Absicht. Wuerde das Depot die Bloecke behalten und
     * wieder herausgeben, koennte man dieselben 64 Bloecke im Kreis pumpen und
     * den Score beliebig hochtreiben. So kostet jeder Punkt echtes Leder.
     */
    private class DepotHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || owner == null) {
                return stack;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!LeatherTiers.accepts(itemId)) {
                return stack;
            }
            if (!simulate && level instanceof ServerLevel serverLevel) {
                // max. 64 * 3.486.784.401 - laeuft in einem long nicht ueber
                long gain = LeatherTiers.valueOf(itemId) * stack.getCount();
                LeatherDepotData.get(serverLevel).addLeather(owner, ownerName, gain);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty()
                    && LeatherTiers.accepts(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
    }
}

package de.cedric.leatherblocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LeatherBlocks.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LeatherBlocks.MODID);

    public static final String[] TIER_NAMES = LeatherTiers.TIER_NAMES;

    public static final List<DeferredBlock<Block>> ALL_BLOCKS = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> ALL_BLOCK_ITEMS = new ArrayList<>();

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LeatherBlocks.MODID);

    /** The leather vault: one per player, fillable via hopper. */
    public static final DeferredBlock<LeatherVaultBlock> LEATHER_VAULT;
    public static final DeferredItem<? extends Item> LEATHER_VAULT_ITEM;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeatherVaultBlockEntity>> LEATHER_VAULT_BE;

    static {
        for (int tier = 0; tier < TIER_NAMES.length; tier++) {
            DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(TIER_NAMES[tier], properties(tier));
            ALL_BLOCKS.add(block);

            long leather = LeatherTiers.leatherIn(tier);
            ALL_BLOCK_ITEMS.add(ITEMS.registerItem(
                    TIER_NAMES[tier],
                    props -> new CompressedLeatherBlockItem(block.get(), props, leather),
                    new Item.Properties()));
        }

        LEATHER_VAULT = BLOCKS.registerBlock("leather_vault", LeatherVaultBlock::new,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BROWN)
                        .sound(SoundType.WOOL)
                        .strength(2.5F, 1200.0F)
                        .pushReaction(PushReaction.BLOCK));
        LEATHER_VAULT_ITEM = ITEMS.registerSimpleBlockItem(LEATHER_VAULT);
        ALL_BLOCK_ITEMS.add(LEATHER_VAULT_ITEM);
        LEATHER_VAULT_BE = BLOCK_ENTITIES.register("leather_vault",
                () -> BlockEntityType.Builder.of(LeatherVaultBlockEntity::new, LEATHER_VAULT.get()).build(null));
    }


    private static BlockBehaviour.Properties properties(int tier) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .sound(SoundType.WOOL)
                // harder per tier, but without a tool requirement
                .strength(0.8F + tier * 0.2F)
                .pushReaction(PushReaction.NORMAL);
        // Deliberately NOT .ignitedByLava(): otherwise 6561 leather burns up at once.
    }

    private ModRegistry() {
    }
}

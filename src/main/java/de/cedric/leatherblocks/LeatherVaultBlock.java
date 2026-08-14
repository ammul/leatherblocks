package de.cedric.leatherblocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LeatherVaultBlock extends BaseEntityBlock {

    public static final MapCodec<LeatherVaultBlock> CODEC = simpleCodec(LeatherVaultBlock::new);

    public LeatherVaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeatherVaultBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * One vault per player. The check happens only after placement: the
     * vault is broken again and returned as an item, instead of cancelling
     * the placement - that's the way that guarantees the player keeps the
     * item, no matter which mod placed it.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel) || !(placer instanceof Player player)) {
            return;
        }

        LeatherDepotData data = LeatherDepotData.get(serverLevel);
        LeatherDepotData.VaultLocation existing = data.vaultOf(player.getUUID());

        if (existing != null) {
            BlockPos other = existing.pos();
            player.displayClientMessage(Component.translatable("message.leatherblocks.vault_exists",
                    other.getX(), other.getY(), other.getZ(), existing.dimension())
                    .withStyle(ChatFormatting.RED), false);
            level.destroyBlock(pos, true);
            return;
        }

        if (level.getBlockEntity(pos) instanceof LeatherVaultBlockEntity vault) {
            vault.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
        data.claimVault(player.getUUID(), player.getGameProfile().getName(), serverLevel, pos);
        player.displayClientMessage(Component.translatable("message.leatherblocks.vault_placed")
                .withStyle(ChatFormatting.GREEN), false);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof LeatherVaultBlockEntity vault
                && vault.getOwner() != null) {
            // Score is preserved, only the spot is freed up again.
            LeatherDepotData.get(serverLevel).releaseVault(vault.getOwner(), serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LeatherVaultBlockEntity vault && vault.getOwner() != null) {
            player.displayClientMessage(Component.translatable("message.leatherblocks.vault_info",
                    vault.getOwnerName(),
                    LeatherTiers.group(vault.storedLeather(), LeatherTiers.SERVER_SEPARATOR)), false);
        }
        return InteractionResult.CONSUME;
    }
}

package de.cedric.leatherblocks;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * BlockItem that shows how much leather is contained in the block in its tooltip.
 * The value is purely informational - the block stores nothing.
 */
public class CompressedLeatherBlockItem extends BlockItem {

    private final long leatherCount;

    public CompressedLeatherBlockItem(Block block, Properties properties, long leatherCount) {
        super(block, properties);
        this.leatherCount = leatherCount;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Thousands separator comes from the language file: "." for de_de, "," for en_us.
        String separator = Component.translatable("tooltip.leatherblocks.group_separator").getString();
        tooltip.add(Component.translatable("tooltip.leatherblocks.worth", LeatherTiers.group(leatherCount, separator))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

}

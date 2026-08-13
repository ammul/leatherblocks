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
 * BlockItem, das im Tooltip anzeigt, wie viel Leder in dem Block steckt.
 * Der Wert ist rein informativ - der Block speichert nichts.
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
        // Tausendertrennzeichen kommt aus der Sprachdatei: "." fuer de_de, "," fuer en_us.
        String separator = Component.translatable("tooltip.leatherblocks.group_separator").getString();
        tooltip.add(Component.translatable("tooltip.leatherblocks.worth", LeatherTiers.group(leatherCount, separator))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

}

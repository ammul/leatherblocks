package de.cedric.leatherblocks;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(LeatherBlocks.MODID)
public class LeatherBlocks {

    public static final String MODID = "leatherblocks";

    public LeatherBlocks(IEventBus modBus, ModContainer container) {
        ModRegistry.BLOCKS.register(modBus);
        ModRegistry.ITEMS.register(modBus);
        ModRegistry.BLOCK_ENTITIES.register(modBus);
        modBus.addListener(LeatherBlocks::addToCreativeTab);
        modBus.addListener(LeatherBlocks::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(LeatherBlocks::registerCommands);
        NeoForge.EVENT_BUS.addListener(LeaderboardBroadcaster::onServerTick);
    }

    /** Without this, the hopper ignores the vault. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModRegistry.LEATHER_VAULT_BE.get(),
                (blockEntity, side) -> blockEntity.getHandler());
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            ModRegistry.ALL_BLOCK_ITEMS.forEach(event::accept);
        }
    }
}

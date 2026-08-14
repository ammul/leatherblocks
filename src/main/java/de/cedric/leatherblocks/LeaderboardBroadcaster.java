package de.cedric.leatherblocks;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Posts the leather leaderboard to every player's chat on a fixed interval,
 * so nobody has to type /leder top to see where they stand.
 */
public final class LeaderboardBroadcaster {

    private static final int INTERVAL_TICKS = 20 * 60 * 30; // 30 minutes

    private static int ticksUntilNext = INTERVAL_TICKS;

    public static void onServerTick(ServerTickEvent.Post event) {
        if (--ticksUntilNext > 0) {
            return;
        }
        ticksUntilNext = INTERVAL_TICKS;
        broadcast(event.getServer());
    }

    private static void broadcast(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        LeatherScores scores = LeatherDepotData.get(overworld).scores();
        List<Component> messages = ModCommands.topMessages(scores.top(ModCommands.TOP_LIMIT));
        for (Component message : messages) {
            server.getPlayerList().broadcastSystemMessage(message, false);
        }
    }

    private LeaderboardBroadcaster() {
    }
}

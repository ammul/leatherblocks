package de.cedric.leatherblocks;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ModCommands {

    private static final int TOP_LIMIT = 10;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("leder")
                .executes(ctx -> own(ctx.getSource()))
                .then(Commands.literal("top").executes(ctx -> top(ctx.getSource()))));
    }

    private static LeatherScores scores(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        return LeatherDepotData.get(level).scores();
    }

    private static int own(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LeatherScores scores = scores(source);
        long total = scores.get(player.getUUID());
        int rank = scores.rankOf(player.getUUID());

        if (total == 0L) {
            source.sendSuccess(() -> Component.translatable("message.leatherblocks.none")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.leatherblocks.own",
                LeatherTiers.group(total, LeatherTiers.SERVER_SEPARATOR), rank).withStyle(ChatFormatting.GOLD), false);
        // Brigadier's return value is an int - a cast would overflow at large
        // totals, so the rank is returned instead of the amount.
        return rank;
    }

    private static int top(CommandSourceStack source) {
        List<LeatherScores.Entry> ranking = scores(source).top(TOP_LIMIT);
        if (ranking.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.leatherblocks.empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.leatherblocks.top_header")
                .withStyle(ChatFormatting.GOLD), false);
        for (int i = 0; i < ranking.size(); i++) {
            LeatherScores.Entry entry = ranking.get(i);
            int place = i + 1;
            source.sendSuccess(() -> Component.translatable("message.leatherblocks.top_line",
                    place, entry.name(), LeatherTiers.group(entry.total(), LeatherTiers.SERVER_SEPARATOR)), false);
        }
        return ranking.size();
    }

    private ModCommands() {
    }
}

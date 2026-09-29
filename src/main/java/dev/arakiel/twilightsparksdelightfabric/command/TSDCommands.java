package dev.arakiel.twilightsparksdelightfabric.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The {@code /tsd250} helper command of the debug tooling. */
public final class TSDCommands {
    private TSDCommands() {
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tsd250")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, TSDItems.Experiment250.MAX_LEVEL))
                        .then(Commands.argument("activity", DoubleArgumentType.doubleArg(0.0D))
                                .executes(context -> give(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "level"),
                                        DoubleArgumentType.getDouble(context, "activity")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> give(EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "level"),
                                                DoubleArgumentType.getDouble(context, "activity")))))));
    }

    private static int give(ServerPlayer player, int level, double activity) {
        double capacity = TSDItems.Experiment250.getCapacity(level);
        if (!Double.isFinite(activity) || activity > capacity) {
            player.sendSystemMessage(Component.translatable(
                    "commands.twilightsparksdelightfabric.tsd250.activity_too_high", capacity, level));
            return 0;
        }
        ItemStack stack = TSDItems.Experiment250.createStack(TSDRegistry.Items.EXPERIMENT_250, level, activity);
        player.getInventory().placeItemBackInInventory(stack);
        player.sendSystemMessage(Component.translatable(
                "commands.twilightsparksdelightfabric.tsd250.success",
                player.getName(), level, activity, capacity));
        return 1;
    }
}

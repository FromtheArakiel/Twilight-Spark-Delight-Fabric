package dev.arakiel.twilightsparksdelightfabric.event;

import dev.arakiel.twilightsparksdelightfabric.command.TSDCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Wires every Fabric callback of the mod. */
public final class TSDEventHandlers {
    private TSDEventHandlers() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                TSDEvents.onUseBlock(player, level, hand, hit));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                TSDEvents.onUseEntity(player, level, hand, entity, hit));
        UseItemCallback.EVENT.register((player, level, hand) -> {
            TSDEvents.onUseItem(player, level, hand);
            return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
                TSDLootEvents.onBlockBreak(level, player, state.getBlock(), pos));
        ServerLivingEntityEvents.AFTER_DEATH.register(TSDLootEvents::onAfterDeath);
        // This API version has no after-damage callback, so the Experiment 250
        // proliferation roll runs when incoming damage is about to be applied.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0.0F) {
                TSDExperimentEvents.onAfterDamage(entity, amount);
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(TSDPlayerEvents::onEndServerTick);
        ServerTickEvents.END_WORLD_TICK.register(TSDEvents::onLevelTick);

        ServerPlayerEvents.COPY_FROM.register(TSDPlayerEvents::onCopyFrom);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                TSDPlayerEvents.onAfterRespawn(newPlayer, alive));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                TSDPlayerEvents.onDisconnect(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                TSDCommands.register(dispatcher));
        TSDLootEvents.register();
    }

}

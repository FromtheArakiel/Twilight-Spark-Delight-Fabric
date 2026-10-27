package dev.arakiel.twilightsparksdelight.event;

import dev.arakiel.gemini.event.EventResult;
import dev.arakiel.gemini.event.GeminiEvents;
import dev.arakiel.gemini.event.effect.MobEffectAddedEvent;
import dev.arakiel.gemini.event.effect.MobEffectApplicableEvent;
import dev.arakiel.gemini.event.entity.living.LivingAttackEvent;
import dev.arakiel.gemini.event.entity.living.LivingDamageEvent;
import dev.arakiel.gemini.event.entity.living.LivingDeathEvent;
import dev.arakiel.gemini.event.entity.living.LivingHurtEvent;
import dev.arakiel.gemini.event.item.ItemUseFinishEvent;
import dev.arakiel.gemini.event.level.BonemealEvent;
import dev.arakiel.gemini.event.level.EntityJoinLevelEvent;
import dev.arakiel.gemini.event.level.LevelTickEvent;
import dev.arakiel.gemini.event.server.PlayerLoggedOutEvent;
import dev.arakiel.gemini.event.server.ServerTickEvent;
import dev.arakiel.twilightsparksdelight.command.TSDCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public final class TSDEventHandlers {
    private TSDEventHandlers() {
    }

    public static void register() {
        GeminiEvents.init();
        GeminiEvents.listen(EntityJoinLevelEvent.class,
                event -> TSDEvents.onEntityLoad(event.getEntity(), event.getLevel()));
        GeminiEvents.listen(BonemealEvent.class, TSDEventHandlers::onBonemeal);
        GeminiEvents.listen(LivingAttackEvent.class, TSDEventHandlers::onLivingAttack);
        GeminiEvents.listen(LivingHurtEvent.class, TSDEventHandlers::onLivingHurt);
        GeminiEvents.listen(LivingDamageEvent.class, TSDEventHandlers::onLivingDamage);
        GeminiEvents.listen(LivingDeathEvent.class,
                event -> TSDLootEvents.onAfterDeath(event.getLivingEntity(), event.getSource()));
        GeminiEvents.listen(MobEffectApplicableEvent.class, TSDEventHandlers::onEffectApplicable);
        GeminiEvents.listen(MobEffectAddedEvent.class, TSDEventHandlers::onEffectAdded);
        GeminiEvents.listen(ItemUseFinishEvent.class, TSDEventHandlers::onItemFinished);
        GeminiEvents.listen(ServerTickEvent.class, TSDEventHandlers::onServerTick);
        GeminiEvents.listen(LevelTickEvent.class, TSDEventHandlers::onLevelTick);
        GeminiEvents.listen(PlayerLoggedOutEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                TSDPlayerEvents.onDisconnect(player);
            }
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                TSDEvents.onUseBlock(player, level, hand, hit));
        UseItemCallback.EVENT.register((player, level, hand) -> {
            TSDEvents.onUseItem(player, level, hand);
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
                TSDLootEvents.onBlockBreak(level, player, state.getBlock(), pos));
        ServerPlayerEvents.COPY_FROM.register(TSDPlayerEvents::onCopyFrom);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                TSDPlayerEvents.onAfterRespawn(newPlayer, alive));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                TSDCommands.register(dispatcher));
        TSDLootEvents.register();
    }

    private static void onBonemeal(BonemealEvent event) {
        if (!event.getLevel().isClientSide()
                && TSDMossEvents.trySpreadWithBonemeal(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (entity instanceof Mob mob && event.getSource().getEntity() instanceof Player player) {
            TSDEvents.onPacifiedMobAttacked(mob, player);
        }
        if (TSDCombatEvents.shouldCancelDamage(entity, event.getSource())) {
            event.setCanceled(true);
        }
    }

    private static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        event.setAmount(Math.max(0.0F,
                TSDCombatEvents.modifyIncomingDamage(entity, event.getSource(), event.getAmount())));
        if (entity instanceof ServerPlayer player
                && TSDExperimentEvents.preventFatalDamage(player, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (!entity.level().isClientSide()) {
            TSDExperimentEvents.onAfterDamage(entity, event.getAmount());
        }
    }

    private static void onEffectApplicable(MobEffectApplicableEvent event) {
        if (TSDCombatEvents.shouldBlockEffect(event.getLivingEntity(), event.getEffect())) {
            event.setResult(EventResult.DENY);
        }
    }

    private static void onEffectAdded(MobEffectAddedEvent event) {
        TSDCombatEvents.onEffectAdded(
                event.getLivingEntity(),
                event.getEffect(),
                event.getPreviousEffect());
    }

    private static void onItemFinished(ItemUseFinishEvent event) {
        LivingEntity user = event.getUser();
        TSDFoodEvents.onItemFinished(user, event.getStack());
        if (user instanceof ServerPlayer player) {
            TSDExperimentEvents.onItemFinished(player, event.getStack());
        }
    }

    private static void onServerTick(ServerTickEvent event) {
        if (event.getPhase() == ServerTickEvent.Phase.POST) {
            TSDPlayerEvents.onEndServerTick(event.getServer());
        }
    }

    private static void onLevelTick(LevelTickEvent event) {
        if (event.getPhase() == LevelTickEvent.Phase.POST) {
            TSDEvents.onLevelTick(event.getServerLevel());
        }
    }
}

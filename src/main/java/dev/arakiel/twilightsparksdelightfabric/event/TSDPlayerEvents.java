package dev.arakiel.twilightsparksdelightfabric.event;

import java.util.Map;
import java.util.WeakHashMap;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.common.network.TSDPayloads;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

/** Player progression, extended food bar and acquisition triggers. */
public final class TSDPlayerEvents {
    private static final Map<Player, TSDPayloads.FoodState> LAST_SENT = new WeakHashMap<>();
    private static final ResourceLocation SPRINT_ID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "extended_food_sprint");

    private TSDPlayerEvents() {
    }

    /** Runs once per server tick for every player. */
    public static void onEndServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TSDCombatEvents.onPlayerTick(player);
            TSDEvents.tickPocketWatch(player);
            TSDExperimentEvents.tick(player);
            updateExtendedFood(player);
            if (player.tickCount % 10 == 0) {
                scanInventory(player);
            }
        }
    }

    private static void updateExtendedFood(ServerPlayer player) {
        if (!TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()) {
            removeSprint(player);
            return;
        }
        TSDFoodData.attach(player);
        updateSprint(player);
        FoodData food = player.getFoodData();
        var state = new TSDPayloads.FoodState(food.getFoodLevel(), food.getSaturationLevel(),
                food.getExhaustionLevel(), TSDFoodData.getMaxFood(player));
        if (!state.equals(LAST_SENT.get(player)) || player.tickCount % 100 == 0) {
            ServerPlayNetworking.send(player, state);
            LAST_SENT.put(player, state);
        }
    }

    private static void updateSprint(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        double benefit = TSDFoodData.getExtraBenefit(player);
        var existing = attribute.getModifier(SPRINT_ID);
        if (existing != null && (!player.isSprinting() || existing.amount() != benefit || benefit <= 0)) {
            attribute.removeModifier(SPRINT_ID);
        }
        if (benefit > 0.0D && player.isSprinting() && !attribute.hasModifier(SPRINT_ID)) {
            attribute.addTransientModifier(new AttributeModifier(SPRINT_ID, benefit,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeSprint(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SPRINT_ID);
        }
    }

    /** Carries the extended food bar across respawns and dimension changes. */
    public static void onCopyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (!TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()) {
            return;
        }
        TSDFoodData.attach(newPlayer);
        if (alive) {
            TSDFoodData.copy(oldPlayer.getFoodData(), newPlayer.getFoodData());
        }
    }

    /** Called after the respawn to honour the vanilla respawn option. */
    public static void onAfterRespawn(ServerPlayer newPlayer, boolean alive) {
        if (!TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get() || alive) {
            return;
        }
        TSDFoodData.resetForRespawn(newPlayer,
                !TSDConfig.EXTENDED_FOOD_KEEP_VANILLA_RESPAWN.get(),
                new FoodData().getSaturationLevel());
    }

    /** Mining speed bonus inside the extended hunger range. */
    public static float modifyBreakSpeed(Player player, float speed) {
        if (!TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()) {
            return speed;
        }
        double benefit = TSDFoodData.getExtraBenefit(player);
        return benefit > 0.0D ? (float) (speed * (1.0D + benefit)) : speed;
    }

    private static void scanInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            checkAcquiredStack(player, player.getInventory().getItem(slot));
        }
        checkAcquiredStack(player, player.containerMenu.getCarried());
    }

    /** Acquisition hooks cover pickups, crafting, containers and held stacks. */
    public static void checkAcquiredStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getOrDefault(TSDRegistry.Components.COOKED_ADVANCEMENT, false)) {
            if (stack.is(TSDRegistry.Items.MILLION_POUND_MEAL)) {
                TSDRegistry.Triggers.MILLION_POUND_MEAL.trigger(player);
            } else if (stack.is(TSDRegistry.Blocks.NAGA_MIXED_RICE.asItem())) {
                TSDRegistry.Triggers.TIME_TO_EVEN_THE_SCALES.trigger(player);
            }
        }
        if (stack.is(TSDRegistry.Items.FIRE_BEETLE_FLAME_SAC)) {
            TSDRegistry.Triggers.FIRE_BEETLE_SAC.trigger(player);
        } else if (stack.is(TSDRegistry.Items.SLIME_BEETLE_HONEY_GLAND)) {
            TSDRegistry.Triggers.SLIME_BEETLE_GLAND.trigger(player);
        } else if (stack.is(TSDRegistry.Items.REDCAP_SPICE)) {
            TSDRegistry.Triggers.REDCAP_SPICE.trigger(player);
        } else if (stack.is(TSDRegistry.Blocks.TWILIGHT_CHEESE_FONDUE.asItem())) {
            TSDRegistry.Triggers.FONDUE_FOREIGN_STYLE.trigger(player);
        } else if (stack.is(TSDRegistry.Blocks.SALT_HELMET_CRAB.asItem())) {
            TSDRegistry.Triggers.SELF_CONTAINED_COOKWARE.trigger(player);
        } else if (stack.is(TSDRegistry.Items.TWILIGHT_CHEESE_FONDUE_COMPANION)
                && TSDItems.getFondueChef(stack) == null) {
            TSDItems.setFondueChef(stack, player.getUUID());
        }
    }

    /** Drops the stored client cap when a player disconnects. */
    public static void onDisconnect(ServerPlayer player) {
        LAST_SENT.remove(player);
        TSDPersistentData.of(player).remove(TSDFoodData.CLIENT_CAP);
    }
}

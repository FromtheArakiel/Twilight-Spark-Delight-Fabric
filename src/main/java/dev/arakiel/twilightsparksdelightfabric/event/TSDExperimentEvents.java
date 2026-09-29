package dev.arakiel.twilightsparksdelightfabric.event;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.common.network.TSDPayloads;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Experiment 250: fatal protection, proliferation and the revive bookkeeping.
 *
 * <p>Fatal protection has to run before the damage is applied, so the damage
 * mixin forwards every lethal hit made against a server player to
 * {@link #preventFatalDamage}.</p>
 */
public final class TSDExperimentEvents {
    private static final String FATAL_TRIGGER_COUNT =
            "twilightsparksdelightfabric.experiment_250_fatal_trigger_count";
    private static final TagKey<Item> BREAD_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/bread"));
    private static final TagKey<Item> DOUGH_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/dough"));
    private static final TagKey<Item> LEGACY_DOUGH_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dough"));

    private TSDExperimentEvents() {
    }

    /**
     * @return {@code true} when the fatal hit was replaced by fatal protection
     */
    public static boolean preventFatalDamage(ServerPlayer player,
                                             net.minecraft.world.damagesource.DamageSource source,
                                             float damage) {
        if (damage <= 0 || damage - player.getAbsorptionAmount() < player.getHealth()) {
            return false;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        boolean bossAttack = TSDEvents.isBoss(attacker == null ? null : attacker.getType());
        if (bossAttack && !TSDConfig.EXPERIMENT_FATAL_ALLOWS_BOSS_ATTACKS.get()) {
            return false;
        }
        ResourceLocation attackerId = attacker == null
                ? null : BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
        boolean consumesTrigger = !TSDConfig.isExperimentFatalNonConsumingAttacker(attackerId)
                && (!bossAttack || TSDConfig.EXPERIMENT_FATAL_BOSS_ATTACKS_CONSUME_TRIGGER.get());
        int used = getFatalTriggerCount(player);
        if (consumesTrigger && used >= TSDConfig.EXPERIMENT_FATAL_TRIGGERS_PER_SLEEP.get()) {
            return false;
        }

        ItemStack container = findExperiment(player);
        double cost = TSDConfig.EXPERIMENT_FATAL_ACTIVITY_COST.get();
        if (container.isEmpty() || TSDItems.Experiment250.getActivity(container) <= cost) {
            return false;
        }

        TSDItems.Experiment250.setActivity(container,
                TSDItems.Experiment250.getActivity(container) - cost);
        if (consumesTrigger) {
            setFatalTriggerCount(player, used + 1);
        }
        int stored = consumesTrigger ? used + 1 : used;
        forEachExperiment(player, stack -> stack.set(TSDRegistry.Components.EXPERIMENT_REVIVES, stored));
        player.setHealth(Math.max(player.getHealth(), player.getMaxHealth() * 0.5F));
        player.getFoodData().setFoodLevel(TSDFoodData.getMaxFood(player));
        player.getFoodData().setSaturation(TSDFoodData.getMaxSaturation(player));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        ServerPlayNetworking.send(player, new TSDPayloads.ExperimentActivation(container.copy()));
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public static void onItemFinished(ServerPlayer player, ItemStack stack) {
        maybeProliferate(player, stack, true);
    }

    public static void onAfterDamage(LivingEntity entity, float damageTaken) {
        if (entity instanceof ServerPlayer player && damageTaken > 0.0F) {
            maybeProliferate(player, ItemStack.EMPTY, false);
        }
    }

    public static void onDeath(Player player) {
        setFatalTriggerCount(player, 0);
        clearReviveCounts(player);
    }

    public static void onWakeUp(Player player) {
        setFatalTriggerCount(player, 0);
        forEachExperiment(player, stack -> stack.remove(TSDRegistry.Components.EXPERIMENT_REVIVES));
    }

    /** Keeps the stored revive counter of every carried experiment in sync. */
    public static void tick(ServerPlayer player) {
        int used = getFatalTriggerCount(player);
        forEachExperiment(player, stack -> {
            if (stack.getOrDefault(TSDRegistry.Components.EXPERIMENT_REVIVES, -1) != used) {
                stack.set(TSDRegistry.Components.EXPERIMENT_REVIVES, used);
            }
        });
    }

    private static ItemStack findExperiment(Player player) {
        var location = TSDConfig.EXPERIMENT_FATAL_ITEM_LOCATION.get();
        for (ItemStack stack : player.getInventory().offhand) {
            if (isUsableExperiment(stack)) {
                return stack;
            }
        }
        if (location == TSDConfig.FatalProtectionItemLocation.OFFHAND) {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isUsableExperiment(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isUsableExperiment(ItemStack stack) {
        return stack.getItem() instanceof TSDItems.Experiment250
                && TSDItems.Experiment250.getActivity(stack) > TSDConfig.EXPERIMENT_FATAL_ACTIVITY_COST.get();
    }

    private static void clearReviveCounts(Player player) {
        forEachExperiment(player, stack -> stack.remove(TSDRegistry.Components.EXPERIMENT_REVIVES));
    }

    private static int getFatalTriggerCount(Player player) {
        return Math.max(0, TSDPersistentData.of(player).getInt(FATAL_TRIGGER_COUNT));
    }

    private static void setFatalTriggerCount(Player player, int count) {
        TSDPersistentData.of(player).putInt(FATAL_TRIGGER_COUNT, Math.max(0, count));
    }

    private static void forEachExperiment(Player player, java.util.function.Consumer<ItemStack> action) {
        player.getInventory().items.forEach(stack -> {
            if (stack.getItem() instanceof TSDItems.Experiment250) {
                action.accept(stack);
            }
        });
        player.getInventory().offhand.forEach(stack -> {
            if (stack.getItem() instanceof TSDItems.Experiment250) {
                action.accept(stack);
            }
        });
    }

    private static void maybeProliferate(ServerPlayer player, ItemStack consumedStack, boolean fromEating) {
        if (player.getRandom().nextDouble() > TSDConfig.EXPERIMENT_PROLIFERATION_CHANCE.get()
                || !hasCatalyst(player)) {
            return;
        }

        boolean ateBread = fromEating && isBreadLike(consumedStack);
        int breadSlot = ateBread ? -1 : findBreadSlot(player);
        if (!ateBread && breadSlot < 0) {
            return;
        }

        MetalChoice metal = findMetalChoice(player);
        if (breadSlot >= 0) {
            player.getInventory().removeItem(breadSlot, 1);
        }
        if (metal != null) {
            player.getInventory().removeItem(metal.slot(), metal.count());
        }

        ItemStack output = new ItemStack(metal == null
                ? TSDRegistry.Items.EXPERIMENT_000 : TSDRegistry.Items.EXPERIMENT_PROTOTYPE);
        player.getInventory().placeItemBackInInventory(output);
        player.displayClientMessage(Component.translatable(
                "twilightsparksdelightfabric.message.experiment_proliferation"), true);
        var data = TSDPersistentData.of(player);
        if (!data.getBoolean("twilightsparksdelightfabric.quietly_wriggling_unlocked")) {
            data.putBoolean("twilightsparksdelightfabric.quietly_wriggling_unlocked", true);
            TSDRegistry.Triggers.QUIETLY_WRIGGLING.trigger(player);
        }
    }

    private static boolean hasCatalyst(Player player) {
        return player.getInventory().items.stream()
                .anyMatch(stack -> stack.is(TSDRegistry.Items.EXPERIMENT_PROTOTYPE)
                        || stack.is(twilightforest.init.TFItems.KNIGHT_PHANTOM_TROPHY.get()));
    }

    private static int findBreadSlot(Player player) {
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            if (isBreadLike(player.getInventory().items.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isBreadLike(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(BREAD_TAG) || stack.is(DOUGH_TAG)
                || stack.is(LEGACY_DOUGH_TAG)
                || stack.is(Items.BREAD)
                || stack.is(TSDRegistry.Items.LIVEROOT_BREAD)
                || stack.is(TSDRegistry.Items.LIVEROOT_DOUGH)
                || stack.is(TSDRegistry.Items.LIVEROOT_FLOUR));
    }

    private static MetalChoice findMetalChoice(Player player) {
        int slot = findItem(player, twilightforest.init.TFItems.KNIGHTMETAL_INGOT.get(), 1);
        if (slot >= 0) {
            return new MetalChoice(slot, 1);
        }
        slot = findItem(player, twilightforest.init.TFItems.ARMOR_SHARD_CLUSTER.get(), 1);
        if (slot >= 0) {
            return new MetalChoice(slot, 1);
        }
        slot = findItem(player, twilightforest.init.TFItems.ARMOR_SHARD.get(), 9);
        return slot >= 0 ? new MetalChoice(slot, 9) : null;
    }

    private static int findItem(Player player, Item item, int count) {
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            ItemStack stack = player.getInventory().items.get(index);
            if (stack.is(item) && stack.getCount() >= count) {
                return index;
            }
        }
        return -1;
    }

    /** Shared eligibility check for the lifedrain scepter charging hook. */
    public static ItemStack findRedirectExperiment(LivingEntity user) {
        if (!(user instanceof Player player) || player.level().isClientSide
                || !player.isUsingItem()
                || !player.getUseItem().is(twilightforest.init.TFItems.LIFEDRAIN_SCEPTER.get())) {
            return ItemStack.EMPTY;
        }
        var other = player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
        ItemStack experiment = player.getItemInHand(other);
        return experiment.getItem() instanceof TSDItems.Experiment250
                && TSDItems.Experiment250.getActivity(experiment)
                < TSDItems.Experiment250.getCapacity(experiment)
                ? experiment : ItemStack.EMPTY;
    }

    private record MetalChoice(int slot, int count) {
    }
}

package dev.arakiel.twilightsparksdelightfabric.common.item;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.item.ConsumableItem;

/**
 * Every item of the mod together with the helpers that read and write the item
 * components owned by this mod.
 */
public final class TSDItems {
    private TSDItems() {
    }

    // ------------------------------------------------------------------
    // Naga mixed rice variant helpers
    // ------------------------------------------------------------------

    /** Adds the translated variant label of a naga mixed rice serving. */
    public static void addNagaRiceLabel(ItemStack stack, List<Component> tooltip) {
        String variant = stack.get(TSDRegistry.Components.NAGA_INGREDIENT);
        if (!"hydra".equals(variant) && !"experiment".equals(variant)) {
            return;
        }
        tooltip.add(Component.literal("[").append(Component.translatable(
                        "twilightsparksdelightfabric.tooltip.naga_mixed_rice." + variant)).append("]")
                .withStyle("hydra".equals(variant) ? ChatFormatting.GOLD : ChatFormatting.AQUA));
    }

    /** Extra effects granted by a variant of the naga mixed rice. */
    public static List<MobEffectInstance> nagaRiceEffects(ItemStack stack) {
        if (!stack.is(TSDRegistry.Items.BOWL_OF_NAGA_MIXED_RICE)
                && !stack.is(TSDRegistry.Items.NAGA_MIXED_RICE_CUP)) {
            return List.of();
        }
        int duration = stack.is(TSDRegistry.Items.NAGA_MIXED_RICE_CUP) ? 3600 : 6000;
        String variant = stack.get(TSDRegistry.Components.NAGA_INGREDIENT);
        if ("experiment".equals(variant)) {
            return List.of(new MobEffectInstance(TSDRegistry.Effects.SORROW, duration, 0, false, false, true));
        }
        if ("hydra".equals(variant)) {
            var effect = BuiltInRegistries.MOB_EFFECT.getHolder(
                            ResourceLocation.fromNamespaceAndPath("twilightdelight", "fire_range"))
                    .<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>>map(value -> value)
                    .orElse(MobEffects.FIRE_RESISTANCE);
            return List.of(new MobEffectInstance(effect, duration, 0, false, false, true));
        }
        return List.of();
    }

    // ------------------------------------------------------------------
    // Twilight cheese fondue companion helpers
    // ------------------------------------------------------------------

    public static final int MAX_FONDUE_USES = 6;

    public static ItemStack withFullDurability(ItemStack stack) {
        setFondueUses(stack, MAX_FONDUE_USES);
        return stack;
    }

    public static int getRemainingUses(ItemStack stack) {
        ensureFondueIntegrity(stack);
        return stack.getOrDefault(TSDRegistry.Components.COMPANION_USES, 1);
    }

    public static void damageFondueCompanion(ItemStack stack, Player player) {
        ensureFondueIntegrity(stack);
        if (player.getAbilities().instabuild) {
            return;
        }
        int uses = getRemainingUses(stack);
        if (uses <= 1) {
            stack.shrink(1);
        } else {
            setFondueUses(stack, uses - 1);
        }
    }

    public static void setFondueChef(ItemStack stack, UUID chef) {
        if (chef != null) {
            stack.set(TSDRegistry.Components.COMPANION_CHEF, chef.toString());
        }
    }

    public static UUID getFondueChef(ItemStack stack) {
        String value = stack.getOrDefault(TSDRegistry.Components.COMPANION_CHEF, "");
        try {
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void addFondueDiner(ItemStack stack, UUID diner) {
        if (diner == null) {
            return;
        }
        List<String> diners = new ArrayList<>(
                stack.getOrDefault(TSDRegistry.Components.COMPANION_DINERS, List.of()));
        String value = diner.toString();
        if (diners.contains(value)) {
            return;
        }
        diners.add(value);
        stack.set(TSDRegistry.Components.COMPANION_DINERS, diners);
    }

    public static int getFondueDinerCount(ItemStack stack) {
        return stack.getOrDefault(TSDRegistry.Components.COMPANION_DINERS, List.of()).size();
    }

    public static List<UUID> getFondueDiners(ItemStack stack) {
        List<String> stored = stack.get(TSDRegistry.Components.COMPANION_DINERS);
        if (stored == null) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>();
        for (String value : stored) {
            try {
                result.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore stale or malformed entries from externally edited data.
            }
        }
        return List.copyOf(result);
    }

    public static void ensureFondueIntegrity(ItemStack stack) {
        setFondueUses(stack, stack.getOrDefault(TSDRegistry.Components.COMPANION_USES, 1));
        if (stack.isEnchanted()) {
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        stack.remove(DataComponents.STORED_ENCHANTMENTS);
        stack.remove(DataComponents.UNBREAKABLE);
    }

    private static void setFondueUses(ItemStack stack, int uses) {
        int clamped = Math.max(1, Math.min(MAX_FONDUE_USES, uses));
        stack.set(TSDRegistry.Components.COMPANION_USES, clamped);
        stack.setDamageValue(MAX_FONDUE_USES - clamped);
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    /**
     * Farmer's Delight consumable that preserves the legacy use animation and
     * duration while keeping the upstream food effect tooltip.
     */
    public static class Consumable extends ConsumableItem {
        private final UseAnim useAnimation;
        private final int useDuration;
        private final List<Supplier<MobEffectInstance>> displayEffects = new ArrayList<>();

        public Consumable(Properties properties, UseAnim useAnimation, int useDuration) {
            super(properties, true);
            this.useAnimation = useAnimation;
            this.useDuration = Math.max(1, useDuration);
        }

        public Consumable addDisplayEffect(Supplier<MobEffectInstance> effect) {
            displayEffects.add(effect);
            return this;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return useAnimation;
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity consumer) {
            return useDuration;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    List<Component> tooltip, TooltipFlag flag) {
            addNagaRiceLabel(stack, tooltip);
            if (stack.is(TSDRegistry.Items.MILLION_POUND_MEAL)) {
                tooltip.add(Component.translatable("twilightsparksdelightfabric.tooltip.million_pound_meal.1")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("twilightsparksdelightfabric.tooltip.million_pound_meal.2")
                        .withStyle(ChatFormatting.GOLD));
            }
            if (!vectorwing.farmersdelight.common.Configuration.ENABLE_FOOD_EFFECT_TOOLTIP.get()) {
                return;
            }
            List<MobEffectInstance> effects = new ArrayList<>();
            for (Supplier<MobEffectInstance> supplier : displayEffects) {
                effects.add(supplier.get());
            }
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food != null) {
                for (var possible : food.effects()) {
                    effects.add(possible.effect());
                }
            }
            effects.addAll(nagaRiceEffects(stack));
            if (!effects.isEmpty()) {
                PotionContents.addPotionTooltip(effects, tooltip::add, 1.0F, context.tickRate());
            }
        }
    }

    /** Removes a limited number of random harmful effects after being eaten. */
    public static class MultiCure extends Consumable {
        private final int maximumCures;

        public MultiCure(Properties properties, int maximumCures) {
            this(properties, maximumCures, UseAnim.EAT, 32);
        }

        public MultiCure(Properties properties, int maximumCures, UseAnim useAnimation, int useDuration) {
            super(properties, useAnimation, useDuration);
            this.maximumCures = maximumCures;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
            ItemStack result = super.finishUsingItem(stack, level, consumer);
            if (!level.isClientSide) {
                List<MobEffectInstance> harmful = new ArrayList<>();
                for (MobEffectInstance effect : consumer.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL
                            && !TSDConfig.RANDOM_CURE_EFFECT_BLACKLIST.get().contains(
                                    BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString())) {
                        harmful.add(effect);
                    }
                }
                int cures = Math.min(maximumCures, harmful.size());
                for (int i = 0; i < cures; i++) {
                    MobEffectInstance effect = harmful.remove(consumer.getRandom().nextInt(harmful.size()));
                    consumer.removeEffect(effect.getEffect());
                }
            }
            return result;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable(maximumCures == 1
                            ? "twilightsparksdelightfabric.tooltip.random_cure"
                            : "twilightsparksdelightfabric.tooltip.random_cure_three")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** Pickled bracken that can be thrown while sneaking. */
    public static final class PickledBracken extends ConsumableItem {
        public PickledBracken(Properties properties) {
            super(properties, true);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) {
                return super.use(level, player, hand);
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                var projectile = new dev.arakiel.twilightsparksdelightfabric.common.entity.ThrownPickledBracken(
                        level, player);
                projectile.setItem(stack);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 1.0F);
                if (!level.addFreshEntity(projectile)) {
                    return InteractionResultHolder.fail(stack);
                }
                stack.consume(1, player);
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("twilightsparksdelightfabric.tooltip.pickled_bracken_throw")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** Living helmet crab that can be bitten eight times. */
    public static final class HelmetCrab extends Item {
        private static final int MAX_BITES = 8;
        private static final int COOLDOWN_TICKS = 300;

        public HelmetCrab(Properties properties) {
            super(properties.stacksTo(16));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                player.displayClientMessage(
                        Component.translatable("twilightsparksdelightfabric.item.hermit_crab.hint"), true);
                player.hurt(new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(TSDRegistry.DamageTypes.CRAB_BITE), player), 2.0F);

                if (player.isDeadOrDying() && player instanceof ServerPlayer serverPlayer) {
                    var data = dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData.of(serverPlayer);
                    if (!data.getBoolean("twilightsparksdelightfabric.false_teeth_unlocked")) {
                        data.putBoolean("twilightsparksdelightfabric.false_teeth_unlocked", true);
                        TSDRegistry.Triggers.MY_FALSE_TEETH.trigger(serverPlayer);
                    }
                }

                int bites = Math.max(0, stack.getOrDefault(TSDRegistry.Components.HERMIT_CRAB_BITES, 0)) + 1;
                if (bites >= MAX_BITES) {
                    stack.shrink(1);
                    if (!stack.isEmpty()) {
                        stack.set(TSDRegistry.Components.HERMIT_CRAB_BITES, 0);
                    }
                    give(player, new ItemStack(TSDRegistry.Items.HERMIT_CRAB_LEG, 6));
                    give(player, new ItemStack(twilightforest.init.TFItems.ARMOR_SHARD.get(), 45));
                    if (player instanceof ServerPlayer serverPlayer) {
                        TSDRegistry.Triggers.RUTHLESS_IRON_MOUTH.trigger(serverPlayer);
                    }
                } else {
                    stack.set(TSDRegistry.Components.HERMIT_CRAB_BITES, bites);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        private static void give(Player player, ItemStack stack) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /** Double-Crown Ice Cream with a configurable post eating Frosted roll. */
    public static final class IceCream extends Consumable {
        private static final int DEFAULT_FROSTED_TICKS = 3600;
        private static final int LONG_FROSTED_TICKS = 12000;

        public IceCream() {
            super(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(7)
                    .saturationModifier(8.4F / (7.0F * 2.0F))
                    .effect(new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE, 1800, 1, false, false, true), 1.0F)
                    .build()), UseAnim.EAT, 32);
            addDisplayEffect(() -> new MobEffectInstance(
                    twilightforest.init.TFMobEffects.FROSTY, DEFAULT_FROSTED_TICKS));
        }

        @Override
        public void affectConsumer(ItemStack stack, Level level, LivingEntity consumer) {
            double skipChance = clamp(TSDConfig.DOUBLE_CROWN_ICE_CREAM_NO_FROSTED_CHANCE.get());
            double longChance = Math.min(
                    clamp(TSDConfig.DOUBLE_CROWN_ICE_CREAM_LONG_FROSTED_CHANCE.get()), 1.0D - skipChance);
            double roll = level.random.nextDouble();
            if (roll < skipChance) {
                return;
            }
            int duration = roll < skipChance + longChance ? LONG_FROSTED_TICKS : DEFAULT_FROSTED_TICKS;
            consumer.addEffect(new MobEffectInstance(
                    twilightforest.init.TFMobEffects.FROSTY, duration, 0, false, false, true));
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }

    /** Experiment 250 carried activity, level and bound raw meat. */
    public static class Experiment250 extends Item {
        public static final int MIN_LEVEL = 1;
        public static final int MAX_LEVEL = 6;
        private static final double BASE_CAPACITY = 250.0D;
        private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

        public Experiment250(Properties properties) {
            super(properties.stacksTo(1));
        }

        public static ItemStack createStack(Item item, int level, double activity) {
            ItemStack stack = new ItemStack(item);
            setLevel(stack, level);
            setActivity(stack, activity);
            return stack;
        }

        public static int getLevel(ItemStack stack) {
            return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL,
                    stack.getOrDefault(TSDRegistry.Components.EXPERIMENT_LEVEL, MIN_LEVEL)));
        }

        public static void setLevel(ItemStack stack, int level) {
            int clamped = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
            stack.set(TSDRegistry.Components.EXPERIMENT_LEVEL, clamped);
            setActivity(stack, getActivity(stack));
        }

        public static double getActivity(ItemStack stack) {
            return clampActivity(stack.getOrDefault(TSDRegistry.Components.EXPERIMENT_ACTIVITY, 0.0D),
                    getLevel(stack));
        }

        public static void setActivity(ItemStack stack, double activity) {
            stack.set(TSDRegistry.Components.EXPERIMENT_ACTIVITY, clampActivity(activity, getLevel(stack)));
        }

        public static double addActivity(ItemStack stack, double amount) {
            double old = getActivity(stack);
            setActivity(stack, old + Math.max(0.0D, amount));
            return getActivity(stack) - old;
        }

        public static double getCapacity(ItemStack stack) {
            return getCapacity(getLevel(stack));
        }

        public static double getCapacity(int level) {
            double capacity = BASE_CAPACITY;
            for (int i = MIN_LEVEL; i < Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)); i++) {
                capacity *= 3.0D;
            }
            return capacity;
        }

        public static ResourceLocation getBoundMeat(ItemStack stack) {
            String id = stack.getOrDefault(TSDRegistry.Components.EXPERIMENT_BOUND_MEAT, "");
            if (id.isEmpty()) {
                return null;
            }
            return ResourceLocation.tryParse(id);
        }

        public static void setBoundMeat(ItemStack stack, ResourceLocation meat) {
            if (meat == null) {
                stack.remove(TSDRegistry.Components.EXPERIMENT_BOUND_MEAT);
            } else {
                stack.set(TSDRegistry.Components.EXPERIMENT_BOUND_MEAT, meat.toString());
            }
        }

        public static int getRemainingRevives(ItemStack stack) {
            double activity = getActivity(stack);
            double cost = TSDConfig.EXPERIMENT_FATAL_ACTIVITY_COST.get();
            if (activity <= cost) {
                return 0;
            }
            int limit = Math.max(0, TSDConfig.EXPERIMENT_FATAL_TRIGGERS_PER_SLEEP.get()
                    - stack.getOrDefault(TSDRegistry.Components.EXPERIMENT_REVIVES, 0));
            return cost <= 0 ? limit : Math.min(limit, Math.max(0, (int) Math.ceil(activity / cost) - 1));
        }

        /**
         * Model overlay stage of the item, used by the {@code activity_stage}
         * item property of the client model overrides.
         */
        public static float getActivityStage(ItemStack stack) {
            double ratio = getActivity(stack) / getCapacity(stack);
            return ratio >= 2.0D / 3.0D ? 2.0F : ratio >= 1.0D / 3.0D ? 1.0F : 0.0F;
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                      InteractionHand usedHand) {
            if (!TSDConfig.EXPERIMENT_BINDING_MODE_ENABLED.get() || !player.isCrouching()
                    || player.level().isClientSide) {
                return InteractionResult.PASS;
            }
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
            List<ResourceLocation> candidates = TSDConfig.getExperimentBindings().get(entityId);
            if (candidates == null || candidates.isEmpty()) {
                return InteractionResult.PASS;
            }

            ResourceLocation current = getBoundMeat(stack);
            ResourceLocation next = candidates.get(0);
            if (current != null) {
                for (int index = 0; index < candidates.size(); index++) {
                    if (current.equals(candidates.get(index))) {
                        next = candidates.get((index + 1) % candidates.size());
                        break;
                    }
                }
            }
            setBoundMeat(stack, next);
            player.displayClientMessage(Component.translatable(
                    "twilightsparksdelightfabric.message.experiment_250.bound",
                    new ItemStack(BuiltInRegistries.ITEM.get(next)).getHoverName()), true);
            return InteractionResult.SUCCESS;
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            setLevel(stack, getLevel(stack));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("twilightsparksdelightfabric.tooltip.experiment_250.level",
                    getLevel(stack)));
            tooltip.add(Component.translatable("twilightsparksdelightfabric.tooltip.experiment_250.activity",
                    FORMAT.format(getActivity(stack)), FORMAT.format(getCapacity(stack))));
            if (getActivity(stack) > TSDConfig.EXPERIMENT_FATAL_ACTIVITY_COST.get()) {
                tooltip.add(Component.translatable(
                        "twilightsparksdelightfabric.tooltip.experiment_250.remaining_revives",
                        getRemainingRevives(stack)));
            }
            ResourceLocation bound = getBoundMeat(stack);
            if (bound != null && TSDConfig.EXPERIMENT_BINDING_MODE_ENABLED.get()) {
                tooltip.add(Component.translatable("twilightsparksdelightfabric.tooltip.experiment_250.bound_meat",
                        new ItemStack(BuiltInRegistries.ITEM.get(bound)).getHoverName()));
            }
        }

        private static double clampActivity(double activity, int level) {
            if (!Double.isFinite(activity) || activity <= 0.0D) {
                return 0.0D;
            }
            return Math.min(getCapacity(level), activity);
        }
    }

    /**
     * The fondue companion has six servings. Its durability is deliberately
     * stored in a separate tag as an integrity check against external repair
     * systems.
     */
    public static final class FondueCompanion extends Item {
        public FondueCompanion(Properties properties) {
            super(properties.stacksTo(1).durability(MAX_FONDUE_USES - 1));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (!level.isClientSide) {
                ensureFondueIntegrity(stack);
                if (entity instanceof Player player && getFondueChef(stack) == null) {
                    setFondueChef(stack, player.getUUID());
                }
            }
        }

        @Override
        public boolean isEnchantable(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
            return false;
        }

        @Override
        public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
            if (context.getLevel().getBlockState(context.getClickedPos())
                    .is(TSDRegistry.Blocks.TWILIGHT_CHEESE_FONDUE)) {
                return InteractionResult.PASS;
            }
            if (!context.getLevel().isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable(
                        "twilightsparksdelightfabric.item.twilight_cheese_fondue_companion.need_fondue"), true);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    List<Component> tooltip, TooltipFlag flag) {
            ensureFondueIntegrity(stack);
            tooltip.add(Component.translatable(
                    "twilightsparksdelightfabric.tooltip.twilight_cheese_fondue_companion.durability",
                    getRemainingUses(stack), MAX_FONDUE_USES));
        }
    }
}

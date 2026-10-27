package dev.arakiel.twilightsparksdelight.registry;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import dev.arakiel.gemini.creative.GeminiCreativeTabs;
import dev.arakiel.gemini.registration.AutoRegistryContainer;
import dev.arakiel.gemini.registration.GeminiRegistration;
import dev.arakiel.gemini.registration.annotations.AssignedName;
import dev.arakiel.gemini.registration.annotations.RegistryNamespace;
import dev.arakiel.gemini.registration.container.BlockEntityRegistryContainer;
import dev.arakiel.gemini.registration.container.BlockRegistryContainer;
import dev.arakiel.gemini.registration.container.EntityRegistryContainer;
import dev.arakiel.gemini.registration.container.ItemRegistryContainer;
import dev.arakiel.gemini.registration.container.MobEffectRegistryContainer;
import dev.arakiel.twilightsparksdelight.TwilightSparksDelight;
import dev.arakiel.twilightsparksdelight.common.block.ContainerBlocks;
import dev.arakiel.twilightsparksdelight.common.block.FeastBlocks;
import dev.arakiel.twilightsparksdelight.common.block.GloryCrucibleBlock;
import dev.arakiel.twilightsparksdelight.common.block.KitchenBlocks;
import dev.arakiel.twilightsparksdelight.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelight.common.effect.TSDMobEffects;
import dev.arakiel.twilightsparksdelight.common.entity.ThrownPickledBracken;
import dev.arakiel.twilightsparksdelight.common.item.TSDItems;
import dev.arakiel.twilightsparksdelight.common.recipe.TSDCustomRecipes;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import vectorwing.farmersdelight.common.registry.ModEffects;

/**
 * Content registration of the mod.
 *
 * <p>Fabric registers content immediately, so every holder class below is
 * evaluated once from {@link #register()} during the mod initializer, in an
 * order that keeps block, item and block entity dependencies satisfied.</p>
 */
public final class TSDRegistry {
    private TSDRegistry() {
    }

    /** Registers every content holder. Must run inside the mod initializer. */
    public static void register() {
        Effects.init();
        Components.init();
        Fluids.init();
        Entities.init();
        Serials.init();
        Triggers.init();
        Blocks.init();
        Items.init();
        BlockEntities.init();
        CreativeTabs.init();
    }

    private static ResourceLocation id(String path) {
        return TwilightSparksDelight.id(path);
    }

    // ------------------------------------------------------------------
    // Mob effects
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Effects implements MobEffectRegistryContainer {
        public static MobEffect ENLARGE;
        public static MobEffect SHRINK;
        public static MobEffect CHARGE;
        public static MobEffect SYMBIOSIS;
        public static MobEffect GRIEF;
        public static MobEffect SORROW;
        public static MobEffect ABYSS_CALL;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            ENLARGE = effect("enlarge", MobEffectCategory.BENEFICIAL, 0xE5A50A);
            SHRINK = effect("shrink", MobEffectCategory.NEUTRAL, 0x4FC3F7);
            CHARGE = effect("charge", MobEffectCategory.BENEFICIAL, 0xFF7043);
            SYMBIOSIS = effect("symbiosis", MobEffectCategory.BENEFICIAL, 0x8BC34A, TSDMobEffects.Behavior.SYMBIOSIS);
            GRIEF = effect("grief", MobEffectCategory.HARMFUL, 0x5E35B1);
            SORROW = effect("sorrow", MobEffectCategory.HARMFUL, 0x3949AB);
            ABYSS_CALL = effect("abyss_call", MobEffectCategory.HARMFUL, 0x6A1B9A);
            GeminiRegistration.init(Effects.class);
        }

        private static MobEffect effect(String path, MobEffectCategory category, int color) {
            return effect(path, category, color, TSDMobEffects.Behavior.NONE);
        }

        private static MobEffect effect(String path, MobEffectCategory category, int color,
                                        TSDMobEffects.Behavior behavior) {
            return new TSDMobEffects.Effect(category, color, behavior);
        }

        private Effects() {
        }
    }

    // ------------------------------------------------------------------
    // Data components
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Components implements AutoRegistryContainer<DataComponentType<?>> {
        public static DataComponentType<String> NAGA_INGREDIENT;
        public static DataComponentType<Boolean> COOKED_ADVANCEMENT;
        public static DataComponentType<Integer> EXPERIMENT_LEVEL;
        public static DataComponentType<Double> EXPERIMENT_ACTIVITY;
        public static DataComponentType<String> EXPERIMENT_BOUND_MEAT;
        public static DataComponentType<Integer> EXPERIMENT_REVIVES;
        public static DataComponentType<Integer> HERMIT_CRAB_BITES;
        public static DataComponentType<Integer> COMPANION_USES;
        public static DataComponentType<String> COMPANION_CHEF;
        public static DataComponentType<List<String>> COMPANION_DINERS;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            NAGA_INGREDIENT = component("naga_ingredient", Codec.STRING);
            COOKED_ADVANCEMENT = component("cooked_advancement", Codec.BOOL);
            EXPERIMENT_LEVEL = component("experiment_level", Codec.INT);
            EXPERIMENT_ACTIVITY = component("experiment_activity", Codec.DOUBLE);
            EXPERIMENT_BOUND_MEAT = component("experiment_bound_meat", Codec.STRING);
            EXPERIMENT_REVIVES = component("experiment_revives", Codec.INT);
            HERMIT_CRAB_BITES = component("hermit_crab_bites", Codec.INT);
            COMPANION_USES = component("companion_uses", Codec.INT);
            COMPANION_CHEF = component("companion_chef", Codec.STRING);
            COMPANION_DINERS = component("companion_diners", Codec.STRING.listOf());
            GeminiRegistration.register(Components.class);
        }

        private static <T> DataComponentType<T> component(String path, Codec<T> codec) {
            return DataComponentType.<T>builder().persistent(codec).build();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Registry<DataComponentType<?>> getRegistry() {
            return (Registry<DataComponentType<?>>) (Registry<?>) BuiltInRegistries.DATA_COMPONENT_TYPE;
        }

        @Override
        public Class<DataComponentType<?>> getTargetFieldType() {
            return AutoRegistryContainer.conform(DataComponentType.class);
        }

        private Components() {
        }
    }

    // ------------------------------------------------------------------
    // Fluids
    // ------------------------------------------------------------------

    /**
     * The fluids are data tokens for the glory crucible tank: they are never
     * placed in the world and therefore only implement the source lookups.
     */
    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Fluids implements AutoRegistryContainer<Fluid> {
        public static Fluid FIERY_BLOOD;
        public static Fluid FLOWING_FIERY_BLOOD;
        public static Fluid FIERY_TEARS;
        public static Fluid FLOWING_FIERY_TEARS;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            FIERY_BLOOD = new SimpleFluid(true);
            FLOWING_FIERY_BLOOD = new SimpleFluid(false);
            FIERY_TEARS = new SimpleFluid(true);
            FLOWING_FIERY_TEARS = new SimpleFluid(false);
            GeminiRegistration.register(Fluids.class);
        }

        @Override
        public Registry<Fluid> getRegistry() {
            return BuiltInRegistries.FLUID;
        }

        @Override
        public Class<Fluid> getTargetFieldType() {
            return Fluid.class;
        }

        public static boolean isHeatingFluid(Fluid fluid) {
            return fluid == FIERY_BLOOD || fluid == FIERY_TEARS;
        }

        private Fluids() {
        }
    }

    /**
     * Minimal fluid implementation used only as a tank content token.
     *
     * <p>These fluids are never placed in the world, so only the source lookups
     * have to be meaningful.</p>
     */
    public static final class SimpleFluid extends Fluid {
        private final boolean source;

        SimpleFluid(boolean source) {
            this.source = source;
        }

        @Override
        public Item getBucket() {
            return net.minecraft.world.item.Items.AIR;
        }

        @Override
        public boolean isSource(net.minecraft.world.level.material.FluidState state) {
            return source;
        }

        @Override
        public int getAmount(net.minecraft.world.level.material.FluidState state) {
            return source ? 8 : 0;
        }

        @Override
        protected boolean canBeReplacedWith(net.minecraft.world.level.material.FluidState state,
                                            net.minecraft.world.level.BlockGetter level,
                                            net.minecraft.core.BlockPos pos, Fluid fluid,
                                            net.minecraft.core.Direction direction) {
            return false;
        }

        @Override
        protected net.minecraft.world.phys.Vec3 getFlow(net.minecraft.world.level.BlockGetter level,
                                                        net.minecraft.core.BlockPos pos,
                                                        net.minecraft.world.level.material.FluidState state) {
            return net.minecraft.world.phys.Vec3.ZERO;
        }

        @Override
        public int getTickDelay(net.minecraft.world.level.LevelReader level) {
            return 5;
        }

        @Override
        protected float getExplosionResistance() {
            return 100.0F;
        }

        @Override
        public float getHeight(net.minecraft.world.level.material.FluidState state,
                               net.minecraft.world.level.BlockGetter level,
                               net.minecraft.core.BlockPos pos) {
            return source ? 8.0F / 9.0F : 0.0F;
        }

        @Override
        public float getOwnHeight(net.minecraft.world.level.material.FluidState state) {
            return getHeight(state, null, null);
        }

        @Override
        protected net.minecraft.world.level.block.state.BlockState createLegacyBlock(
                net.minecraft.world.level.material.FluidState state) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        @Override
        public net.minecraft.world.phys.shapes.VoxelShape getShape(
                net.minecraft.world.level.material.FluidState state,
                net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }

        @Override
        public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound() {
            return java.util.Optional.empty();
        }
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Entities implements EntityRegistryContainer {
        public static EntityType<ThrownPickledBracken> PICKLED_BRACKEN;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            PICKLED_BRACKEN = EntityType.Builder.<ThrownPickledBracken>of(
                            ThrownPickledBracken::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build(null);
            GeminiRegistration.init(Entities.class);
        }

        private Entities() {
        }
    }

    // ------------------------------------------------------------------
    // Recipe serializers
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Serials implements AutoRegistryContainer<RecipeSerializer<?>> {
        public static RecipeSerializer<TSDCustomRecipes.Experiment250Replication>
                EXPERIMENT_250_REPLICATION;
        public static RecipeSerializer<TSDCustomRecipes.ReusableWaterShapeless> REUSABLE_WATER_SHAPELESS;
        public static RecipeSerializer<TSDCustomRecipes.KeepingItemShapeless> KEEPING_ITEM_SHAPELESS;
        public static RecipeSerializer<TSDCustomRecipes.HelmetCrabCutting> HELMET_CRAB_CUTTING;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            EXPERIMENT_250_REPLICATION = new TSDCustomRecipes.Experiment250Replication.Serializer();
            REUSABLE_WATER_SHAPELESS = new TSDCustomRecipes.ReusableWaterShapeless.Serializer();
            KEEPING_ITEM_SHAPELESS = new TSDCustomRecipes.KeepingItemShapeless.Serializer();
            HELMET_CRAB_CUTTING = new TSDCustomRecipes.HelmetCrabCutting.Serializer();
            GeminiRegistration.register(Serials.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Registry<RecipeSerializer<?>> getRegistry() {
            return (Registry<RecipeSerializer<?>>) (Registry<?>) BuiltInRegistries.RECIPE_SERIALIZER;
        }

        @Override
        public Class<RecipeSerializer<?>> getTargetFieldType() {
            return AutoRegistryContainer.conform(RecipeSerializer.class);
        }

        private Serials() {
        }
    }

    // ------------------------------------------------------------------
    // Advancement triggers
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Triggers implements AutoRegistryContainer<Triggers.Trigger> {
        public static Trigger TIME_TO_EVEN_THE_SCALES;
        public static Trigger REDCAP_SPICE;
        public static Trigger GATHERED_AROUND;
        public static Trigger RUTHLESS_IRON_MOUTH;
        public static Trigger EXECUTIVE_CHEF;
        public static Trigger MY_FALSE_TEETH;
        public static Trigger SLIME_BEETLE_GLAND;
        public static Trigger FIRE_BEETLE_SAC;
        public static Trigger MILLION_POUND_MEAL;
        public static Trigger MILK_QUEST_RAM;
        public static Trigger QUIETLY_WRIGGLING;
        public static Trigger MOSS_SPREAD;
        public static Trigger DONT_EAT_ME;
        public static Trigger BURNING_RAM_LORD;
        public static Trigger GUNFIRE_BREAKS_RABBIT_WATCH;
        public static Trigger FONDUE_FOREIGN_STYLE;
        public static Trigger SELF_CONTAINED_COOKWARE;
        public static Trigger LATE_FOR_DATE;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            TIME_TO_EVEN_THE_SCALES = trigger("time_to_even_the_scales");
            REDCAP_SPICE = trigger("redcap_spice");
            GATHERED_AROUND = trigger("gathered_around");
            RUTHLESS_IRON_MOUTH = trigger("ruthless_iron_mouth");
            EXECUTIVE_CHEF = trigger("executive_chef");
            MY_FALSE_TEETH = trigger("my_false_teeth");
            SLIME_BEETLE_GLAND = trigger("slime_beetle_gland");
            FIRE_BEETLE_SAC = trigger("fire_beetle_sac");
            MILLION_POUND_MEAL = trigger("million_pound_meal");
            MILK_QUEST_RAM = trigger("milk_quest_ram");
            QUIETLY_WRIGGLING = trigger("quietly_wriggling");
            MOSS_SPREAD = trigger("moss_spread");
            DONT_EAT_ME = trigger("dont_eat_me");
            BURNING_RAM_LORD = trigger("burning_ram_lord");
            GUNFIRE_BREAKS_RABBIT_WATCH = trigger("gunfire_breaks_rabbit_watch");
            FONDUE_FOREIGN_STYLE = trigger("fondue_foreign_style");
            SELF_CONTAINED_COOKWARE = trigger("self_contained_cookware");
            LATE_FOR_DATE = trigger("late_for_date");
            GeminiRegistration.register(Triggers.class);
        }

        private static Trigger trigger(String path) {
            return new Trigger();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Registry<Trigger> getRegistry() {
            return (Registry<Trigger>) (Registry<?>) BuiltInRegistries.TRIGGER_TYPES;
        }

        @Override
        public Class<Trigger> getTargetFieldType() {
            return Trigger.class;
        }

        /**
         * Legacy advancement trigger ids kept as real one twenty one trigger
         * types so the shipped advancements stay loadable. The runtime awards
         * the matching criterion directly.
         */
        public static final class Trigger extends SimpleCriterionTrigger<Trigger.Instance> {
            @Override
            public Codec<Instance> codec() {
                return Instance.CODEC;
            }

            public void trigger(net.minecraft.server.level.ServerPlayer player) {
                trigger(player, ignored -> true);
            }

            public record Instance(java.util.Optional<ContextAwarePredicate> player)
                    implements SimpleCriterionTrigger.SimpleInstance {
                public static final Codec<Instance> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder
                        .create(instance -> instance.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                        .forGetter(Instance::player)
                        ).apply(instance, Instance::new));
            }
        }

        private Triggers() {
        }
    }

    // ------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Blocks implements BlockRegistryContainer {
        public static FeastBlocks.CheeseFondue TWILIGHT_CHEESE_FONDUE;
        public static ContainerBlocks.SaltCrab SALT_HELMET_CRAB;
        public static ContainerBlocks.Colony LABYRINTH_MUSHROOM_COLONY;
        public static ContainerBlocks.Colony TWILIGHT_BRACKEN_COLONY;
        public static ContainerBlocks.UnripeJar UNRIPE_PICKLED_BRACKEN_JAR;
        public static ContainerBlocks.PickledJar PICKLED_BRACKEN_JAR;
        public static GloryCrucibleBlock GLORY_CRUCIBLE;
        public static FeastBlocks.Borscht TWILIGHT_BORSCHT;
        public static Block LABYRINTH_MUSHROOM_CRATE;
        public static Block BRACKEN_CRATE;
        @NoBlockItem
        public static FeastBlocks.StructurePart NAGA_MIXED_RICE_PART;
        public static FeastBlocks.NagaRice NAGA_MIXED_RICE;
        public static FeastBlocks.AbyssPie ABYSS_PIE;
        @NoBlockItem
        public static FeastBlocks.StructurePart TWILIGHT_BOAR_KNUCKLE_PART;
        public static FeastBlocks.BoarKnuckle TWILIGHT_BOAR_KNUCKLE;
        public static KitchenBlocks.GiantStove GIANT_STOVE;
        @NoBlockItem
        public static KitchenBlocks.KitchenPart GIANT_STOVE_PART;
        public static KitchenBlocks.GiantPot GIANT_COOKING_POT;
        @NoBlockItem
        public static KitchenBlocks.KitchenPart GIANT_COOKING_POT_PART;
        public static KitchenBlocks.GiantStove GIANTS_STOVE;
        @NoBlockItem
        public static KitchenBlocks.KitchenPart GIANTS_STOVE_PART;
        public static KitchenBlocks.GiantPot GIANTS_COOKING_POT;
        @NoBlockItem
        public static KitchenBlocks.KitchenPart GIANTS_COOKING_POT_PART;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;

            TWILIGHT_CHEESE_FONDUE = defineBlock(
                    () -> new FeastBlocks.CheeseFondue(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE).strength(1.5F)));
            SALT_HELMET_CRAB = defineBlock(
                    () -> new ContainerBlocks.SaltCrab(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN).strength(1.5F)));
            LABYRINTH_MUSHROOM_COLONY = defineColony(MapColor.COLOR_RED, () -> Items.LABYRINTH_MUSHROOM);
            TWILIGHT_BRACKEN_COLONY = defineColony(MapColor.COLOR_GREEN, () -> Items.BRACKEN);
            UNRIPE_PICKLED_BRACKEN_JAR = defineBlock(
                    () -> new ContainerBlocks.UnripeJar(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN).strength(0.5F)));
            PICKLED_BRACKEN_JAR = defineBlock(
                    () -> new ContainerBlocks.PickledJar(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN).strength(0.5F)));
            GLORY_CRUCIBLE = defineBlock(
                    () -> new GloryCrucibleBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F)));
            TWILIGHT_BORSCHT = defineBlock(
                    () -> new FeastBlocks.Borscht(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED).strength(1.5F)));
            LABYRINTH_MUSHROOM_CRATE = defineBlock(
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(1.5F).sound(SoundType.WOOD)));
            BRACKEN_CRATE = defineBlock(
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(1.5F).sound(SoundType.WOOD)));

            NAGA_MIXED_RICE_PART = defineBlock(
                    () -> new FeastBlocks.StructurePart(BlockBehaviour.Properties.of().strength(1.0F),
                            () -> NAGA_MIXED_RICE));
            NAGA_MIXED_RICE = defineBlock(
                    () -> new FeastBlocks.NagaRice(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN).strength(1.5F),
                            () -> Items.BOWL_OF_NAGA_MIXED_RICE, () -> NAGA_MIXED_RICE_PART));
            ABYSS_PIE = defineBlock(
                    () -> new FeastBlocks.AbyssPie(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).strength(1.5F)));
            TWILIGHT_BOAR_KNUCKLE_PART = defineBlock(
                    () -> new FeastBlocks.StructurePart(BlockBehaviour.Properties.of().strength(1.0F),
                            () -> TWILIGHT_BOAR_KNUCKLE));
            TWILIGHT_BOAR_KNUCKLE = defineBlock(
                    () -> new FeastBlocks.BoarKnuckle(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN).strength(1.5F),
                            () -> TWILIGHT_BOAR_KNUCKLE_PART));

            GIANT_STOVE = defineBlock(
                    () -> new KitchenBlocks.GiantStove(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANT_STOVE_PART, 2));
            GIANT_STOVE_PART = defineBlock(
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANT_STOVE,
                            KitchenBlocks.KitchenPart.Kind.STOVE));
            GIANT_COOKING_POT = defineBlock(
                    () -> new KitchenBlocks.GiantPot(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANT_COOKING_POT_PART, 2));
            GIANT_COOKING_POT_PART = defineBlock(
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANT_COOKING_POT,
                            KitchenBlocks.KitchenPart.Kind.COOKING_POT));
            GIANTS_STOVE = defineBlock(
                    () -> new KitchenBlocks.GiantStove(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANTS_STOVE_PART, 4));
            GIANTS_STOVE_PART = defineBlock(
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANTS_STOVE,
                            KitchenBlocks.KitchenPart.Kind.STOVE));
            GIANTS_COOKING_POT = defineBlock(
                    () -> new KitchenBlocks.GiantPot(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANTS_COOKING_POT_PART, 4));
            GIANTS_COOKING_POT_PART = defineBlock(
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANTS_COOKING_POT,
                            KitchenBlocks.KitchenPart.Kind.COOKING_POT));

            GeminiRegistration.init(Blocks.class);
        }

        private static <B extends Block> B defineBlock(Supplier<B> supplier) {
            return supplier.get();
        }

        private static ContainerBlocks.Colony defineColony(MapColor color, Supplier<Item> harvestItem) {
            return new ContainerBlocks.Colony(
                    BlockBehaviour.Properties.of().mapColor(color),
                    harvestItem);
        }

        @Override
        public void postProcessField(String namespace, Block block, String identifier, java.lang.reflect.Field field) {
            if (field.isAnnotationPresent(NoBlockItem.class)) {
                return;
            }
            Item item = switch (identifier) {
                case "labyrinth_mushroom_colony", "twilight_bracken_colony" ->
                        new ContainerBlocks.ColonyItem(
                                (ContainerBlocks.Colony) block, new Item.Properties());
                case "giants_cooking_pot" ->
                        new KitchenBlocks.GiantPotItem(new Item.Properties().stacksTo(1));
                case "glory_crucible", "labyrinth_mushroom_crate", "bracken_crate" ->
                        new BlockItem(block, new Item.Properties().stacksTo(64));
                case "twilight_cheese_fondue", "salt_helmet_crab", "unripe_pickled_bracken_jar",
                        "pickled_bracken_jar", "twilight_borscht", "naga_mixed_rice", "abyss_pie",
                        "twilight_boar_knuckle", "giant_stove", "giant_cooking_pot", "giants_stove" ->
                        new BlockItem(block, new Item.Properties().stacksTo(1));
                default -> new BlockItem(block, new Item.Properties());
            };
            getItemSink().register(
                    ResourceLocation.fromNamespaceAndPath(namespace, identifier),
                    item);
        }

        private Blocks() {
        }
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    /** Description of one food effect entry. */
    private record EffectSpec(Holder<MobEffect> effect, int duration, int amplifier, float chance,
                              boolean displayOnly) {
    }

    private static EffectSpec effect(Holder<MobEffect> effect, int duration, int amplifier, float chance) {
        return new EffectSpec(effect, duration, amplifier, chance, false);
    }

    private static EffectSpec effect(MobEffect effect, int duration, int amplifier, float chance) {
        return effect(TSDMobEffects.holder(effect), duration, amplifier, chance);
    }

    private static EffectSpec displayEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new EffectSpec(effect, duration, amplifier, 1.0F, true);
    }

    private static EffectSpec displayEffect(MobEffect effect, int duration, int amplifier) {
        return displayEffect(TSDMobEffects.holder(effect), duration, amplifier);
    }

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class Items implements ItemRegistryContainer {
        @AssignedName("liveroot_cone")
        public static Item LIVEROOT_FLOUR;
        public static Item LIVEROOT_PIE_CRUST;
        public static Item LIVEROOT_DOUGH;
        public static Item LIVEROOT_BREAD;
        public static Item RAW_WILD_BOAR_MEAT;
        public static Item COOKED_WILD_BOAR_MEAT;
        public static Item RAW_WILD_BOAR_MEAT_CUBES;
        public static Item COOKED_WILD_BOAR_MEAT_CUBES;
        public static Item RAW_BIGHORN_MUTTON;
        public static Item COOKED_BIGHORN_MUTTON;
        public static Item RAW_BIGHORN_MUTTON_CHOP;
        public static Item COOKED_BIGHORN_MUTTON_CHOP;
        public static Item HERMIT_CRAB_LEG;
        public static Item COOKED_HERMIT_CRAB_LEG;
        public static Item PINCH_BEETLE_LEG;
        public static Item COOKED_PINCH_BEETLE_LEG;
        public static Item SLIME_BEETLE_LEG;
        public static Item COOKED_SLIME_BEETLE_LEG;
        public static Item FIRE_BEETLE_LEG;
        public static Item COOKED_FIRE_BEETLE_LEG;
        public static Item FIRE_BEETLE_FLAME_SAC;
        public static Item SLIME_BEETLE_HONEY_GLAND;
        public static Item HERMIT_CRAB;
        public static Item QUEST_RAM_MILK;
        public static Item QUEST_RAM_CHEESE;
        public static Item TWILIGHT_SKEWER;
        public static Item LABYRINTH_FLAVOR_SKEWER;
        public static Item LABYRINTH_SASHIMI_MEDLEY;
        @AssignedName("labyrinth_a5_mushroom_flavor_double_cheeseburger")
        public static Item LABYRINTH_A5_DOUBLE_CHEESEBURGER;
        public static Item TORCHBERRY_SAUCE;
        public static Item LABYRINTH_TACO;
        public static Item GELID_CRYSTAL;
        public static Item LABYRINTH_MUSHROOM;
        public static Item EXPERIMENT_PROTOTYPE;
        public static Item EXPERIMENT_000;
        public static Item EXPERIMENT_234;
        public static Item ABYSS_PIE_SLICE;
        public static Item EXPERIMENT_250;
        public static Item FIERY_SLAG;
        public static Item GRIDDLE_TENTACLE;
        public static Item SCOURGE_STEAK;
        public static Item REDCAP_SPICE;
        public static Item DRINK_ME;
        public static Item EAT_ME;
        public static Item MINO_MINCE;
        public static Item MINO_PATTY;
        public static Item TRAIL_RATIONS;
        public static Item BRACKEN;
        public static Item PICKLED_BRACKEN;
        public static Item BOWL_OF_TWILIGHT_BORSCHT;
        public static Item TWILIGHT_BORSCHT_CUP;
        public static Item PLATE_OF_TWILIGHT_BOAR_KNUCKLE;
        public static Item CREAM_OF_LABYRINTH_MUSHROOM_SOUP;
        public static Item CREAM_OF_LABYRINTH_MUSHROOM_SOUP_CUP;
        public static Item BOWL_OF_CHICKEN_AND_HYDRA_SOUP;
        public static Item CHICKEN_AND_HYDRA_SOUP_CUP;
        public static Item STIR_FRIED_BRACKEN;
        public static Item HELMET_CRAB_LEG_SUSHI_ROLL;
        public static Item HELMET_CRAB_LEG_SUSHI;
        public static Item TENTACLE_CHOW_MEIN;
        public static Item MILLION_POUND_MEAL;
        public static Item TWILIGHT_CHEESE_FONDUE_COMPANION;
        public static Item TWILIGHT_CHEESE_FONDUE_WITH_BREAD;
        public static Item TWILIGHT_SUPREME_SANDWICH;
        public static Item DOUBLE_CROWN_ICE_CREAM;
        public static Item TWIN_RADIANCE_ICE_POP;
        public static Item SALT_ROASTED_HELMET_CRAB_CLAW;
        public static Item BOWL_OF_SALTED_CRAB_MEAT;
        public static Item BOWL_OF_NAGA_MIXED_RICE;
        public static Item NAGA_MIXED_RICE_CUP;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;

            LIVEROOT_FLOUR = basic("liveroot_cone");
            LIVEROOT_PIE_CRUST = basic("liveroot_pie_crust");
            LIVEROOT_DOUGH = food("liveroot_dough", 2, 2.4F, false, null,
                    effect(MobEffects.BLINDNESS, 600, 0, 0.3F));
            LIVEROOT_BREAD = food("liveroot_bread", 6, 5.0F, false, null);

            RAW_WILD_BOAR_MEAT = food("raw_wild_boar_meat", 3, 1.6F, false, null);
            COOKED_WILD_BOAR_MEAT = food("cooked_wild_boar_meat", 7, 7.2F, false, null);
            RAW_WILD_BOAR_MEAT_CUBES = food("raw_wild_boar_meat_cubes", 2, 1.3F, false, null);
            COOKED_WILD_BOAR_MEAT_CUBES = food("cooked_wild_boar_meat_cubes", 4, 4.1F, false, null);

            RAW_BIGHORN_MUTTON = food("raw_bighorn_mutton", 2, 1.6F, false, null);
            COOKED_BIGHORN_MUTTON = food("cooked_bighorn_mutton", 7, 7.2F, false, null);
            RAW_BIGHORN_MUTTON_CHOP = food("raw_bighorn_mutton_chop", 2, 1.3F, false, null);
            COOKED_BIGHORN_MUTTON_CHOP = food("cooked_bighorn_mutton_chop", 4, 4.1F, false, null);

            HERMIT_CRAB_LEG = food("hermit_crab_leg", 4, 1.5F, false, null);
            COOKED_HERMIT_CRAB_LEG = food("cooked_hermit_crab_leg", 6, 3.5F, false, null,
                    effect(MobEffects.NIGHT_VISION, 3600, 0, 1.0F));
            PINCH_BEETLE_LEG = food("pinch_beetle_leg", 4, 1.5F, false, null);
            COOKED_PINCH_BEETLE_LEG = food("cooked_pinch_beetle_leg", 6, 3.5F, false, null,
                    effect(MobEffects.NIGHT_VISION, 3600, 0, 1.0F));
            SLIME_BEETLE_LEG = food("slime_beetle_leg", 4, 1.5F, false, null);
            COOKED_SLIME_BEETLE_LEG = food("cooked_slime_beetle_leg", 6, 3.5F, false, null,
                    effect(MobEffects.NIGHT_VISION, 3600, 0, 1.0F));
            FIRE_BEETLE_LEG = food("fire_beetle_leg", 4, 1.5F, false, null);
            COOKED_FIRE_BEETLE_LEG = food("cooked_fire_beetle_leg", 6, 3.5F, false, null,
                    effect(MobEffects.NIGHT_VISION, 3600, 0, 1.0F));

            FIRE_BEETLE_FLAME_SAC = food("fire_beetle_flame_sac", 5, 3.0F, false, null,
                    effect(MobEffects.FIRE_RESISTANCE, 3600, 0, 1.0F));
            SLIME_BEETLE_HONEY_GLAND = food("slime_beetle_honey_gland", 5, 3.0F, false, null,
                    effect(MobEffects.REGENERATION, 300, 0, 1.0F));
            HERMIT_CRAB = new TSDItems.HelmetCrab(new Item.Properties());
            QUEST_RAM_MILK = drinkCureFood("quest_ram_milk", 0, 0.0F, net.minecraft.world.item.Items.GLASS_BOTTLE, 1, true);
            QUEST_RAM_CHEESE = cureFood("quest_ram_cheese", 6, 9.0F, net.minecraft.world.item.Items.GLASS_BOTTLE, 1, false);

            TWILIGHT_SKEWER = food("twilight_skewer", 12, 11.0F, false, null);
            LABYRINTH_FLAVOR_SKEWER = food("labyrinth_flavor_skewer", 14, 12.0F, false, null,
                    effect(Effects.CHARGE, 3600, 0, 1.0F),
                    effect(MobEffects.FIRE_RESISTANCE, 5400, 0, 1.0F));
            LABYRINTH_SASHIMI_MEDLEY = food("labyrinth_sashimi_medley", 16, 10.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(MobEffects.NIGHT_VISION, 14400, 0, 1.0F),
                    effect(ModEffects.NOURISHMENT, 3600, 0, 1.0F));
            LABYRINTH_A5_DOUBLE_CHEESEBURGER = cureFood(
                    "labyrinth_a5_mushroom_flavor_double_cheeseburger", 12, 18.6F, null, 3, false,
                    effect(Effects.CHARGE, 5400, 0, 1.0F));
            TORCHBERRY_SAUCE = food("torchberry_sauce", 3, 2.2F, false, net.minecraft.world.item.Items.BOWL);
            LABYRINTH_TACO = food("labyrinth_taco", 12, 14.4F, false, net.minecraft.world.item.Items.BOWL,
                    effect(Effects.CHARGE, 3600, 0, 1.0F));
            GELID_CRYSTAL = food("gelid_crystal", 1, 0.0F, false, null,
                    effect(twilightforest.init.TFMobEffects.FROSTY, 3600, 0, 1.0F));
            LABYRINTH_MUSHROOM = food("labyrinth_mushroom", 6, 7.2F, false, null,
                    effect(MobEffects.CONFUSION, 60, 0, 1.0F),
                    effect(Effects.CHARGE, 1200, 0, 1.0F));

            EXPERIMENT_PROTOTYPE = basic("experiment_prototype");
            EXPERIMENT_000 = food("experiment_000", 3, 1.8F, false, null,
                    effect(Effects.SYMBIOSIS, 1200, 0, 1.0F));
            EXPERIMENT_234 = food("experiment_234", 5, 0.0F, false, null,
                    effect(MobEffects.HUNGER, 1200, 0, 1.0F),
                    effect(MobEffects.CONFUSION, 1200, 0, 1.0F),
                    effect(Effects.SORROW, 3600, 0, 1.0F));
            ABYSS_PIE_SLICE = food("abyss_pie_slice", 8, 10.0F, false, null,
                    effect(Effects.SYMBIOSIS, 6000, 2, 1.0F),
                    effect(Effects.ABYSS_CALL, 1800, 0, 1.0F),
                    effect(MobEffects.NIGHT_VISION, 3600, 0, 1.0F),
                    effect(Effects.SORROW, 3600, 0, 1.0F));
            EXPERIMENT_250 = new TSDItems.Experiment250(new Item.Properties());
            FIERY_SLAG = basic("fiery_slag");
            GRIDDLE_TENTACLE = food("griddle_tentacle", 10, 12.5F, false, null,
                    effect(Effects.SYMBIOSIS, 2400, 0, 1.0F));
            SCOURGE_STEAK = food("scourge_steak", 18, 16.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.COMFORT, 3600, 0, 1.0F));
            REDCAP_SPICE = basic("redcap_spice");
            DRINK_ME = foodWithAnimation("drink_me", 4, 2.0F, false, net.minecraft.world.item.Items.GLASS_BOTTLE, UseAnim.DRINK,
                    32,
                    displayEffect(Effects.SHRINK, 3600, 0),
                    effect(MobEffects.REGENERATION, 600, 0, 1.0F));
            EAT_ME = food("eat_me", 5, 3.3F, false, null,
                    displayEffect(Effects.ENLARGE, 3600, 0));
            MINO_MINCE = food("mino_mince", 2, 1.2F, false, null);
            MINO_PATTY = food("mino_patty", 6, 6.2F, false, null,
                    effect(Effects.CHARGE, 600, 0, 1.0F));
            TRAIL_RATIONS = foodWithDuration("trail_rations", 12, 15.0F, false, null, 15,
                    effect(Effects.CHARGE, 2400, 0, 1.0F));
            BRACKEN = food("bracken", 3, 3.6F, false, null);
            PICKLED_BRACKEN = new TSDItems.PickledBracken(new Item.Properties()
                            .food(new net.minecraft.world.food.FoodProperties.Builder()
                                    .nutrition(7).saturationModifier(0.0F).build()));

            BOWL_OF_TWILIGHT_BORSCHT = food("bowl_of_twilight_borscht", 16, 23.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.COMFORT, 7800, 0, 1.0F),
                    effect(ModEffects.NOURISHMENT, 4800, 0, 1.0F),
                    effect(Effects.CHARGE, 5400, 0, 1.0F));
            TWILIGHT_BORSCHT_CUP = food("twilight_borscht_cup", 8, 11.5F, false, null,
                    effect(ModEffects.COMFORT, 5100, 0, 1.0F),
                    effect(ModEffects.NOURISHMENT, 3300, 0, 1.0F),
                    effect(Effects.CHARGE, 3600, 0, 1.0F));
            PLATE_OF_TWILIGHT_BOAR_KNUCKLE = food("plate_of_twilight_boar_knuckle", 18, 23.0F, false, null,
                    effect(ModEffects.NOURISHMENT, 6000, 0, 1.0F),
                    effect(Effects.CHARGE, 6000, 1, 1.0F));
            CREAM_OF_LABYRINTH_MUSHROOM_SOUP = cureFood("cream_of_labyrinth_mushroom_soup",
                    14, 21.0F, net.minecraft.world.item.Items.BOWL, 3, false,
                    effect(ModEffects.COMFORT, 6000, 0, 1.0F),
                    effect(Effects.CHARGE, 2400, 0, 1.0F));
            CREAM_OF_LABYRINTH_MUSHROOM_SOUP_CUP = cureFood("cream_of_labyrinth_mushroom_soup_cup",
                    7, 10.5F, null, 3, false,
                    effect(ModEffects.COMFORT, 3900, 0, 1.0F),
                    effect(Effects.CHARGE, 1500, 0, 1.0F));
            BOWL_OF_CHICKEN_AND_HYDRA_SOUP = food("bowl_of_chicken_and_hydra_soup", 15, 23.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.COMFORT, 6000, 0, 1.0F),
                    effect(MobEffects.MOVEMENT_SPEED, 3600, 1, 1.0F),
                    effect(MobEffects.DAMAGE_BOOST, 1800, 1, 1.0F));
            CHICKEN_AND_HYDRA_SOUP_CUP = food("chicken_and_hydra_soup_cup", 8, 11.0F, false, null,
                    effect(ModEffects.COMFORT, 3900, 0, 1.0F),
                    effect(MobEffects.MOVEMENT_SPEED, 2400, 1, 1.0F),
                    effect(MobEffects.DAMAGE_BOOST, 1200, 1, 1.0F));
            STIR_FRIED_BRACKEN = food("stir_fried_bracken", 17, 19.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(MobEffects.FIRE_RESISTANCE, 9600, 0, 1.0F));
            HELMET_CRAB_LEG_SUSHI_ROLL = food("helmet_crab_leg_sushi_roll", 12, 14.4F, false, null,
                    effect(MobEffects.DIG_SPEED, 5400, 1, 1.0F));
            HELMET_CRAB_LEG_SUSHI = food("helmet_crab_leg_sushi", 6, 6.0F, false, null,
                    effect(MobEffects.DIG_SPEED, 1800, 2, 1.0F));
            TENTACLE_CHOW_MEIN = food("tentacle_chow_mein", 16, 15.2F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.NOURISHMENT, 6000, 0, 1.0F),
                    effect(Effects.SYMBIOSIS, 6000, 1, 1.0F));
            MILLION_POUND_MEAL = food("million_pound_meal", 14, 19.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.NOURISHMENT, 3600, 0, 1.0F),
                    effect(Effects.CHARGE, 3600, 0, 1.0F));
            TWILIGHT_CHEESE_FONDUE_COMPANION =
                    new TSDItems.FondueCompanion(new Item.Properties());
            TWILIGHT_CHEESE_FONDUE_WITH_BREAD = cureFood("twilight_cheese_fondue_with_bread",
                    16, 21.0F, null, 3, false,
                    effect(ModEffects.NOURISHMENT, 6000, 0, 1.0F),
                    effect(Effects.CHARGE, 5400, 0, 1.0F),
                    effect(MobEffects.REGENERATION, 600, 0, 1.0F));
            TWILIGHT_SUPREME_SANDWICH = food("twilight_supreme_sandwich", 15, 16.0F, false, null);
            DOUBLE_CROWN_ICE_CREAM = new TSDItems.IceCream();
            TWIN_RADIANCE_ICE_POP = food("twin_radiance_ice_pop", 5, 3.2F, true, null,
                    effect(MobEffects.DAMAGE_RESISTANCE, 3600, 0, 1.0F),
                    effect(twilightforest.init.TFMobEffects.FROSTY, 3600, 0, 1.0F));
            SALT_ROASTED_HELMET_CRAB_CLAW = food("salt_roasted_helmet_crab_claw", 12, 16.6F, false, null,
                    effect(ModEffects.NOURISHMENT, 3600, 0, 1.0F),
                    effect(MobEffects.DIG_SPEED, 5400, 1, 1.0F));
            BOWL_OF_SALTED_CRAB_MEAT = food("bowl_of_salted_crab_meat", 16, 21.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.NOURISHMENT, 6000, 0, 1.0F),
                    effect(MobEffects.DAMAGE_BOOST, 5400, 0, 1.0F));
            BOWL_OF_NAGA_MIXED_RICE = food("bowl_of_naga_mixed_rice", 30, 30.0F, false, net.minecraft.world.item.Items.BOWL,
                    effect(ModEffects.NOURISHMENT, 5400, 0, 1.0F),
                    effect(Effects.CHARGE, 6000, 1, 1.0F),
                    effect(ModEffects.COMFORT, 6000, 0, 1.0F),
                    effect(MobEffects.DAMAGE_RESISTANCE, 2400, 1, 1.0F),
                    effect(MobEffects.MOVEMENT_SPEED, 2400, 2, 1.0F));
            NAGA_MIXED_RICE_CUP = food("naga_mixed_rice_cup", 15, 15.0F, false, null,
                    effect(ModEffects.NOURISHMENT, 3600, 0, 1.0F),
                    effect(Effects.CHARGE, 3900, 1, 1.0F),
                    effect(ModEffects.COMFORT, 3900, 0, 1.0F),
                    effect(MobEffects.DAMAGE_RESISTANCE, 1500, 1, 1.0F),
                    effect(MobEffects.MOVEMENT_SPEED, 1500, 2, 1.0F));

            GeminiRegistration.init(Items.class);
        }

        private static Item basic(String path) {
            return new Item(new Item.Properties());
        }

        private static Item food(String path, int nutrition, float saturation, boolean alwaysEdible,
                                 Item container, EffectSpec... effects) {
            return foodWithAnimation(path, nutrition, saturation, alwaysEdible, container, UseAnim.EAT,
                    32, effects);
        }

        private static Item foodWithDuration(String path, int nutrition, float saturation,
                                             boolean alwaysEdible, Item container, int useDuration,
                                             EffectSpec... effects) {
            return foodWithAnimation(path, nutrition, saturation, alwaysEdible, container, UseAnim.EAT,
                    useDuration, effects);
        }

        private static Item drinkCureFood(String path, int nutrition, float saturation, Item container,
                                          int maximumCures, boolean alwaysEdible, EffectSpec... effects) {
            return cureFoodWithAnimation(path, nutrition, saturation, container, maximumCures, alwaysEdible,
                    UseAnim.DRINK, effects);
        }

        private static Item cureFood(String path, int nutrition, float saturation, Item container,
                                     int maximumCures, boolean alwaysEdible, EffectSpec... effects) {
            return cureFoodWithAnimation(path, nutrition, saturation, container, maximumCures, alwaysEdible,
                    UseAnim.EAT, effects);
        }

        private static Item cureFoodWithAnimation(String path, int nutrition, float saturation, Item container,
                                                  int maximumCures, boolean alwaysEdible, UseAnim animation,
                                                  EffectSpec... effects) {
            return buildConsumable(path, nutrition, saturation, alwaysEdible, container, animation, 32,
                    maximumCures, effects);
        }

        private static Item foodWithAnimation(String path, int nutrition, float saturation,
                                              boolean alwaysEdible, Item container, UseAnim animation,
                                              int useDuration, EffectSpec... effects) {
            return buildConsumable(path, nutrition, saturation, alwaysEdible, container, animation,
                    useDuration, 0, effects);
        }

        private static Item buildConsumable(String path, int nutrition, float saturation,
                                            boolean alwaysEdible, Item container, UseAnim animation,
                                            int useDuration, int maximumCures, EffectSpec... effects) {
            var builder = new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(nutrition)
                    .saturationModifier(nutrition == 0 ? 0.0F : saturation / (nutrition * 2.0F));
            if (alwaysEdible) {
                builder.alwaysEdible();
            }
            for (EffectSpec effect : effects) {
                if (effect.displayOnly()) {
                    continue;
                }
                // Farmer's Delight still lists the effect, but the instance
                // itself must not spawn a second particle cloud.
                builder.effect(new MobEffectInstance(effect.effect(), effect.duration(),
                        effect.amplifier(), false, false, true), effect.chance());
            }
            Item.Properties properties = new Item.Properties().food(builder.build());
            if (container != null) {
                properties.craftRemainder(container);
            }
            TSDItems.Consumable item = maximumCures > 0
                    ? new TSDItems.MultiCure(properties, maximumCures, animation, useDuration)
                    : new TSDItems.Consumable(properties, animation, useDuration);
            for (EffectSpec effect : effects) {
                if (effect.displayOnly()) {
                    item.addDisplayEffect(() -> new MobEffectInstance(effect.effect(), effect.duration(),
                            effect.amplifier(), false, false, true));
                }
            }
            return item;
        }

        private Items() {
        }
    }

    // ------------------------------------------------------------------
    // Block entities
    // ------------------------------------------------------------------

    @RegistryNamespace(TwilightSparksDelight.MOD_ID)
    public static final class BlockEntities implements BlockEntityRegistryContainer {
        public static BlockEntityType<TSDBlockEntities.LargeFeast> LARGE_FEAST;
        public static BlockEntityType<TSDBlockEntities.SharingFeast> SHARING_FEAST;
        public static BlockEntityType<TSDBlockEntities.UnripeJar> UNRIPE_PICKLED_BRACKEN_JAR;
        public static BlockEntityType<TSDBlockEntities.GiantStove> GIANT_STOVE;
        public static BlockEntityType<TSDBlockEntities.GiantPot> GIANT_COOKING_POT;
        public static BlockEntityType<TSDBlockEntities.GloryCrucible> GLORY_CRUCIBLE;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            LARGE_FEAST = BlockEntityType.Builder.of(TSDBlockEntities.LargeFeast::new,
                    Blocks.NAGA_MIXED_RICE, Blocks.TWILIGHT_BOAR_KNUCKLE).build(null);
            SHARING_FEAST = BlockEntityType.Builder.of(TSDBlockEntities.SharingFeast::new,
                    Blocks.SALT_HELMET_CRAB).build(null);
            UNRIPE_PICKLED_BRACKEN_JAR = BlockEntityType.Builder.of(TSDBlockEntities.UnripeJar::new,
                    Blocks.UNRIPE_PICKLED_BRACKEN_JAR).build(null);
            GIANT_STOVE = BlockEntityType.Builder.of(TSDBlockEntities.GiantStove::new,
                    Blocks.GIANT_STOVE, Blocks.GIANTS_STOVE).build(null);
            GIANT_COOKING_POT = BlockEntityType.Builder.of(TSDBlockEntities.GiantPot::new,
                    Blocks.GIANT_COOKING_POT, Blocks.GIANTS_COOKING_POT).build(null);
            GLORY_CRUCIBLE = BlockEntityType.Builder.of(TSDBlockEntities.GloryCrucible::new,
                    Blocks.GLORY_CRUCIBLE, Blocks.TWILIGHT_BORSCHT).build(null);
            GeminiRegistration.init(BlockEntities.class);
            ItemStorage.SIDED.registerForBlockEntity(
                    (pot, side) -> pot.getStorage(side), GIANT_COOKING_POT);
        }

        private BlockEntities() {
        }
    }

    // ------------------------------------------------------------------
    // Damage types
    // ------------------------------------------------------------------

    public static final class DamageTypes {
        public static final ResourceKey<DamageType> CRAB_BITE = ResourceKey.create(
                Registries.DAMAGE_TYPE, id("crab_bite"));
        public static final ResourceKey<DamageType> LIFEDRAIN_PEDESTAL = ResourceKey.create(
                Registries.DAMAGE_TYPE, id("lifedrain_pedestal"));

        private DamageTypes() {
        }
    }

    // ------------------------------------------------------------------
    // Creative tab
    // ------------------------------------------------------------------

    public static final class CreativeTabs {
        public static ResourceKey<CreativeModeTab> TWILIGHT_SPARKS_DELIGHT;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            TWILIGHT_SPARKS_DELIGHT = GeminiCreativeTabs.register(
                    id("twilight_sparks_delight"),
                    Component.translatable("itemGroup.twilightsparksdelight"),
                    () -> new ItemStack(Items.HERMIT_CRAB),
                    output -> {
                        for (Item item : BuiltInRegistries.ITEM) {
                            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                            if (!TwilightSparksDelight.MOD_ID.equals(key.getNamespace())) {
                                continue;
                            }
                            if (item == Items.EXPERIMENT_250) {
                                output.accept(TSDItems.Experiment250.createStack(item, 1, 0));
                            } else if (item == Items.TWILIGHT_CHEESE_FONDUE_COMPANION) {
                                output.accept(TSDItems.withFullDurability(new ItemStack(item)));
                            } else {
                                output.accept(item);
                            }
                        }
                        output.accept(twilightforest.init.TFItems.POCKET_WATCH.get());
                        output.accept(twilightforest.init.TFBlocks.MASON_JAR.get());
                    });
        }

        private CreativeTabs() {
        }
    }
}

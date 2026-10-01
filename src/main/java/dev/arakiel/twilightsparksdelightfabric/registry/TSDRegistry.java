package dev.arakiel.twilightsparksdelightfabric.registry;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import dev.arakiel.twilightsparksdelightfabric.common.block.ContainerBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.block.FeastBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.block.GloryCrucibleBlock;
import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.common.effect.TSDMobEffects;
import dev.arakiel.twilightsparksdelightfabric.common.entity.ThrownPickledBracken;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.common.recipe.TSDCustomRecipes;
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
        return TwilightSparksDelightFabric.id(path);
    }

    private static <V, T extends V> T register(Registry<V> registry, String path, T value) {
        return Registry.register(registry, id(path), value);
    }

    // ------------------------------------------------------------------
    // Mob effects
    // ------------------------------------------------------------------

    public static final class Effects {
        public static Holder<MobEffect> ENLARGE;
        public static Holder<MobEffect> SHRINK;
        public static Holder<MobEffect> CHARGE;
        public static Holder<MobEffect> SYMBIOSIS;
        public static Holder<MobEffect> GRIEF;
        public static Holder<MobEffect> SORROW;
        public static Holder<MobEffect> ABYSS_CALL;

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
        }

        private static Holder<MobEffect> effect(String path, MobEffectCategory category, int color) {
            return effect(path, category, color, TSDMobEffects.Behavior.NONE);
        }

        private static Holder<MobEffect> effect(String path, MobEffectCategory category, int color,
                                                TSDMobEffects.Behavior behavior) {
            return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id(path),
                    new TSDMobEffects.Effect(category, color, behavior));
        }

        private Effects() {
        }
    }

    // ------------------------------------------------------------------
    // Data components
    // ------------------------------------------------------------------

    public static final class Components {
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
        }

        private static <T> DataComponentType<T> component(String path, Codec<T> codec) {
            return register(BuiltInRegistries.DATA_COMPONENT_TYPE, path,
                    DataComponentType.<T>builder().persistent(codec).build());
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
    public static final class Fluids {
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
            FIERY_BLOOD = register(BuiltInRegistries.FLUID, "fiery_blood",
                    new SimpleFluid(true));
            FLOWING_FIERY_BLOOD = register(BuiltInRegistries.FLUID, "flowing_fiery_blood",
                    new SimpleFluid(false));
            FIERY_TEARS = register(BuiltInRegistries.FLUID, "fiery_tears",
                    new SimpleFluid(true));
            FLOWING_FIERY_TEARS = register(BuiltInRegistries.FLUID, "flowing_fiery_tears",
                    new SimpleFluid(false));
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
     * <p>The NeoForge edition used {@code BaseFlowingFluid} with a
     * {@code FluidType} for the same purpose. Those fluids are never placed in
     * the world, so only the source lookups have to be meaningful.</p>
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

    public static final class Entities {
        public static EntityType<ThrownPickledBracken> PICKLED_BRACKEN;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            PICKLED_BRACKEN = register(BuiltInRegistries.ENTITY_TYPE, "pickled_bracken",
                    EntityType.Builder.<ThrownPickledBracken>of(ThrownPickledBracken::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build("twilightsparksdelightfabric:pickled_bracken"));
        }

        private Entities() {
        }
    }

    // ------------------------------------------------------------------
    // Recipe serializers
    // ------------------------------------------------------------------

    public static final class Serials {
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
            EXPERIMENT_250_REPLICATION = register(BuiltInRegistries.RECIPE_SERIALIZER,
                    "experiment_250_replication", new TSDCustomRecipes.Experiment250Replication.Serializer());
            REUSABLE_WATER_SHAPELESS = register(BuiltInRegistries.RECIPE_SERIALIZER,
                    "reusable_water_shapeless", new TSDCustomRecipes.ReusableWaterShapeless.Serializer());
            KEEPING_ITEM_SHAPELESS = register(BuiltInRegistries.RECIPE_SERIALIZER,
                    "keeping_item_shapeless", new TSDCustomRecipes.KeepingItemShapeless.Serializer());
            HELMET_CRAB_CUTTING = register(BuiltInRegistries.RECIPE_SERIALIZER,
                    "helmet_crab_cutting", new TSDCustomRecipes.HelmetCrabCutting.Serializer());
        }

        private Serials() {
        }
    }

    // ------------------------------------------------------------------
    // Advancement triggers
    // ------------------------------------------------------------------

    public static final class Triggers {
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
        }

        private static Trigger trigger(String path) {
            return register(BuiltInRegistries.TRIGGER_TYPES, path, new Trigger());
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

    public static final class Blocks {
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
        public static FeastBlocks.StructurePart NAGA_MIXED_RICE_PART;
        public static FeastBlocks.NagaRice NAGA_MIXED_RICE;
        public static FeastBlocks.AbyssPie ABYSS_PIE;
        public static FeastBlocks.StructurePart TWILIGHT_BOAR_KNUCKLE_PART;
        public static FeastBlocks.BoarKnuckle TWILIGHT_BOAR_KNUCKLE;
        public static KitchenBlocks.GiantStove GIANT_STOVE;
        public static KitchenBlocks.KitchenPart GIANT_STOVE_PART;
        public static KitchenBlocks.GiantPot GIANT_COOKING_POT;
        public static KitchenBlocks.KitchenPart GIANT_COOKING_POT_PART;
        public static KitchenBlocks.GiantStove GIANTS_STOVE;
        public static KitchenBlocks.KitchenPart GIANTS_STOVE_PART;
        public static KitchenBlocks.GiantPot GIANTS_COOKING_POT;
        public static KitchenBlocks.KitchenPart GIANTS_COOKING_POT_PART;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;

            TWILIGHT_CHEESE_FONDUE = registerBlock("twilight_cheese_fondue",
                    () -> new FeastBlocks.CheeseFondue(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE).strength(1.5F)), true, 1);
            SALT_HELMET_CRAB = registerBlock("salt_helmet_crab",
                    () -> new ContainerBlocks.SaltCrab(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN).strength(1.5F)), true, 1);
            LABYRINTH_MUSHROOM_COLONY = registerColony("labyrinth_mushroom_colony",
                    MapColor.COLOR_RED, () -> Items.LABYRINTH_MUSHROOM);
            TWILIGHT_BRACKEN_COLONY = registerColony("twilight_bracken_colony",
                    MapColor.COLOR_GREEN, () -> Items.BRACKEN);
            UNRIPE_PICKLED_BRACKEN_JAR = registerBlock("unripe_pickled_bracken_jar",
                    () -> new ContainerBlocks.UnripeJar(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN).strength(0.5F)), true, 1);
            PICKLED_BRACKEN_JAR = registerBlock("pickled_bracken_jar",
                    () -> new ContainerBlocks.PickledJar(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN).strength(0.5F)), true, 1);
            GLORY_CRUCIBLE = registerBlock("glory_crucible",
                    () -> new GloryCrucibleBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F)), true, 64);
            TWILIGHT_BORSCHT = registerBlock("twilight_borscht",
                    () -> new FeastBlocks.Borscht(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED).strength(1.5F)), true, 1);
            LABYRINTH_MUSHROOM_CRATE = registerBlock("labyrinth_mushroom_crate",
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(1.5F).sound(SoundType.WOOD)), true, 64);
            BRACKEN_CRATE = registerBlock("bracken_crate",
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(1.5F).sound(SoundType.WOOD)), true, 64);

            NAGA_MIXED_RICE_PART = registerBlock("naga_mixed_rice_part",
                    () -> new FeastBlocks.StructurePart(BlockBehaviour.Properties.of().strength(1.0F),
                            () -> NAGA_MIXED_RICE), false, 1);
            NAGA_MIXED_RICE = registerBlock("naga_mixed_rice",
                    () -> new FeastBlocks.NagaRice(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN).strength(1.5F),
                            () -> Items.BOWL_OF_NAGA_MIXED_RICE, () -> NAGA_MIXED_RICE_PART), true, 1);
            ABYSS_PIE = registerBlock("abyss_pie",
                    () -> new FeastBlocks.AbyssPie(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).strength(1.5F)), true, 1);
            TWILIGHT_BOAR_KNUCKLE_PART = registerBlock("twilight_boar_knuckle_part",
                    () -> new FeastBlocks.StructurePart(BlockBehaviour.Properties.of().strength(1.0F),
                            () -> TWILIGHT_BOAR_KNUCKLE), false, 1);
            TWILIGHT_BOAR_KNUCKLE = registerBlock("twilight_boar_knuckle",
                    () -> new FeastBlocks.BoarKnuckle(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN).strength(1.5F),
                            () -> TWILIGHT_BOAR_KNUCKLE_PART), true, 1);

            GIANT_STOVE = registerBlock("giant_stove",
                    () -> new KitchenBlocks.GiantStove(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANT_STOVE_PART, 2), true, 1);
            GIANT_STOVE_PART = registerBlock("giant_stove_part",
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANT_STOVE,
                            KitchenBlocks.KitchenPart.Kind.STOVE), false, 1);
            GIANT_COOKING_POT = registerBlock("giant_cooking_pot",
                    () -> new KitchenBlocks.GiantPot(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANT_COOKING_POT_PART, 2),
                    true, 1);
            GIANT_COOKING_POT_PART = registerBlock("giant_cooking_pot_part",
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANT_COOKING_POT,
                            KitchenBlocks.KitchenPart.Kind.COOKING_POT), false, 1);
            GIANTS_STOVE = registerBlock("giants_stove",
                    () -> new KitchenBlocks.GiantStove(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANTS_STOVE_PART, 4), true, 1);
            GIANTS_STOVE_PART = registerBlock("giants_stove_part",
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE).strength(1.5F), () -> GIANTS_STOVE,
                            KitchenBlocks.KitchenPart.Kind.STOVE), false, 1);
            GIANTS_COOKING_POT = registerBlock("giants_cooking_pot",
                    () -> new KitchenBlocks.GiantPot(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANTS_COOKING_POT_PART, 4),
                    false, 1);
            GIANTS_COOKING_POT_PART = registerBlock("giants_cooking_pot_part",
                    () -> new KitchenBlocks.KitchenPart(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(1.5F), () -> GIANTS_COOKING_POT,
                            KitchenBlocks.KitchenPart.Kind.COOKING_POT), false, 1);

            Registry.register(BuiltInRegistries.ITEM, id("giants_cooking_pot"),
                    new KitchenBlocks.GiantPotItem(new Item.Properties().stacksTo(1)));
        }

        private static <B extends Block> B registerBlock(String path, Supplier<B> supplier,
                                                         boolean blockItem, int stackSize) {
            B block = Registry.register(BuiltInRegistries.BLOCK, id(path), supplier.get());
            if (blockItem) {
                Registry.register(BuiltInRegistries.ITEM, id(path),
                        new BlockItem(block, new Item.Properties().stacksTo(stackSize)));
            }
            return block;
        }

        /**
         * Colonies place fully grown, so they need the dedicated block item
         * instead of a plain one.
         */
        private static ContainerBlocks.Colony registerColony(String path, MapColor color,
                                                             Supplier<Item> harvestItem) {
            ContainerBlocks.Colony colony = Registry.register(BuiltInRegistries.BLOCK, id(path),
                    new ContainerBlocks.Colony(BlockBehaviour.Properties.of().mapColor(color),
                            harvestItem));
            Registry.register(BuiltInRegistries.ITEM, id(path),
                    new ContainerBlocks.ColonyItem(colony, new Item.Properties()));
            return colony;
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

    private static EffectSpec displayEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new EffectSpec(effect, duration, amplifier, 1.0F, true);
    }

    public static final class Items {
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
            HERMIT_CRAB = register(BuiltInRegistries.ITEM, "hermit_crab",
                    new TSDItems.HelmetCrab(new Item.Properties()));
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
            EXPERIMENT_250 = register(BuiltInRegistries.ITEM, "experiment_250",
                    new TSDItems.Experiment250(new Item.Properties()));
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
            PICKLED_BRACKEN = register(BuiltInRegistries.ITEM, "pickled_bracken",
                    new TSDItems.PickledBracken(new Item.Properties()
                            .food(new net.minecraft.world.food.FoodProperties.Builder()
                                    .nutrition(7).saturationModifier(0.0F).build())));

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
            TWILIGHT_CHEESE_FONDUE_COMPANION = register(BuiltInRegistries.ITEM,
                    "twilight_cheese_fondue_companion",
                    new TSDItems.FondueCompanion(new Item.Properties()));
            TWILIGHT_CHEESE_FONDUE_WITH_BREAD = cureFood("twilight_cheese_fondue_with_bread",
                    16, 21.0F, null, 3, false,
                    effect(ModEffects.NOURISHMENT, 6000, 0, 1.0F),
                    effect(Effects.CHARGE, 5400, 0, 1.0F),
                    effect(MobEffects.REGENERATION, 600, 0, 1.0F));
            TWILIGHT_SUPREME_SANDWICH = food("twilight_supreme_sandwich", 15, 16.0F, false, null);
            DOUBLE_CROWN_ICE_CREAM = register(BuiltInRegistries.ITEM, "double_crown_ice_cream",
                    new TSDItems.IceCream());
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
        }

        private static Item basic(String path) {
            return register(BuiltInRegistries.ITEM, path, new Item(new Item.Properties()));
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
            return register(BuiltInRegistries.ITEM, path,
                    buildConsumable(path, nutrition, saturation, alwaysEdible, container, animation, 32,
                            maximumCures, effects));
        }

        private static Item foodWithAnimation(String path, int nutrition, float saturation,
                                              boolean alwaysEdible, Item container, UseAnim animation,
                                              int useDuration, EffectSpec... effects) {
            return register(BuiltInRegistries.ITEM, path,
                    buildConsumable(path, nutrition, saturation, alwaysEdible, container, animation,
                            useDuration, 0, effects));
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

    public static final class BlockEntities {
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
            LARGE_FEAST = register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "large_feast",
                    BlockEntityType.Builder.of(TSDBlockEntities.LargeFeast::new,
                            Blocks.NAGA_MIXED_RICE, Blocks.TWILIGHT_BOAR_KNUCKLE).build(null));
            SHARING_FEAST = register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "sharing_feast",
                    BlockEntityType.Builder.of(TSDBlockEntities.SharingFeast::new,
                            Blocks.SALT_HELMET_CRAB).build(null));
            UNRIPE_PICKLED_BRACKEN_JAR = register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    "unripe_pickled_bracken_jar",
                    BlockEntityType.Builder.of(TSDBlockEntities.UnripeJar::new,
                            Blocks.UNRIPE_PICKLED_BRACKEN_JAR).build(null));
            GIANT_STOVE = register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "giant_stove",
                    BlockEntityType.Builder.of(TSDBlockEntities.GiantStove::new,
                            Blocks.GIANT_STOVE, Blocks.GIANTS_STOVE).build(null));
            GIANT_COOKING_POT = register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "giant_cooking_pot",
                    BlockEntityType.Builder.of(TSDBlockEntities.GiantPot::new,
                            Blocks.GIANT_COOKING_POT, Blocks.GIANTS_COOKING_POT).build(null));
            /*
             * Farmer's Delight exposes its pot inventory through the transfer API
             * for its own block entity type only. The giant pots carry their own
             * type, so the same lookup is registered here; without it hoppers and
             * pipes could not reach them, while the original NeoForge version
             * registered an item handler capability for exactly this type.
             */
            ItemStorage.SIDED.registerForBlockEntity(
                    (pot, side) -> pot.getStorage(side), GIANT_COOKING_POT);
            GLORY_CRUCIBLE = register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "glory_crucible",
                    BlockEntityType.Builder.of(TSDBlockEntities.GloryCrucible::new,
                            Blocks.GLORY_CRUCIBLE, Blocks.TWILIGHT_BORSCHT).build(null));
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
        public static CreativeModeTab TWILIGHT_SPARKS_DELIGHT;

        private static boolean initialized;

        static void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            TWILIGHT_SPARKS_DELIGHT = register(BuiltInRegistries.CREATIVE_MODE_TAB,
                    "twilight_sparks_delight",
                    net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
                            .title(Component.translatable("itemGroup.twilightsparksdelightfabric"))
                            .icon(() -> new ItemStack(Items.HERMIT_CRAB))
                            .displayItems((parameters, output) -> {
                                output.accept(Items.LIVEROOT_FLOUR);
                                output.accept(Items.LIVEROOT_PIE_CRUST);
                                output.accept(Items.LIVEROOT_DOUGH);
                                output.accept(Items.LIVEROOT_BREAD);
                                output.accept(Items.RAW_WILD_BOAR_MEAT);
                                output.accept(Items.RAW_WILD_BOAR_MEAT_CUBES);
                                output.accept(Items.COOKED_WILD_BOAR_MEAT);
                                output.accept(Items.COOKED_WILD_BOAR_MEAT_CUBES);
                                output.accept(Items.RAW_BIGHORN_MUTTON);
                                output.accept(Items.RAW_BIGHORN_MUTTON_CHOP);
                                output.accept(Items.COOKED_BIGHORN_MUTTON);
                                output.accept(Items.COOKED_BIGHORN_MUTTON_CHOP);
                                output.accept(Items.HERMIT_CRAB);
                                output.accept(Items.HERMIT_CRAB_LEG);
                                output.accept(Items.COOKED_HERMIT_CRAB_LEG);
                                output.accept(Items.PINCH_BEETLE_LEG);
                                output.accept(Items.COOKED_PINCH_BEETLE_LEG);
                                output.accept(Items.SLIME_BEETLE_LEG);
                                output.accept(Items.COOKED_SLIME_BEETLE_LEG);
                                output.accept(Items.SLIME_BEETLE_HONEY_GLAND);
                                output.accept(Items.FIRE_BEETLE_LEG);
                                output.accept(Items.COOKED_FIRE_BEETLE_LEG);
                                output.accept(Items.FIRE_BEETLE_FLAME_SAC);
                                output.accept(Items.GELID_CRYSTAL);
                                output.accept(Items.LABYRINTH_MUSHROOM);
                                output.accept(Blocks.LABYRINTH_MUSHROOM_COLONY);
                                output.accept(Blocks.LABYRINTH_MUSHROOM_CRATE);
                                output.accept(Items.EXPERIMENT_PROTOTYPE);
                                output.accept(Items.EXPERIMENT_000);
                                output.accept(Items.EXPERIMENT_234);
                                output.accept(TSDItems.Experiment250.createStack(Items.EXPERIMENT_250, 1, 0));
                                output.accept(Items.GRIDDLE_TENTACLE);
                                output.accept(Items.FIERY_SLAG);
                                output.accept(twilightforest.init.TFItems.POCKET_WATCH.get());
                                output.accept(Items.REDCAP_SPICE);
                                output.accept(Items.MINO_MINCE);
                                output.accept(Items.MINO_PATTY);
                                output.accept(Items.BRACKEN);
                                output.accept(Blocks.TWILIGHT_BRACKEN_COLONY);
                                output.accept(Items.PICKLED_BRACKEN);
                                output.accept(Blocks.BRACKEN_CRATE);
                                output.accept(Items.QUEST_RAM_MILK);
                                output.accept(Items.QUEST_RAM_CHEESE);
                                output.accept(Items.TORCHBERRY_SAUCE);
                                output.accept(Items.DRINK_ME);
                                output.accept(Items.EAT_ME);
                                output.accept(Items.TWILIGHT_SKEWER);
                                output.accept(Items.LABYRINTH_FLAVOR_SKEWER);
                                output.accept(Items.LABYRINTH_SASHIMI_MEDLEY);
                                output.accept(Items.LABYRINTH_A5_DOUBLE_CHEESEBURGER);
                                output.accept(Items.LABYRINTH_TACO);
                                output.accept(Items.TWILIGHT_SUPREME_SANDWICH);
                                output.accept(Items.SCOURGE_STEAK);
                                output.accept(Items.TRAIL_RATIONS);
                                output.accept(Items.DOUBLE_CROWN_ICE_CREAM);
                                output.accept(Items.TWIN_RADIANCE_ICE_POP);
                                output.accept(Items.CREAM_OF_LABYRINTH_MUSHROOM_SOUP);
                                output.accept(Items.CREAM_OF_LABYRINTH_MUSHROOM_SOUP_CUP);
                                output.accept(Items.BOWL_OF_CHICKEN_AND_HYDRA_SOUP);
                                output.accept(Items.CHICKEN_AND_HYDRA_SOUP_CUP);
                                output.accept(Items.STIR_FRIED_BRACKEN);
                                output.accept(Items.HELMET_CRAB_LEG_SUSHI_ROLL);
                                output.accept(Items.HELMET_CRAB_LEG_SUSHI);
                                output.accept(Items.TENTACLE_CHOW_MEIN);
                                output.accept(Items.MILLION_POUND_MEAL);
                                output.accept(Blocks.TWILIGHT_CHEESE_FONDUE);
                                output.accept(TSDItems.withFullDurability(
                                        new ItemStack(Items.TWILIGHT_CHEESE_FONDUE_COMPANION)));
                                output.accept(Items.TWILIGHT_CHEESE_FONDUE_WITH_BREAD);
                                output.accept(Blocks.SALT_HELMET_CRAB);
                                output.accept(Items.BOWL_OF_SALTED_CRAB_MEAT);
                                output.accept(Items.SALT_ROASTED_HELMET_CRAB_CLAW);
                                output.accept(Blocks.TWILIGHT_BORSCHT);
                                output.accept(Items.BOWL_OF_TWILIGHT_BORSCHT);
                                output.accept(Items.TWILIGHT_BORSCHT_CUP);
                                output.accept(Blocks.TWILIGHT_BOAR_KNUCKLE);
                                output.accept(Items.PLATE_OF_TWILIGHT_BOAR_KNUCKLE);
                                output.accept(Blocks.NAGA_MIXED_RICE);
                                output.accept(Items.BOWL_OF_NAGA_MIXED_RICE);
                                output.accept(Items.NAGA_MIXED_RICE_CUP);
                                output.accept(Blocks.ABYSS_PIE);
                                output.accept(Items.ABYSS_PIE_SLICE);
                                output.accept(Blocks.UNRIPE_PICKLED_BRACKEN_JAR);
                                output.accept(Blocks.PICKLED_BRACKEN_JAR);
                                output.accept(twilightforest.init.TFBlocks.MASON_JAR.get());
                                output.accept(Blocks.GLORY_CRUCIBLE);
                                output.accept(Blocks.GIANT_STOVE);
                                output.accept(Blocks.GIANT_COOKING_POT);
                                output.accept(Blocks.GIANTS_STOVE);
                                output.accept(Blocks.GIANTS_COOKING_POT);
                            })
                            .build());
        }

        private CreativeTabs() {
        }
    }

}

package dev.arakiel.twilightsparksdelightfabric;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * Configuration of the mod.
 *
 * <p>The NeoForge edition declared every option through {@code ModConfigSpec}.
 * Fabric has no built in config API, so the same options are declared here and
 * persisted as a single JSON file inside the {@code config} directory. Every
 * option keeps the original name, default value and range.</p>
 */
public final class TSDConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Option<?>> OPTIONS = new ArrayList<>();
    private static boolean loaded;

    // ------------------------------------------------------------------
    // Option plumbing
    // ------------------------------------------------------------------

    /** Base type of every option; {@code get()} mirrors the NeoForge accessor. */
    public static class Value<T> {
        private final String key;
        private final T defaultValue;
        // Package private on purpose: the option wrappers below write it.
        T value;

        private Value(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        private String key() {
            return key;
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(String key, boolean value) {
            super(key, value);
        }
    }

    public static final class IntValue extends Value<Integer> {
        private IntValue(String key, int value) {
            super(key, value);
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(String key, double value) {
            super(key, value);
        }
    }

    public static final class StringValue extends Value<String> {
        private StringValue(String key, String value) {
            super(key, value);
        }
    }

    public static final class ListValue extends Value<List<? extends String>> {
        private ListValue(String key, List<String> value) {
            super(key, value);
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        private EnumValue(String key, E value) {
            super(key, value);
        }
    }

    private interface Option<T> {
        String key();

        void read(JsonElement element);

        JsonElement write();
    }

    private static <T, V extends Value<T>> V register(V value, Option<T> option) {
        OPTIONS.add(option);
        return value;
    }

    private static BooleanValue bool(String key, boolean value) {
        BooleanValue option = new BooleanValue(key, value);
        return register(option, new Option<Boolean>() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void read(JsonElement element) {
                if (element.isJsonPrimitive()) {
                    option.value = element.getAsBoolean();
                }
            }

            @Override
            public JsonElement write() {
                return new JsonPrimitive(option.value);
            }
        });
    }

    private static IntValue integer(String key, int value, int min, int max) {
        IntValue option = new IntValue(key, value);
        return register(option, new Option<Integer>() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void read(JsonElement element) {
                if (element.isJsonPrimitive()) {
                    option.value = Math.max(min, Math.min(max, element.getAsInt()));
                }
            }

            @Override
            public JsonElement write() {
                return new JsonPrimitive(option.value);
            }
        });
    }

    private static DoubleValue number(String key, double value, double min, double max) {
        DoubleValue option = new DoubleValue(key, value);
        return register(option, new Option<Double>() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void read(JsonElement element) {
                if (element.isJsonPrimitive()) {
                    double read = element.getAsDouble();
                    if (Double.isFinite(read)) {
                        option.value = Math.max(min, Math.min(max, read));
                    }
                }
            }

            @Override
            public JsonElement write() {
                return new JsonPrimitive(option.value);
            }
        });
    }

    private static StringValue text(String key, String value) {
        StringValue option = new StringValue(key, value);
        return register(option, new Option<String>() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void read(JsonElement element) {
                if (element.isJsonPrimitive()) {
                    option.value = element.getAsString();
                }
            }

            @Override
            public JsonElement write() {
                return new JsonPrimitive(option.value);
            }
        });
    }

    private static ListValue strings(String key, List<String> value) {
        ListValue option = new ListValue(key, value);
        return register(option, new Option<List<? extends String>>() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void read(JsonElement element) {
                if (!element.isJsonArray()) {
                    return;
                }
                List<String> read = new ArrayList<>();
                for (JsonElement child : element.getAsJsonArray()) {
                    if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                        read.add(child.getAsString());
                    }
                }
                option.value = read;
            }

            @Override
            public JsonElement write() {
                JsonArray array = new JsonArray();
                for (String entry : option.value) {
                    array.add(entry);
                }
                return array;
            }
        });
    }

    private static <E extends Enum<E>> EnumValue<E> enumeration(String key, E value) {
        EnumValue<E> option = new EnumValue<>(key, value);
        Class<E> type = value.getDeclaringClass();
        return register(option, new Option<E>() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void read(JsonElement element) {
                if (!element.isJsonPrimitive()) {
                    return;
                }
                String name = element.getAsString();
                for (E candidate : type.getEnumConstants()) {
                    if (candidate.name().equalsIgnoreCase(name)) {
                        option.value = candidate;
                        return;
                    }
                }
            }

            @Override
            public JsonElement write() {
                return new JsonPrimitive(option.value.name());
            }
        });
    }

    // ------------------------------------------------------------------
    // Options
    // ------------------------------------------------------------------

    public static final ListValue RANDOM_CURE_EFFECT_BLACKLIST =
            strings("randomCureEffectBlacklist", List.of());
    public static final StringValue CONFIG_VERSION = text("configVersion", "1");
    public static final BooleanValue EXTENDED_FOOD_STATS_ENABLED = bool("extendedFoodStatsEnabled", true);
    public static final BooleanValue EXTENDED_FOOD_PROGRESSIVE_CAPS_ENABLED =
            bool("extendedFoodProgressiveCapsEnabled", true);
    public static final BooleanValue EXTENDED_FOOD_KEEP_VANILLA_RESPAWN =
            bool("extendedFoodKeepVanillaRespawn", true);
    public static final BooleanValue EXTENDED_FOOD_EXTRA_CONSUMPTION_ENABLED =
            bool("extendedFoodExtraConsumptionEnabled", true);
    public static final DoubleValue EXTENDED_FOOD_EXTRA_CONSUMPTION_MULTIPLIER =
            number("extendedFoodExtraConsumptionMultiplier", 0.5D, 0.0D, 10.0D);
    public static final DoubleValue EXTENDED_FOOD_EXTRA_BENEFIT_MULTIPLIER =
            number("extendedFoodExtraBenefitMultiplier", 0.25D, 0.0D, 10.0D);

    public enum ExperimentWorkstation {
        CRAFTING_TABLE, CUTTING_BOARD, COOKING_POT
    }

    public static final BooleanValue EXPERIMENT_BINDING_MODE_ENABLED =
            bool("experimentBindingModeEnabled", false);
    public static final EnumValue<ExperimentWorkstation> EXPERIMENT_WORKSTATION =
            enumeration("experimentWorkstation", ExperimentWorkstation.CUTTING_BOARD);
    public static final ListValue EXPERIMENT_ENTITY_MEAT_BINDINGS = strings("experimentEntityMeatBindings",
            List.of(
                    "minecraft:pig=minecraft:porkchop",
                    "minecraft:cow=minecraft:beef",
                    "minecraft:chicken=minecraft:chicken",
                    "minecraft:sheep=minecraft:mutton",
                    "minecraft:rabbit=minecraft:rabbit",
                    "minecraft:spider=minecraft:spider_eye",
                    "minecraft:zombie=minecraft:rotten_flesh",
                    "twilightforest:wild_boar=twilightsparksdelightfabric:raw_wild_boar_meat",
                    "twilightforest:bighorn_sheep=twilightsparksdelightfabric:raw_bighorn_mutton",
                    "twilightforest:helmet_crab=twilightsparksdelightfabric:hermit_crab_leg",
                    "twilightforest:fire_beetle=twilightsparksdelightfabric:fire_beetle_leg",
                    "twilightforest:slime_beetle=twilightsparksdelightfabric:slime_beetle_leg",
                    "twilightforest:pinch_beetle=twilightsparksdelightfabric:pinch_beetle_leg",
                    "twilightforest:minotaur=twilightforest:raw_meef",
                    "twilightforest:minoshroom=twilightforest:raw_meef",
                    "twilightforest:hydra=twilightforest:hydra_chop"));
    public static final ListValue EXPERIMENT_MEAT_ACTIVITY_COSTS = strings("experimentMeatActivityCosts",
            List.of(
                    "minecraft:porkchop:10",
                    "minecraft:beef:10",
                    "minecraft:chicken:4",
                    "minecraft:mutton:8",
                    "minecraft:rabbit:3",
                    "minecraft:rabbit_foot:15",
                    "farmersdelight:ham:50",
                    "twilightforest:raw_venison:10",
                    "twilightforest:raw_meef:30",
                    "twilightsparksdelightfabric:raw_wild_boar_meat:10",
                    "twilightsparksdelightfabric:raw_bighorn_mutton:8",
                    "twilightsparksdelightfabric:hermit_crab_leg:13",
                    "twilightsparksdelightfabric:fire_beetle_leg:25",
                    "twilightsparksdelightfabric:slime_beetle_leg:25",
                    "twilightsparksdelightfabric:pinch_beetle_leg:40"));
    public static final DoubleValue EXPERIMENT_FATAL_ACTIVITY_COST =
            number("experimentFatalActivityCost", 20000.0D, 0.0D, Double.MAX_VALUE);
    public static final DoubleValue EXPERIMENT_PROLIFERATION_CHANCE =
            number("experimentProliferationChance", 0.30D, 0.0D, 1.0D);
    public static final DoubleValue EXPERIMENT_ACTIVITY_DISCOUNT_PER_LEVEL =
            number("experimentActivityDiscountPerLevel", 0.06D, 0.0D, 1.0D);
    public static final IntValue EXPERIMENT_FATAL_TRIGGERS_PER_SLEEP =
            integer("experimentFatalTriggersPerSleep", 3, 1, 100);
    public static final BooleanValue EXPERIMENT_FATAL_ALLOWS_BOSS_ATTACKS =
            bool("experimentFatalAllowsBossAttacks", true);
    public static final BooleanValue EXPERIMENT_FATAL_BOSS_ATTACKS_CONSUME_TRIGGER =
            bool("experimentFatalBossAttacksConsumeTrigger", true);
    public static final ListValue EXPERIMENT_FATAL_NON_CONSUMING_ATTACKERS =
            strings("experimentFatalNonConsumingAttackers", List.of());

    public enum FatalProtectionItemLocation {
        INVENTORY,
        OFFHAND,
        BAUBLES_OR_OFFHAND
    }

    public static final EnumValue<FatalProtectionItemLocation> EXPERIMENT_FATAL_ITEM_LOCATION =
            enumeration("experimentFatalItemLocation", FatalProtectionItemLocation.INVENTORY);

    public static final DoubleValue SHRINK_BASE_REDUCTION =
            number("shrinkBaseReduction", 0.40D, 0.0D, 0.9D);
    public static final DoubleValue SHRINK_REDUCTION_PER_LEVEL =
            number("shrinkReductionPerLevel", 0.10D, 0.0D, 0.9D);
    public static final DoubleValue ENLARGE_BASE_SCALE =
            number("enlargeBaseScale", 1.00D, 0.0D, 20.0D);
    public static final DoubleValue ENLARGE_SCALE_PER_LEVEL =
            number("enlargeScalePerLevel", 1.00D, 0.0D, 20.0D);
    public static final DoubleValue CHARGE_SPEED_BONUS =
            number("chargeSpeedBonus", 0.20D, 0.0D, 10.0D);
    public static final DoubleValue CHARGE_STEP_HEIGHT_BONUS =
            number("chargeStepHeightBonus", 5.0D, 0.0D, 10.0D);
    public static final DoubleValue CHARGE_MAX_ATTACK_BONUS =
            number("chargeMaxAttackBonus", 0.50D, 0.0D, 10.0D);
    public static final DoubleValue SYMBIOSIS_DAMAGE_REDUCTION =
            number("symbiosisDamageReduction", 0.10D, 0.0D, 0.95D);
    public static final DoubleValue SYMBIOSIS_DAMAGE_REDUCTION_PER_LEVEL =
            number("symbiosisDamageReductionPerLevel", 0.10D, 0.0D, 0.95D);
    public static final DoubleValue SYMBIOSIS_HUNGER_DRAIN_PER_TICK =
            number("symbiosisHungerDrainPerTick", 0.0025D, 0.0D, 10.0D);
    public static final IntValue SYMBIOSIS_HEAL_INTERVAL =
            integer("symbiosisHealInterval", 100, 1, 72000);
    public static final DoubleValue SYMBIOSIS_ENTITY_HEAL_AMOUNT =
            number("symbiosisEntityHealAmount", 1.0D, 0.0D, 1000.0D);
    public static final DoubleValue GRIEF_DAMAGE_BONUS_PER_LEVEL =
            number("griefDamageBonusPerLevel", 0.10D, 0.0D, 10.0D);
    public static final DoubleValue DOUBLE_CROWN_ICE_CREAM_NO_FROSTED_CHANCE =
            number("doubleCrownIceCreamNoFrostedChance", 0.10D, 0.0D, 1.0D);
    public static final DoubleValue DOUBLE_CROWN_ICE_CREAM_LONG_FROSTED_CHANCE =
            number("doubleCrownIceCreamLongFrostedChance", 0.10D, 0.0D, 1.0D);
    public static final DoubleValue HELMET_CRAB_CUTTING_EXTRA_LEGS_CHANCE =
            number("helmetCrabCuttingExtraLegsChance", 0.75D, 0.0D, 1.0D);
    public static final DoubleValue HELMET_CRAB_CUTTING_ARMOR_CHANCE =
            number("helmetCrabCuttingArmorChance", 0.25D, 0.0D, 1.0D);
    public static final IntValue GLORY_CRUCIBLE_CAPACITY_MB =
            integer("gloryCrucibleCapacityMb", 2000, 1000, 64000);
    public static final IntValue GLORY_CRUCIBLE_BREWING_TICKS =
            integer("gloryCrucibleBrewingTimeTicks", 200, 1, 72000);
    public static final DoubleValue RABBIT_POCKET_WATCH_KILLER_RABBIT_CHANCE =
            number("rabbitPocketWatchKillerRabbitChance", 0.10D, 0.0D, 1.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_BASE_RIPENING_CHANCE =
            number("pickledBrackenJarBaseRipeningChance", 0.50D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_SHADE_BONUS =
            number("pickledBrackenJarShadeBonus", 0.20D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_LEAF_SHADE_BONUS =
            number("pickledBrackenJarLeafShadeBonus", 0.30D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_CLEAN_AREA_BONUS =
            number("pickledBrackenJarCleanAreaBonus", 0.15D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_MAGIC_LOG_BONUS =
            number("pickledBrackenJarMagicLogBonus", 0.50D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_MUSHROOM_COLONY_BONUS =
            number("pickledBrackenJarMushroomColonyBonus", 0.10D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_PEACOCK_FAN_BONUS =
            number("pickledBrackenJarPeacockFanBonus", 0.30D, 0.0D, 100.0D);
    public static final DoubleValue PICKLED_BRACKEN_JAR_STRUCTURE_BONUS =
            number("pickledBrackenJarStructureBonus", 0.20D, 0.0D, 100.0D);
    public static final IntValue GIANT_COOKING_POT_MAX_BATCHES =
            integer("giantCookingPotMaxBatches", 8, 1, 64);
    public static final IntValue GIANTS_COOKING_POT_MAX_BATCHES =
            integer("giantsCookingPotMaxBatches", 16, 1, 64);
    public static final IntValue GIANTS_STOVE_MAX_PORTIONS =
            integer("giantsStoveMaxPortions", 16, 1, 16);
    public static final DoubleValue ARMORED_GIANT_SKILLET_VARIANT_CHANCE =
            number("armoredGiantSkilletVariantChance", 0.20D, 0.0D, 1.0D);
    public static final DoubleValue ARMORED_GIANT_KITCHEN_SET_DROP_CHANCE =
            number("armoredGiantKitchenSetDropChance", 0.25D, 0.0D, 1.0D);
    public static final IntValue TWILIGHT_CHEESE_FONDUE_DINER_COUNT =
            integer("twilightCheeseFondueDinerCount", 3, 1, 64);

    // ------------------------------------------------------------------
    // Loading and saving
    // ------------------------------------------------------------------

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("twilightsparksdelightfabric.json");
    }

    /** Reads the configuration file, creating it with the defaults if needed. */
    public static synchronized void init() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = path();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    JsonObject object = root.getAsJsonObject();
                    for (Option<?> option : OPTIONS) {
                        JsonElement element = object.get(option.key());
                        if (element != null) {
                            try {
                                option.read(element);
                            } catch (RuntimeException ignored) {
                                // Keep the default for malformed entries.
                            }
                        }
                    }
                }
            } catch (IOException | RuntimeException exception) {
                TwilightSparksDelightFabric.LOGGER.error("Unable to read the configuration file", exception);
            }
        } else {
            save();
        }
    }

    /** Writes the current configuration back to disk. */
    public static synchronized void save() {
        JsonObject root = new JsonObject();
        for (Option<?> option : OPTIONS) {
            root.add(option.key(), option.write());
        }
        try {
            Files.createDirectories(path().getParent());
            try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            TwilightSparksDelightFabric.LOGGER.error("Unable to write the configuration file", exception);
        }
    }

    // ------------------------------------------------------------------
    // Derived values
    // ------------------------------------------------------------------

    private static Map<ResourceLocation, Double> parsedMeatActivityCosts() {
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();
        for (String raw : EXPERIMENT_MEAT_ACTIVITY_COSTS.get()) {
            int separator = raw.lastIndexOf(':');
            if (separator <= 0 || separator == raw.length() - 1) {
                continue;
            }
            try {
                ResourceLocation item = ResourceLocation.parse(raw.substring(0, separator).trim());
                double cost = Double.parseDouble(raw.substring(separator + 1).trim());
                if (Double.isFinite(cost) && cost > 0.0D) {
                    result.put(item, cost);
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries.
            }
        }
        return result;
    }

    public static Map<ResourceLocation, Double> getExperimentMeatActivityCosts() {
        Map<ResourceLocation, Double> result = parsedMeatActivityCosts();
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> binding : getExperimentBindings().entrySet()) {
            double health = getEntityMaxHealth(binding.getKey());
            if (health > 0.0D && Double.isFinite(health)) {
                for (ResourceLocation item : binding.getValue()) {
                    result.putIfAbsent(item, getExperimentMeatActivityCost(item));
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static double getExperimentMeatActivityCost(ResourceLocation item) {
        if (item == null) {
            return 0.0D;
        }
        double explicit = parsedMeatActivityCosts().getOrDefault(item, 0.0D);
        if (explicit > 0.0D) {
            return explicit;
        }
        double generated = 0.0D;
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> binding : getExperimentBindings().entrySet()) {
            if (!binding.getValue().contains(item)) {
                continue;
            }
            generated = Math.max(generated, getEntityMaxHealth(binding.getKey()));
        }
        return generated;
    }

    @SuppressWarnings("unchecked")
    private static double getEntityMaxHealth(ResourceLocation entityId) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (entityType == null || !DefaultAttributes.hasSupplier(entityType)) {
            return 0.0D;
        }
        try {
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            return DefaultAttributes.getSupplier(livingType).getBaseValue(Attributes.MAX_HEALTH);
        } catch (RuntimeException ignored) {
            return 0.0D;
        }
    }

    public static Map<ResourceLocation, List<ResourceLocation>> getExperimentBindings() {
        Map<ResourceLocation, List<ResourceLocation>> result = new LinkedHashMap<>();
        for (String raw : EXPERIMENT_ENTITY_MEAT_BINDINGS.get()) {
            int separator = raw.indexOf('=');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            try {
                ResourceLocation entity = ResourceLocation.parse(raw.substring(0, separator).trim());
                ResourceLocation meat = ResourceLocation.parse(raw.substring(separator + 1).trim());
                result.computeIfAbsent(entity, ignored -> new ArrayList<>()).add(meat);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries.
            }
        }
        return result;
    }

    public static boolean isExperimentFatalNonConsumingAttacker(ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }
        return EXPERIMENT_FATAL_NON_CONSUMING_ATTACKERS.get().stream()
                .map(String::trim)
                .anyMatch(entityId.toString()::equals);
    }

    private TSDConfig() {
    }
}

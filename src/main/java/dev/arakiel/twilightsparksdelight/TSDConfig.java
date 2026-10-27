package dev.arakiel.twilightsparksdelight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.arakiel.gemini.config.Config;
import dev.arakiel.gemini.config.ConfigEntry;
import dev.arakiel.gemini.config.ConfigHandle;
import dev.arakiel.gemini.config.GeminiConfig;
import dev.arakiel.gemini.config.Range;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

public final class TSDConfig {
    private static volatile ConfigHandle<Values> handle;

    @Config(value = "config", namespace = TwilightSparksDelight.MOD_ID, nested = false)
    public static final class Values {
        @ConfigEntry("randomCureEffectBlacklist")
        public List<String> randomCureEffectBlacklist = List.of();

        @ConfigEntry("configVersion")
        public String configVersion = "1";

        @ConfigEntry("extendedFoodStatsEnabled")
        public boolean extendedFoodStatsEnabled = true;

        @ConfigEntry("extendedFoodProgressiveCapsEnabled")
        public boolean extendedFoodProgressiveCapsEnabled = true;

        @ConfigEntry("extendedFoodKeepVanillaRespawn")
        public boolean extendedFoodKeepVanillaRespawn = true;

        @ConfigEntry("extendedFoodExtraConsumptionEnabled")
        public boolean extendedFoodExtraConsumptionEnabled = true;

        @ConfigEntry("extendedFoodExtraConsumptionMultiplier")
        @Range(min = 0.0D, max = 10.0D)
        public double extendedFoodExtraConsumptionMultiplier = 0.5D;

        @ConfigEntry("extendedFoodExtraBenefitMultiplier")
        @Range(min = 0.0D, max = 10.0D)
        public double extendedFoodExtraBenefitMultiplier = 0.25D;

        @ConfigEntry("experimentBindingModeEnabled")
        public boolean experimentBindingModeEnabled;

        @ConfigEntry("experimentWorkstation")
        public ExperimentWorkstation experimentWorkstation = ExperimentWorkstation.CUTTING_BOARD;

        @ConfigEntry("experimentEntityMeatBindings")
        public List<String> experimentEntityMeatBindings = List.of(
                "minecraft:pig=minecraft:porkchop",
                "minecraft:cow=minecraft:beef",
                "minecraft:chicken=minecraft:chicken",
                "minecraft:sheep=minecraft:mutton",
                "minecraft:rabbit=minecraft:rabbit",
                "minecraft:spider=minecraft:spider_eye",
                "minecraft:zombie=minecraft:rotten_flesh",
                "twilightforest:wild_boar=twilightsparksdelight:raw_wild_boar_meat",
                "twilightforest:bighorn_sheep=twilightsparksdelight:raw_bighorn_mutton",
                "twilightforest:helmet_crab=twilightsparksdelight:hermit_crab_leg",
                "twilightforest:fire_beetle=twilightsparksdelight:fire_beetle_leg",
                "twilightforest:slime_beetle=twilightsparksdelight:slime_beetle_leg",
                "twilightforest:pinch_beetle=twilightsparksdelight:pinch_beetle_leg",
                "twilightforest:minotaur=twilightforest:raw_meef",
                "twilightforest:minoshroom=twilightforest:raw_meef",
                "twilightforest:hydra=twilightforest:hydra_chop");

        @ConfigEntry("experimentMeatActivityCosts")
        public List<String> experimentMeatActivityCosts = List.of(
                "minecraft:porkchop:10",
                "minecraft:beef:10",
                "minecraft:chicken:4",
                "minecraft:mutton:8",
                "minecraft:rabbit:3",
                "minecraft:rabbit_foot:15",
                "farmersdelight:ham:50",
                "twilightforest:raw_venison:10",
                "twilightforest:raw_meef:30",
                "twilightsparksdelight:raw_wild_boar_meat:10",
                "twilightsparksdelight:raw_bighorn_mutton:8",
                "twilightsparksdelight:hermit_crab_leg:13",
                "twilightsparksdelight:fire_beetle_leg:25",
                "twilightsparksdelight:slime_beetle_leg:25",
                "twilightsparksdelight:pinch_beetle_leg:40");

        @ConfigEntry("experimentFatalActivityCost")
        @Range(min = 0.0D, max = Double.MAX_VALUE)
        public double experimentFatalActivityCost = 20000.0D;

        @ConfigEntry("experimentProliferationChance")
        @Range(min = 0.0D, max = 1.0D)
        public double experimentProliferationChance = 0.3D;

        @ConfigEntry("experimentActivityDiscountPerLevel")
        @Range(min = 0.0D, max = 1.0D)
        public double experimentActivityDiscountPerLevel = 0.06D;

        @ConfigEntry("experimentFatalTriggersPerSleep")
        @Range(min = 1.0D, max = 100.0D)
        public int experimentFatalTriggersPerSleep = 3;

        @ConfigEntry("experimentFatalAllowsBossAttacks")
        public boolean experimentFatalAllowsBossAttacks = true;

        @ConfigEntry("experimentFatalBossAttacksConsumeTrigger")
        public boolean experimentFatalBossAttacksConsumeTrigger = true;

        @ConfigEntry("experimentFatalNonConsumingAttackers")
        public List<String> experimentFatalNonConsumingAttackers = List.of();

        @ConfigEntry("experimentFatalItemLocation")
        public FatalProtectionItemLocation experimentFatalItemLocation = FatalProtectionItemLocation.INVENTORY;

        @ConfigEntry("shrinkBaseReduction")
        @Range(min = 0.0D, max = 0.9D)
        public double shrinkBaseReduction = 0.4D;

        @ConfigEntry("shrinkReductionPerLevel")
        @Range(min = 0.0D, max = 0.9D)
        public double shrinkReductionPerLevel = 0.1D;

        @ConfigEntry("enlargeBaseScale")
        @Range(min = 0.0D, max = 20.0D)
        public double enlargeBaseScale = 1.0D;

        @ConfigEntry("enlargeScalePerLevel")
        @Range(min = 0.0D, max = 20.0D)
        public double enlargeScalePerLevel = 1.0D;

        @ConfigEntry("chargeSpeedBonus")
        @Range(min = 0.0D, max = 10.0D)
        public double chargeSpeedBonus = 0.2D;

        @ConfigEntry("chargeStepHeightBonus")
        @Range(min = 0.0D, max = 10.0D)
        public double chargeStepHeightBonus = 5.0D;

        @ConfigEntry("chargeMaxAttackBonus")
        @Range(min = 0.0D, max = 10.0D)
        public double chargeMaxAttackBonus = 0.5D;

        @ConfigEntry("symbiosisDamageReduction")
        @Range(min = 0.0D, max = 0.95D)
        public double symbiosisDamageReduction = 0.1D;

        @ConfigEntry("symbiosisDamageReductionPerLevel")
        @Range(min = 0.0D, max = 0.95D)
        public double symbiosisDamageReductionPerLevel = 0.1D;

        @ConfigEntry("symbiosisHungerDrainPerTick")
        @Range(min = 0.0D, max = 10.0D)
        public double symbiosisHungerDrainPerTick = 0.0025D;

        @ConfigEntry("symbiosisHealInterval")
        @Range(min = 1.0D, max = 72000.0D)
        public int symbiosisHealInterval = 100;

        @ConfigEntry("symbiosisEntityHealAmount")
        @Range(min = 0.0D, max = 1000.0D)
        public double symbiosisEntityHealAmount = 1.0D;

        @ConfigEntry("griefDamageBonusPerLevel")
        @Range(min = 0.0D, max = 10.0D)
        public double griefDamageBonusPerLevel = 0.1D;

        @ConfigEntry("doubleCrownIceCreamNoFrostedChance")
        @Range(min = 0.0D, max = 1.0D)
        public double doubleCrownIceCreamNoFrostedChance = 0.1D;

        @ConfigEntry("doubleCrownIceCreamLongFrostedChance")
        @Range(min = 0.0D, max = 1.0D)
        public double doubleCrownIceCreamLongFrostedChance = 0.1D;

        @ConfigEntry("helmetCrabCuttingExtraLegsChance")
        @Range(min = 0.0D, max = 1.0D)
        public double helmetCrabCuttingExtraLegsChance = 0.75D;

        @ConfigEntry("helmetCrabCuttingArmorChance")
        @Range(min = 0.0D, max = 1.0D)
        public double helmetCrabCuttingArmorChance = 0.25D;

        @ConfigEntry("gloryCrucibleCapacityMb")
        @Range(min = 1000.0D, max = 64000.0D)
        public int gloryCrucibleCapacityMb = 2000;

        @ConfigEntry("gloryCrucibleBrewingTimeTicks")
        @Range(min = 1.0D, max = 72000.0D)
        public int gloryCrucibleBrewingTicks = 200;

        @ConfigEntry("rabbitPocketWatchKillerRabbitChance")
        @Range(min = 0.0D, max = 1.0D)
        public double rabbitPocketWatchKillerRabbitChance = 0.1D;

        @ConfigEntry("pickledBrackenJarBaseRipeningChance")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarBaseRipeningChance = 0.5D;

        @ConfigEntry("pickledBrackenJarShadeBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarShadeBonus = 0.2D;

        @ConfigEntry("pickledBrackenJarLeafShadeBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarLeafShadeBonus = 0.3D;

        @ConfigEntry("pickledBrackenJarCleanAreaBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarCleanAreaBonus = 0.15D;

        @ConfigEntry("pickledBrackenJarMagicLogBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarMagicLogBonus = 0.5D;

        @ConfigEntry("pickledBrackenJarMushroomColonyBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarMushroomColonyBonus = 0.1D;

        @ConfigEntry("pickledBrackenJarPeacockFanBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarPeacockFanBonus = 0.3D;

        @ConfigEntry("pickledBrackenJarStructureBonus")
        @Range(min = 0.0D, max = 100.0D)
        public double pickledBrackenJarStructureBonus = 0.2D;

        @ConfigEntry("giantCookingPotMaxBatches")
        @Range(min = 1.0D, max = 64.0D)
        public int giantCookingPotMaxBatches = 8;

        @ConfigEntry("giantsCookingPotMaxBatches")
        @Range(min = 1.0D, max = 64.0D)
        public int giantsCookingPotMaxBatches = 16;

        @ConfigEntry("giantsStoveMaxPortions")
        @Range(min = 1.0D, max = 16.0D)
        public int giantsStoveMaxPortions = 16;

        @ConfigEntry("armoredGiantSkilletVariantChance")
        @Range(min = 0.0D, max = 1.0D)
        public double armoredGiantSkilletVariantChance = 0.2D;

        @ConfigEntry("armoredGiantKitchenSetDropChance")
        @Range(min = 0.0D, max = 1.0D)
        public double armoredGiantKitchenSetDropChance = 0.25D;

        @ConfigEntry("twilightCheeseFondueDinerCount")
        @Range(min = 1.0D, max = 64.0D)
        public int twilightCheeseFondueDinerCount = 3;
    }

    public enum ExperimentWorkstation {
        CRAFTING_TABLE,
        CUTTING_BOARD,
        COOKING_POT
    }

    public enum FatalProtectionItemLocation {
        INVENTORY,
        OFFHAND,
        BAUBLES_OR_OFFHAND
    }

    private TSDConfig() {
    }

    public static void init() {
        initialize();
    }

    public static Values values() {
        ConfigHandle<Values> current = handle;
        if (current == null) {
            current = initialize();
        }
        return current.value();
    }

    private static synchronized ConfigHandle<Values> initialize() {
        if (handle == null) {
            GeminiConfig.init();
            handle = GeminiConfig.register(Values.class);
        }
        return handle;
    }

    public static void save() {
        ConfigHandle<Values> current = handle;
        if (current == null) {
            current = initialize();
        }
        current.save();
    }

    private static Map<ResourceLocation, Double> parsedMeatActivityCosts() {
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();
        for (String raw : values().experimentMeatActivityCosts) {
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
            if (binding.getValue().contains(item)) {
                generated = Math.max(generated, getEntityMaxHealth(binding.getKey()));
            }
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
        for (String raw : values().experimentEntityMeatBindings) {
            int separator = raw.indexOf('=');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            try {
                ResourceLocation entity = ResourceLocation.parse(raw.substring(0, separator).trim());
                ResourceLocation meat = ResourceLocation.parse(raw.substring(separator + 1).trim());
                result.computeIfAbsent(entity, ignored -> new ArrayList<>()).add(meat);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    public static boolean isExperimentFatalNonConsumingAttacker(ResourceLocation entityId) {
        return entityId != null && values().experimentFatalNonConsumingAttackers.stream()
                .map(String::trim)
                .anyMatch(entityId.toString()::equals);
    }
}

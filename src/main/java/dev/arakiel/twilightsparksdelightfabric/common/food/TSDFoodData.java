package dev.arakiel.twilightsparksdelightfabric.common.food;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Extended hunger and saturation cap.
 *
 * <p>The NeoForge edition attached an owner to {@code FoodData} through a
 * mixin so the vanilla caps could be raised. The owner is still attached here,
 * but the cap is expressed directly through {@link #ownerOf(FoodData)}.</p>
 */
public final class TSDFoodData {
    public interface OwnerAccess {
        void tsd$setOwner(Player player);

        Player tsd$getOwner();
    }

    private static final ThreadLocal<Boolean> HUNGER_EXHAUSTION = ThreadLocal.withInitial(() -> false);

    private TSDFoodData() {
    }

    public static void attach(Player player) {
        ((OwnerAccess) player.getFoodData()).tsd$setOwner(player);
    }

    public static Player ownerOf(FoodData data) {
        return ((OwnerAccess) data).tsd$getOwner();
    }

    public static float exhaustionMultiplier(Player player) {
        return !HUNGER_EXHAUSTION.get() && shouldUseExtraDrain(player)
                ? (float) (1 + TSDConfig.EXTENDED_FOOD_EXTRA_CONSUMPTION_MULTIPLIER.get()) : 1.0F;
    }

    public static void resetForRespawn(Player player, boolean extendedCap, float originalSaturation) {
        FoodData data = player.getFoodData();
        CompoundTag saved = new CompoundTag();
        data.addAdditionalSaveData(saved);
        int food = extendedCap ? getMaxFood(player) : BASE_CAP;
        saved.putInt("foodLevel", food);
        saved.putFloat("foodSaturationLevel", Math.min(food, Math.max(0, originalSaturation)));
        saved.putFloat("foodExhaustionLevel", 0);
        saved.putInt("foodTickTimer", 0);
        data.readAdditionalSaveData(saved);
        attach(player);
    }

    public static void copy(FoodData source, FoodData target) {
        CompoundTag saved = new CompoundTag();
        source.addAdditionalSaveData(saved);
        target.readAdditionalSaveData(saved);
    }

    public static void applyHungerExhaustion(Player player, float amount) {
        boolean previous = HUNGER_EXHAUSTION.get();
        HUNGER_EXHAUSTION.set(true);
        try {
            player.causeFoodExhaustion(amount);
        } finally {
            HUNGER_EXHAUSTION.set(previous);
        }
    }

    // ------------------------------------------------------------------
    // Progression
    // ------------------------------------------------------------------

    public static final int BASE_CAP = 20;
    public static final int FULL_CAP = 40;
    public static final String CLIENT_CAP = "twilightsparksdelightfabric.food_cap";
    private static final int CAP_PER_ADVANCEMENT = 2;

    private static final ResourceLocation[] PROGRESSION = {
            id("progress_naga"),
            id("progress_lich"),
            id("progress_knights"),
            id("progress_labyrinth"),
            id("progress_yeti"),
            id("progress_ur_ghast"),
            id("progress_hydra"),
            id("progress_glacier"),
            id("quest_ram"),
            id("progress_merge")
    };

    public static int getMaxFood(Player player) {
        if (!TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()) {
            return BASE_CAP;
        }
        if (player != null && player.level().isClientSide) {
            CompoundTag data = TSDPersistentData.of(player);
            if (data.contains(CLIENT_CAP)) {
                return Math.max(BASE_CAP, Math.min(FULL_CAP, data.getInt(CLIENT_CAP)));
            }
            return FULL_CAP;
        }
        if (!TSDConfig.EXTENDED_FOOD_PROGRESSIVE_CAPS_ENABLED.get()
                || !(player instanceof ServerPlayer serverPlayer)) {
            return FULL_CAP;
        }

        int completed = 0;
        for (ResourceLocation advancementId : PROGRESSION) {
            AdvancementHolder advancement = serverPlayer.server.getAdvancements().get(advancementId);
            if (advancement != null && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone()) {
                completed++;
            }
        }
        return Math.min(FULL_CAP, BASE_CAP + completed * CAP_PER_ADVANCEMENT);
    }

    public static float getMaxSaturation(Player player) {
        return getMaxFood(player);
    }

    public static boolean isInExtraRange(Player player) {
        return player != null && (player.getFoodData().getFoodLevel() > BASE_CAP
                || player.getFoodData().getSaturationLevel() > BASE_CAP);
    }

    public static boolean shouldUseExtraDrain(Player player) {
        return TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()
                && TSDConfig.EXTENDED_FOOD_EXTRA_CONSUMPTION_ENABLED.get()
                && player != null && isInExtraRange(player);
    }

    public static double getExtraBenefit(Player player) {
        return TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()
                && TSDConfig.EXTENDED_FOOD_EXTRA_CONSUMPTION_ENABLED.get() && isInExtraRange(player)
                ? TSDConfig.EXTENDED_FOOD_EXTRA_BENEFIT_MULTIPLIER.get()
                : 0.0D;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("twilightforest", path);
    }
}

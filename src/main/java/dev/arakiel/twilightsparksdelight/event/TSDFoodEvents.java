package dev.arakiel.twilightsparksdelight.event;

import dev.arakiel.twilightsparksdelight.common.effect.TSDMobEffects;
import dev.arakiel.twilightsparksdelight.common.item.TSDItems;
import dev.arakiel.twilightsparksdelight.registry.TSDRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Food behaviour that cannot be expressed through {@code FoodProperties}. */
public final class TSDFoodEvents {
    private static final int EFFECT_DURATION = 3600;
    private static final int HYDRA_CHOP_NUTRITION = 16;
    private static final float HYDRA_CHOP_SATURATION = 16.0F;

    private TSDFoodEvents() {
    }

    /** Called when a living entity finished using an item. */
    public static void onItemFinished(LivingEntity consumer, ItemStack stack) {
        if (consumer.level().isClientSide) {
            return;
        }
        for (MobEffectInstance effect : TSDItems.nagaRiceEffects(stack)) {
            consumer.addEffect(effect);
        }
        applyHydraChopValues(consumer, stack);
        if (stack.is(TSDRegistry.Items.DRINK_ME)) {
            changeSizeEffect(consumer, TSDRegistry.Effects.SHRINK, TSDRegistry.Effects.ENLARGE, 5);
        } else if (stack.is(TSDRegistry.Items.EAT_ME)) {
            int level = changeSizeEffect(consumer, TSDRegistry.Effects.ENLARGE, TSDRegistry.Effects.SHRINK, 3);
            if (consumer instanceof ServerPlayer player && level >= 3) {
                var data = dev.arakiel.twilightsparksdelight.common.data.TSDPersistentData.of(player);
                if (!data.getBoolean("twilightsparksdelight.enlarge_three_unlocked")) {
                    data.putBoolean("twilightsparksdelight.enlarge_three_unlocked", true);
                    TSDRegistry.Triggers.DONT_EAT_ME.trigger(player);
                }
            }
        }
    }

    /**
     * The Twilight Forest hydra chop feeds as much as the migrated hydra based
     * meals. Fabric's item API of this Minecraft version has no default item
     * component event that runs before a dependency constructs its items, so
     * the difference is applied right after the chop was eaten.
     */
    private static void applyHydraChopValues(LivingEntity consumer, ItemStack stack) {
        // Only players have a food bar; every other consumer is unaffected.
        if (!(consumer instanceof Player player)
                || !stack.is(twilightforest.init.TFItems.HYDRA_CHOP.get())) {
            return;
        }
        FoodProperties original = stack.get(DataComponents.FOOD);
        if (original == null) {
            return;
        }
        int extraNutrition = Math.max(0, HYDRA_CHOP_NUTRITION - original.nutrition());
        float extraSaturation = Math.max(0.0F, HYDRA_CHOP_SATURATION - original.saturation());
        if (extraNutrition > 0 || extraSaturation > 0.0F) {
            player.getFoodData().eat(extraNutrition, extraSaturation);
        }
    }

    private static int changeSizeEffect(LivingEntity entity,
                                        net.minecraft.world.effect.MobEffect increase,
                                        net.minecraft.world.effect.MobEffect decrease,
                                        int maximumLevel) {
        var decreaseHolder = TSDMobEffects.holder(decrease);
        var increaseHolder = TSDMobEffects.holder(increase);
        MobEffectInstance opposite = entity.getEffect(decreaseHolder);
        if (opposite != null) {
            entity.removeEffect(decreaseHolder);
            if (opposite.getAmplifier() > 0) {
                entity.addEffect(new MobEffectInstance(decreaseHolder, EFFECT_DURATION,
                        opposite.getAmplifier() - 1, false, false, true));
            }
            return 0;
        }
        MobEffectInstance current = entity.getEffect(increaseHolder);
        int nextLevel = current == null ? 1 : Math.min(maximumLevel, current.getAmplifier() + 2);
        entity.addEffect(new MobEffectInstance(
                increaseHolder, EFFECT_DURATION, nextLevel - 1, false, false, true));
        return nextLevel;
    }

}

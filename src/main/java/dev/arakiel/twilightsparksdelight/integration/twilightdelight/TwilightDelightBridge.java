package dev.arakiel.twilightsparksdelight.integration.twilightdelight;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class TwilightDelightBridge {
    public interface Hooks {
        List<MobEffectInstance> visibleEffects(ItemStack stack, List<MobEffectInstance> effects);

        void afterEating(LivingEntity entity, ItemStack stack);
    }

    private static volatile Hooks hooks = new Hooks() {
        @Override
        public List<MobEffectInstance> visibleEffects(ItemStack stack, List<MobEffectInstance> effects) {
            return effects;
        }

        @Override
        public void afterEating(LivingEntity entity, ItemStack stack) {
        }
    };

    private TwilightDelightBridge() {
    }

    public static void install(Hooks implementation) {
        hooks = implementation;
    }

    public static List<MobEffectInstance> visibleEffects(ItemStack stack, List<MobEffectInstance> effects) {
        return hooks.visibleEffects(stack, effects);
    }

    public static void afterEating(LivingEntity entity, ItemStack stack) {
        hooks.afterEating(entity, stack);
    }
}

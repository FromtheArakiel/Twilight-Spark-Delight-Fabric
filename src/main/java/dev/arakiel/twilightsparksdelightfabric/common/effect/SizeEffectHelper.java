package dev.arakiel.twilightsparksdelightfabric.common.effect;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/** Scale, reach and jump maths shared by the size effects. */
public final class SizeEffectHelper {
    public static final ResourceLocation SCALE_MODIFIER_ID =
            TwilightSparksDelightFabric.id("potion_scale");

    private SizeEffectHelper() {
    }

    public static float getScale(Player player) {
        MobEffectInstance enlarge = getEffectDuringConstruction(player, TSDRegistry.Effects.ENLARGE);
        MobEffectInstance shrink = getEffectDuringConstruction(player, TSDRegistry.Effects.SHRINK);
        int enlargeLevel = enlarge == null ? 0 : enlarge.getAmplifier() + 1;
        int shrinkLevel = shrink == null ? 0 : shrink.getAmplifier() + 1;
        int netLevel = enlargeLevel - shrinkLevel;

        if (netLevel > 0) {
            double increase = TSDConfig.ENLARGE_BASE_SCALE.get()
                    + TSDConfig.ENLARGE_SCALE_PER_LEVEL.get() * netLevel;
            return (float) Math.max(0.1D, 1.0D + increase);
        }
        if (netLevel < 0) {
            double reduction = TSDConfig.SHRINK_BASE_REDUCTION.get()
                    + TSDConfig.SHRINK_REDUCTION_PER_LEVEL.get() * -netLevel;
            return (float) Math.max(0.1D, 1.0D - Math.min(0.9D, reduction));
        }
        return 1.0F;
    }

    private static MobEffectInstance getEffectDuringConstruction(Player player,
                                                                  net.minecraft.core.Holder<
                                                                          net.minecraft.world.effect.MobEffect>
                                                                          effect) {
        /*
         * The size event fires while a LivingEntity is still being
         * constructed; the active effect map is not initialized yet, so an
         * effect lookup has to be treated as empty until construction ends.
         */
        try {
            return player.getEffect(effect);
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    public static double getReachMultiplier(Player player) {
        float scale = getScale(player);
        return scale > 1.0F ? scale : scale < 1.0F ? 1.0D - (1.0D - scale) * 0.5D : 1.0D;
    }

    public static double getDodgeChance(Player player) {
        float scale = getScale(player);
        return scale < 1.0F ? Math.max(0.0D, Math.min(0.95D, 1.0D - scale)) : 0.0D;
    }

    public static double attributeValueWithout(AttributeInstance attribute, ResourceLocation excluded,
                                               boolean sanitize) {
        double base = attribute.getBaseValue();
        double baseMultiplier = 1;
        double totalMultiplier = 1;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (excluded != null && modifier.id().equals(excluded)) {
                continue;
            }
            switch (modifier.operation()) {
                case ADD_VALUE -> base += modifier.amount();
                case ADD_MULTIPLIED_BASE -> baseMultiplier += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= 1 + modifier.amount();
            }
        }
        double value = base * baseMultiplier * totalMultiplier;
        return sanitize ? attribute.getAttribute().value().sanitizeValue(value) : value;
    }

    public static double additiveModifierFactor(AttributeInstance attribute) {
        double base = 1;
        double total = 1;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                base += modifier.amount();
            } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                total *= 1 + modifier.amount();
            }
        }
        return base * total;
    }

    public static double jumpVelocityForHeight(double height, double gravity) {
        if (gravity <= 0) {
            return 0.42D;
        }
        double low = 0;
        double high = Math.max(1, Math.sqrt(2 * gravity * height) + gravity + 1);
        for (int attempt = 0; attempt < 40; attempt++) {
            double velocity = (low + high) * 0.5D;
            double rise = 0;
            double motion = velocity;
            for (int tick = 0; motion > 0 && tick < 1000; tick++) {
                rise += motion;
                motion = (motion - gravity) * 0.98D;
            }
            if (rise < height) {
                low = velocity;
            } else {
                high = velocity;
            }
        }
        return (low + high) * 0.5D;
    }
}

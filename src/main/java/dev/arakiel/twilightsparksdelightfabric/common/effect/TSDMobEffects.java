package dev.arakiel.twilightsparksdelightfabric.common.effect;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Mob effects whose behaviour depends on the affected entity type. */
public final class TSDMobEffects {
    private TSDMobEffects() {
    }

    public enum Behavior {
        NONE,
        SYMBIOSIS
    }

    public static class Effect extends MobEffect {
        private final Behavior behavior;

        public Effect(MobEffectCategory category, int color) {
            this(category, color, Behavior.NONE);
        }

        public Effect(MobEffectCategory category, int color, Behavior behavior) {
            super(category, color);
            this.behavior = behavior;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return behavior == Behavior.SYMBIOSIS;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (behavior == Behavior.SYMBIOSIS && !entity.level().isClientSide) {
                if (entity instanceof Player player) {
                    var data = TSDPersistentData.of(player);
                    double value = data.getDouble("tsd_symbiosis_hunger");
                    double drain = TSDConfig.SYMBIOSIS_HUNGER_DRAIN_PER_TICK.get() / Math.max(1, amplifier + 1);
                    if (player.hasEffect(vectorwing.farmersdelight.common.registry.ModEffects.NOURISHMENT)) {
                        drain *= 0.5D;
                    }
                    value += drain;
                    while (value >= 1.0D) {
                        value -= 1.0D;
                        player.getFoodData().setFoodLevel(
                                Math.max(0, player.getFoodData().getFoodLevel() - 1));
                    }
                    data.putDouble("tsd_symbiosis_hunger", value);
                } else {
                    var data = TSDPersistentData.of(entity);
                    int ticks = data.getInt("tsd_symbiosis_heal_ticks") + 1;
                    if (ticks >= TSDConfig.SYMBIOSIS_HEAL_INTERVAL.get()) {
                        ticks = 0;
                        entity.heal(TSDConfig.SYMBIOSIS_ENTITY_HEAL_AMOUNT.get().floatValue());
                    }
                    data.putInt("tsd_symbiosis_heal_ticks", ticks);
                }
            }
            return true;
        }
    }
}

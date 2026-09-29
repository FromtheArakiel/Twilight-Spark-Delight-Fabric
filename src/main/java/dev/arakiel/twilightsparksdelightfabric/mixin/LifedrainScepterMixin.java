package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.event.TSDExperimentEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.item.LifedrainScepterItem;

/** Charges a held Experiment 250 instead of healing the scepter user. */
@Mixin(LifedrainScepterItem.class)
public abstract class LifedrainScepterMixin {
    @Unique
    private final ThreadLocal<Boolean> tsd$redirectRecovery = ThreadLocal.withInitial(() -> false);

    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void tsd$beginDrain(Level level, LivingEntity user, ItemStack stack, int ticks,
                                CallbackInfo callback) {
        tsd$redirectRecovery.set(false);
    }

    @Redirect(method = "onUseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt"
                    + "(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 0), require = 0)
    private boolean tsd$chargeExperiment(LivingEntity target, DamageSource source, float amount,
                                         Level level, LivingEntity user, ItemStack scepter, int ticks) {
        ItemStack experiment = TSDExperimentEvents.findRedirectExperiment(user);
        boolean slowed = target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        float healthBefore = target.getHealth();
        boolean hit = target.hurt(source, amount);
        if (hit && !experiment.isEmpty() && target.getHealth() < healthBefore) {
            TSDItems.Experiment250.addActivity(experiment, slowed ? 3 : 1);
            tsd$redirectRecovery.set(true);
        }
        return hit;
    }

    @Redirect(method = "onUseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"), require = 0)
    private void tsd$redirectScepterHealing(LivingEntity user, float amount) {
        if (!tsd$redirectRecovery.get()) {
            user.heal(amount);
        }
    }

    @Redirect(method = "onUseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"), require = 0)
    private void tsd$redirectScepterFood(FoodData food, int hunger, float saturation) {
        if (!tsd$redirectRecovery.get()) {
            food.eat(hunger, saturation);
        }
    }

    @Inject(method = "onUseTick", at = @At("RETURN"))
    private void tsd$endDrain(Level level, LivingEntity user, ItemStack stack, int ticks,
                              CallbackInfo callback) {
        tsd$redirectRecovery.remove();
    }
}

package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.event.TSDCombatEvents;
import dev.arakiel.twilightsparksdelightfabric.event.TSDExperimentEvents;
import dev.arakiel.twilightsparksdelightfabric.event.TSDFoodEvents;
import dev.arakiel.twilightsparksdelightfabric.event.TSDEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Damage, effect, item use and jump hooks of the mod. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private MobEffectInstance tsd$previousEffect;

    /**
     * Runs right after the consumed stack was finished, but before the game
     * clears the in-use stack, so the finished item is still available.
     */
    @Inject(method = "completeUsingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem"
                    + "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)"
                    + "Lnet/minecraft/world/item/ItemStack;",
            shift = At.Shift.AFTER))
    private void tsd$onItemFinished(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        TSDFoodEvents.onItemFinished(self, self.getUseItem());
        if (self instanceof ServerPlayer player) {
            TSDExperimentEvents.onItemFinished(player, self.getUseItem());
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void tsd$onJump(CallbackInfo callback) {
        if ((Object) this instanceof Player player) {
            TSDCombatEvents.onJump(player);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float tsd$modifyDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return amount;
        }
        float modified = TSDCombatEvents.modifyIncomingDamage(self, source, amount);
        if (self instanceof Mob mob && source.getEntity() instanceof Player) {
            TSDEvents.onPacifiedMobAttacked(mob, source.getEntity());
        }
        return Math.max(0.0F, modified);
    }

    /**
     * Fatal protection has to see the damage that would really be applied,
     * which is the post armour amount handled by {@code actuallyHurt}.
     */
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float tsd$fatalProtection(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && self instanceof ServerPlayer player
                && TSDExperimentEvents.preventFatalDamage(player, source, amount)) {
            return 0.0F;
        }
        return amount;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void tsd$cancelDamage(DamageSource source, float amount,
                                  CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && TSDCombatEvents.shouldCancelDamage(self, source)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void tsd$blockEffect(MobEffectInstance instance, Entity source,
                                 CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        tsd$previousEffect = self.getEffect(instance.getEffect());
        if (TSDCombatEvents.shouldBlockEffect(self, instance)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN"))
    private void tsd$effectAdded(MobEffectInstance instance, Entity source,
                                 CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            TSDCombatEvents.onEffectAdded((LivingEntity) (Object) this, instance, tsd$previousEffect);
        }
    }
}

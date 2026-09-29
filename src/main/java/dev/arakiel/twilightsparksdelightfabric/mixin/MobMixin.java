package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.event.TSDEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Honours the bracken pacification state of a mob. */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tsd$tickPacification(CallbackInfo callback) {
        TSDEvents.tickPacification((Mob) (Object) this);
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void tsd$blockPacifiedTarget(LivingEntity target, CallbackInfo callback) {
        if (target != null && TSDEvents.shouldCancelTargetChange((Mob) (Object) this, target)) {
            callback.cancel();
        }
    }
}

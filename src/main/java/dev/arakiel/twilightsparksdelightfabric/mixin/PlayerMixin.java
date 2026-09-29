package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.event.TSDExperimentEvents;
import dev.arakiel.twilightsparksdelightfabric.event.TSDPlayerEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mining speed and wake up hooks. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void tsd$extendedFoodBreakSpeed(BlockState state, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(TSDPlayerEvents.modifyBreakSpeed((Player) (Object) this, callback.getReturnValue()));
    }

    @Inject(method = "stopSleepInBed", at = @At("TAIL"))
    private void tsd$onWakeUp(boolean wakeImmediately, boolean updateLevelForSleepingPlayers,
                              CallbackInfo callback) {
        if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player) {
            TSDExperimentEvents.onWakeUp(player);
        }
    }
}

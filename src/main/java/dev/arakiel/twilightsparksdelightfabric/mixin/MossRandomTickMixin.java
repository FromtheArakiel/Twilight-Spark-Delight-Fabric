package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.event.TSDMossEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the Twilight Forest moss patch tick and spread onto our soils. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MossRandomTickMixin {
    @Shadow
    public abstract Block getBlock();

    @Inject(method = "isRandomlyTicking", at = @At("RETURN"), cancellable = true)
    private void tsd$tickingMoss(CallbackInfoReturnable<Boolean> callback) {
        if (getBlock() instanceof twilightforest.block.MossPatchBlock) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void tsd$spreadMoss(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        TSDMossEvents.onRandomTick(level, pos);
    }
}

package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.event.TSDMossEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bone meal applied to Twilight Forest moss spreads it like the moss block. */
@Mixin(BoneMealItem.class)
public abstract class BonemealItemMixin {
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void tsd$spreadMossWithBonemeal(ItemStack stack, Level level, BlockPos pos,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (TSDMossEvents.trySpreadWithBonemeal(level, pos)) {
            callback.setReturnValue(true);
        }
    }
}

package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.registry.ModBlocks;

/**
 * Keeps the giant cooking pot screens open.
 *
 * <p>Farmer's Delight validates its pot menu with
 * {@code stillValid(access, player, ModBlocks.COOKING_POT)}, which demands that
 * the block at the menu position is its own cooking pot. The giant cooking pots
 * are separate blocks, so the server closed their menu again on the very next
 * tick and the screen only flashed open. For those blocks the distance part of
 * the check is repeated here instead; the vanilla logic stays untouched for
 * every other block and menu.</p>
 */
@Mixin(AbstractContainerMenu.class)
public abstract class CookingPotStillValidMixin {
    @Inject(method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;"
                    + "Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/level/block/Block;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void tsd$giantCookingPotStillValid(ContainerLevelAccess access, Player player,
                                                       Block block, CallbackInfoReturnable<Boolean> callback) {
        if (block != ModBlocks.COOKING_POT.get()) {
            return;
        }
        access.evaluate((level, pos) -> {
            if (level.getBlockState(pos).getBlock() instanceof KitchenBlocks.GiantPot
                    && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D) <= 64.0D) {
                callback.setReturnValue(Boolean.TRUE);
            }
            return Boolean.FALSE;
        }, Boolean.FALSE);
    }
}

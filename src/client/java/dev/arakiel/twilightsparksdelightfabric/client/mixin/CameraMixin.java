package dev.arakiel.twilightsparksdelightfabric.client.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.effect.SizeEffectHelper;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Scales the near plane and camera padding for shrunk players. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Entity entity;

    @ModifyConstant(method = "getNearPlane", constant = @Constant(doubleValue = 0.05000000074505806D),
            require = 0)
    private double tsd$scaledNearPlane(double original) {
        return original * tsd$cameraScale();
    }

    @ModifyConstant(method = "getMaxZoom", constant = @Constant(floatValue = 0.1F), require = 0)
    private float tsd$scaledCollisionPadding(float original) {
        return original * tsd$cameraScale();
    }

    private float tsd$cameraScale() {
        return entity instanceof Player player && SizeEffectHelper.getScale(player) < 1
                ? Math.min(1.0F, player.getScale()) : 1.0F;
    }
}

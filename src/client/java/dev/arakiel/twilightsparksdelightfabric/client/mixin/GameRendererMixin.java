package dev.arakiel.twilightsparksdelightfabric.client.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.effect.SizeEffectHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Scales the projection near plane for shrunk players. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyConstant(method = "getProjectionMatrix", constant = @Constant(floatValue = 0.05F), require = 0)
    private float tsd$scaledNearPlane(float original) {
        var player = Minecraft.getInstance().player;
        return player != null && SizeEffectHelper.getScale(player) < 1
                ? original * Math.min(1.0F, player.getScale()) : original;
    }
}

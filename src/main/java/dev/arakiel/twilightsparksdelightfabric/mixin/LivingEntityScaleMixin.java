package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.effect.SizeEffectHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the native size pipeline but bypasses its cap for enlarged players. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScaleMixin {
    @Inject(method = "getScale", at = @At("RETURN"), cancellable = true)
    private void tsd$uncappedPlayerEnlarge(CallbackInfoReturnable<Float> callback) {
        if (!((Object) this instanceof Player player) || player.getAttributes() == null) {
            return;
        }
        var attribute = player.getAttribute(Attributes.SCALE);
        if (attribute == null || attribute.getModifier(SizeEffectHelper.SCALE_MODIFIER_ID) == null) {
            return;
        }
        double scale = SizeEffectHelper.attributeValueWithout(attribute, null, false);
        if (Double.isFinite(scale) && scale > 16) {
            callback.setReturnValue((float) scale);
        }
    }
}

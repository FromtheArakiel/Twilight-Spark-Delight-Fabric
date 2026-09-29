package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hunger exhaustion must not be scaled by the extended food multiplier. */
@Mixin(targets = "net.minecraft.world.effect.HungerMobEffect")
public abstract class HungerMobEffectMixin {
    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), require = 0)
    private void tsd$unscaledHungerExhaustion(Player player, float amount) {
        TSDFoodData.applyHungerExhaustion(player, amount);
    }
}

package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raises the vanilla food and saturation caps. */
@Mixin(FoodData.class)
public abstract class FoodDataMixin implements TSDFoodData.OwnerAccess {
    @Shadow
    private int foodLevel;
    @Shadow
    private float saturationLevel;
    @Unique
    private Player tsd$owner;
    @Unique
    private boolean tsd$previouslyEnabled;

    @Override
    public void tsd$setOwner(Player player) {
        tsd$owner = player;
        boolean enabled = TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get();
        if (enabled || tsd$previouslyEnabled) {
            int cap = enabled ? TSDFoodData.getMaxFood(player) : TSDFoodData.BASE_CAP;
            foodLevel = Math.max(0, Math.min(cap, foodLevel));
            saturationLevel = Math.max(0, Math.min(foodLevel, saturationLevel));
        }
        tsd$previouslyEnabled = enabled;
    }

    @Override
    public Player tsd$getOwner() {
        return tsd$owner;
    }

    @ModifyConstant(method = {"add", "needsFood"}, constant = @Constant(intValue = 20), require = 0)
    private int tsd$foodCap(int original) {
        return tsd$enabled() ? TSDFoodData.getMaxFood(tsd$owner) : original;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tsd$bindOwner(Player player, CallbackInfo callback) {
        tsd$setOwner(player);
    }

    @Inject(method = {"setFoodLevel", "setSaturation", "readAdditionalSaveData"}, at = @At("RETURN"))
    private void tsd$clampChanges(CallbackInfo callback) {
        if (tsd$enabled()) {
            tsd$setOwner(tsd$owner);
        }
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float tsd$extraExhaustion(float amount) {
        return tsd$enabled() ? amount * TSDFoodData.exhaustionMultiplier(tsd$owner) : amount;
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"), index = 0, require = 0)
    private float tsd$extraNaturalRegeneration(float amount) {
        return tsd$enabled()
                ? amount * (float) (1 + TSDFoodData.getExtraBenefit(tsd$owner)) : amount;
    }

    @Unique
    private boolean tsd$enabled() {
        return tsd$owner != null && TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get();
    }
}

package dev.arakiel.twilightsparksdelightfabric.client;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.helpers.FoodHelper;

/**
 * AppleSkin integration: the extra hunger and saturation rows are drawn on top
 * of AppleSkin's own overlays, so the icons share its anchor and its preview
 * animation.
 *
 * <p>Only loaded when AppleSkin is present; the vanilla compatible layer lives
 * in {@link TSDExtraFoodHud}.</p>
 */
public final class TSDAppleSkinCompat {
    private TSDAppleSkinCompat() {
    }

    public static void register() {
        HUDOverlayEvent.HungerRestored.EVENT.register(TSDAppleSkinCompat::onHungerRestored);
        HUDOverlayEvent.Saturation.EVENT.register(TSDAppleSkinCompat::onSaturation);
    }

    private static void onHungerRestored(HUDOverlayEvent.HungerRestored event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!enabled(player) || !ModConfig.INSTANCE.showFoodValuesHudOverlay || event.isCanceled) {
            return;
        }
        GuiGraphics graphics = event.context;
        var food = player.getFoodData();
        int currentExtra = Math.max(0, food.getFoodLevel() - TSDFoodData.BASE_CAP);
        FoodProperties preview = previewFood(player);
        int targetExtra = preview == null ? currentExtra
                : Math.max(0, Math.min(TSDFoodData.getMaxFood(player),
                        food.getFoodLevel() + preview.nutrition()) - TSDFoodData.BASE_CAP);
        float alpha = 0.3F + 0.25F * (float) Math.sin(minecraft.gui.getGuiTicks() / 5.0D);
        RenderSystem.enableBlend();
        try {
            if (targetExtra > currentExtra) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                TSDExtraFoodHud.renderFoodPreview(graphics, player, event.x, event.y, currentExtra, targetExtra);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    private static void onSaturation(HUDOverlayEvent.Saturation event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!enabled(player) || !ModConfig.INSTANCE.showSaturationHudOverlay || event.isCanceled) {
            return;
        }
        GuiGraphics graphics = event.context;
        var food = player.getFoodData();
        int currentExtra = Math.max(0, food.getFoodLevel() - TSDFoodData.BASE_CAP);
        float currentSaturation = Math.max(0.0F, food.getSaturationLevel() - TSDFoodData.BASE_CAP);
        FoodProperties preview = previewFood(player);
        int targetExtra = currentExtra;
        float targetSaturation = currentSaturation;
        if (preview != null) {
            int targetFood = Math.min(TSDFoodData.getMaxFood(player),
                    food.getFoodLevel() + preview.nutrition());
            targetExtra = Math.max(0, targetFood - TSDFoodData.BASE_CAP);
            targetSaturation = Math.max(0.0F, Math.min(targetFood,
                    food.getSaturationLevel() + preview.saturation()) - TSDFoodData.BASE_CAP);
        }
        float alpha = 0.3F + 0.25F * (float) Math.sin(minecraft.gui.getGuiTicks() / 5.0D);
        RenderSystem.enableBlend();
        try {
            if (currentSaturation > 0.0F) {
                // The current row is drawn once; the flashing preview is added
                // on top of it below.
                TSDExtraFoodHud.renderSaturation(graphics, event.x, event.y, currentExtra, currentSaturation);
            }
            if (targetSaturation > currentSaturation) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                TSDExtraFoodHud.renderSaturationPreview(graphics, event.x, event.y,
                        currentExtra, currentSaturation, targetExtra, targetSaturation);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    private static boolean enabled(Player player) {
        return TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get() && player != null;
    }

    private static FoodProperties previewFood(Player player) {
        if (!ModConfig.INSTANCE.showFoodValuesHudOverlay) {
            return null;
        }
        var result = FoodHelper.query(player.getMainHandItem(), player);
        if ((result == null || !FoodHelper.canConsume(player, result.modifiedFoodComponent))
                && ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand) {
            result = FoodHelper.query(player.getOffhandItem(), player);
        }
        return result != null && FoodHelper.canConsume(player, result.modifiedFoodComponent)
                ? result.modifiedFoodComponent : null;
    }
}

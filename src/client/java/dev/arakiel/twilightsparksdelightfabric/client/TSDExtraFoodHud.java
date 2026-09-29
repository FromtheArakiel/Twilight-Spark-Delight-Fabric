package dev.arakiel.twilightsparksdelightfabric.client;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Renders the part of the food bar above the vanilla twenty point range.
 *
 * <p>The NeoForge edition hooked the vanilla food GUI layer. On Fabric the
 * same icons are drawn from a HUD callback, and AppleSkin reports its own
 * overlay anchor so both stay aligned. The vertical position follows the
 * vanilla right hand HUD column (armour, vehicle, air) instead of using a
 * fixed coordinate.</p>
 */
public final class TSDExtraFoodHud {
    public static final ResourceLocation EXTRA_HUNGER = TwilightSparksDelightFabric.id(
            "textures/gui/extra_hunger_icons.png");
    public static final ResourceLocation EXTRA_SATURATION = TwilightSparksDelightFabric.id(
            "textures/gui/extra_saturation_icons.png");
    private static final int ICON_SIZE = 9;
    private static final int ROW_RIGHT = 91;

    private TSDExtraFoodHud() {
    }

    /** Draws the extra hunger icons of the player. */
    public static void render(GuiGraphics graphics) {
        if (!TSDConfig.EXTENDED_FOOD_STATS_ENABLED.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.gameMode == null
                || !minecraft.gameMode.canHurtPlayer() || isMountHealthShown(player)) {
            return;
        }
        FoodData food = player.getFoodData();
        int extraFood = Math.max(0, food.getFoodLevel() - TSDFoodData.BASE_CAP);
        if (extraFood <= 0) {
            return;
        }
        int left = graphics.guiWidth() / 2 + ROW_RIGHT;
        int top = foodRowTop(minecraft, graphics);
        RenderSystem.enableBlend();
        try {
            renderFood(graphics, player, left, top, extraFood);
        } finally {
            RenderSystem.disableBlend();
        }
    }

    /** Vertical position of the food row, honouring the vanilla HUD layout. */
    public static int foodRowTop(Minecraft minecraft, GuiGraphics graphics) {
        int top = graphics.guiHeight() - 39;
        if (minecraft.player != null && minecraft.player.getArmorValue() > 0) {
            top -= 10;
        }
        if (minecraft.player != null && minecraft.player.getVehicle() != null
                && minecraft.player.getVehicle().showVehicleHealth()) {
            top -= 10;
        }
        return top;
    }

    private static boolean isMountHealthShown(Player player) {
        return player.getVehicle() != null && player.getVehicle().showVehicleHealth();
    }

    private static void renderFood(GuiGraphics graphics, Player player, int left, int top, int extraFood) {
        boolean hunger = player.hasEffect(MobEffects.HUNGER);
        for (int index = 0; index < 10; index++) {
            int threshold = index * 2 + 1;
            if (threshold > extraFood) {
                continue;
            }
            int u = threshold == extraFood ? (hunger ? 27 : 9) : (hunger ? 36 : 18);
            draw(graphics, EXTRA_HUNGER, left - index * 8 - ICON_SIZE, top, u);
        }
    }

    /** Draws the extra saturation icons of the player. */
    public static void renderSaturation(GuiGraphics graphics, int left, int top,
                                        int extraFood, float extraSaturation) {
        for (int index = 0; index < 10; index++) {
            int slotFood = Math.min(2, Math.max(0, extraFood - index * 2));
            if (slotFood <= 0) {
                continue;
            }
            float slotSaturation = Math.min(slotFood, Math.max(0.0F, extraSaturation - index * 2));
            if (slotSaturation <= 0.0F) {
                continue;
            }
            int x = left - index * 8 - ICON_SIZE;
            float normalized = slotFood == 1
                    ? Math.min(0.5F, slotSaturation / 2.0F) : slotSaturation / 2.0F;
            draw(graphics, EXTRA_SATURATION, x, top, saturationU(normalized));
        }
    }

    /** Draws the food the held item would restore in the extra range. */
    public static void renderFoodPreview(GuiGraphics graphics, Player player, int left, int top,
                                         int currentExtra, int targetExtra) {
        boolean hunger = player.hasEffect(MobEffects.HUNGER);
        for (int index = Math.max(0, currentExtra / 2); index < 10; index++) {
            int current = Math.min(2, Math.max(0, currentExtra - index * 2));
            int target = Math.min(2, Math.max(0, targetExtra - index * 2));
            if (target <= current) {
                continue;
            }
            int u = target == 1 ? (hunger ? 27 : 9) : (hunger ? 36 : 18);
            draw(graphics, EXTRA_HUNGER, left - index * 8 - ICON_SIZE, top, u);
        }
    }

    /** Draws the saturation the held item would restore in the extra range. */
    public static void renderSaturationPreview(GuiGraphics graphics, int left, int top,
                                               int currentExtraFood, float currentExtraSaturation,
                                               int targetExtraFood, float targetExtraSaturation) {
        for (int index = 0; index < 10; index++) {
            int currentFood = Math.min(2, Math.max(0, currentExtraFood - index * 2));
            int targetFood = Math.min(2, Math.max(0, targetExtraFood - index * 2));
            float current = Math.min(currentFood, Math.max(0, currentExtraSaturation - index * 2));
            float target = Math.min(targetFood, Math.max(0, targetExtraSaturation - index * 2));
            if (target <= current) {
                continue;
            }
            draw(graphics, EXTRA_SATURATION, left - index * 8 - ICON_SIZE, top,
                    saturationU(target / 2.0F));
        }
    }

    private static int saturationU(float fill) {
        if (fill >= 1.0F) {
            return 27;
        }
        if (fill > 0.5F) {
            return 18;
        }
        if (fill > 0.25F) {
            return 9;
        }
        return 0;
    }

    private static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u) {
        graphics.blit(texture, x, y, u, 0, ICON_SIZE, ICON_SIZE);
    }
}

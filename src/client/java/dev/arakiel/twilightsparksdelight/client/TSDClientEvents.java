package dev.arakiel.twilightsparksdelight.client;

import dev.arakiel.twilightsparksdelight.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelight.common.food.TSDFoodData;
import dev.arakiel.twilightsparksdelight.common.network.TSDNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Client side handling of the mod payloads. */
public final class TSDClientEvents {
    private TSDClientEvents() {
    }

    public static void applyFoodState(TSDNetwork.FoodState state) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        TSDPersistentData.of(player).putInt(TSDFoodData.CLIENT_CAP, state.cap());
        TSDFoodData.attach(player);
        var data = player.getFoodData();
        data.setFoodLevel(state.food());
        data.setSaturation(state.saturation());
        data.setExhaustion(state.exhaustion());
    }

    public static void showExperimentActivation(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        // The activation mirrors the totem animation of the original edition.
        minecraft.gameRenderer.displayItemActivation(stack);
        minecraft.particleEngine.createTrackingEmitter(player,
                net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, 30);
        if (minecraft.level != null) {
            minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.TOTEM_USE, player.getSoundSource(), 1.0F, 1.0F, false);
        }
    }
}

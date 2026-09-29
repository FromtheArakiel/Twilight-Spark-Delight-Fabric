package dev.arakiel.twilightsparksdelightfabric.client;

import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelightfabric.common.food.TSDFoodData;
import dev.arakiel.twilightsparksdelightfabric.common.network.TSDPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Client side handling of the mod payloads. */
public final class TSDClientEvents {
    private TSDClientEvents() {
    }

    public static void applyFoodState(TSDPayloads.FoodState payload) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        TSDPersistentData.of(player).putInt(TSDFoodData.CLIENT_CAP, payload.cap());
        TSDFoodData.attach(player);
        var data = player.getFoodData();
        data.setFoodLevel(payload.food());
        data.setSaturation(payload.saturation());
        data.setExhaustion(payload.exhaustion());
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

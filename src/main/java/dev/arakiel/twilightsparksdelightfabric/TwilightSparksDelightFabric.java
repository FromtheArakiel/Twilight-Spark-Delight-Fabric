package dev.arakiel.twilightsparksdelightfabric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.arakiel.twilightsparksdelightfabric.common.network.TSDPayloads;
import dev.arakiel.twilightsparksdelightfabric.common.recipe.WaterSourceIngredient;
import dev.arakiel.twilightsparksdelightfabric.event.TSDEventHandlers;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

/**
 * Common entry point of the Fabric edition.
 *
 * <p>Content registration and the event wiring happen here, after Farmer's
 * Delight and the Twilight Forest have finished their own initialization.</p>
 */
public final class TwilightSparksDelightFabric implements ModInitializer {
    public static final String MOD_ID = "twilightsparksdelightfabric";
    public static final String MOD_NAME = "Twilight Spark's Delight";
    public static final String MOD_NAME_ZH = "暮光乐事";
    public static final String AUTHOR = "xy177";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        TSDConfig.init();
        WaterSourceIngredient.register();
        TSDRegistry.register();
        TSDPayloads.register();
        TSDEventHandlers.register();
        LOGGER.info("{} is loading for Minecraft 1.21.1 Fabric", MOD_NAME);
    }
}

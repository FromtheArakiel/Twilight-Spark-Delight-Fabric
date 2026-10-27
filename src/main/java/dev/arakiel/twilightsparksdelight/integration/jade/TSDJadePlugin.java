package dev.arakiel.twilightsparksdelight.integration.jade;

import dev.arakiel.twilightsparksdelight.common.block.FeastBlocks;
import dev.arakiel.twilightsparksdelight.common.block.KitchenBlocks;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.arakiel.gemini.integration.GeminiIntegrationType;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade integration.
 *
 * <p>Shaped after Twilight Forest's {@code twilightforest.compat.jade.JadeCompat}: the plugin only
 * registers client side block components, one singleton provider per meal family. Jade resolves the
 * registered classes through a hierarchy lookup, so the subclasses of each family are covered.</p>
 */
@WailaPlugin("twilightsparksdelight")
@GeminiIntegration(value = "jade", version = "[15.0,)", type = GeminiIntegrationType.CLIENT)
public final class TSDJadePlugin implements IWailaPlugin {
    public void applyClient() {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FeastServingsProvider.INSTANCE, FeastBlocks.Feast.class);
        registration.registerBlockComponent(StageFeastProvider.INSTANCE, FeastBlocks.StageFeast.class);
        // Multiblock cells: report the controller the invisible parts belong to.
        registration.registerBlockComponent(StructurePartProvider.INSTANCE, FeastBlocks.StructurePart.class);
        registration.registerBlockComponent(KitchenPartProvider.INSTANCE, KitchenBlocks.KitchenPart.class);
    }
}

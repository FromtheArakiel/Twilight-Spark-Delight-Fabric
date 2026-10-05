package dev.arakiel.twilightsparksdelightfabric.integration.jade;

import dev.arakiel.twilightsparksdelightfabric.common.block.FeastBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;

/**
 * Optional Jade integration.
 *
 * <p>Shaped after Twilight Forest's {@code twilightforest.compat.jade.JadeCompat}: the plugin only
 * registers client side block components, one singleton provider per meal family. Jade resolves the
 * registered classes through a hierarchy lookup, so the subclasses of each family are covered.</p>
 */
public final class TSDJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FeastServingsProvider.INSTANCE, FeastBlocks.Feast.class);
        registration.registerBlockComponent(StageFeastProvider.INSTANCE, FeastBlocks.StageFeast.class);
        // Multiblock cells: report the controller the invisible parts belong to.
        registration.registerBlockComponent(StructurePartProvider.INSTANCE, FeastBlocks.StructurePart.class);
        registration.registerBlockComponent(KitchenPartProvider.INSTANCE, KitchenBlocks.KitchenPart.class);
    }
}

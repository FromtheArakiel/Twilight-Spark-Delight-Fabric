package dev.arakiel.twilightsparksdelightfabric.integration.jade;

import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * The giant kitchen is a multiblock as well: the invisible cells report the controller they belong
 * to instead of their own structural name.
 */
public enum KitchenPartProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "kitchen_part");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof KitchenBlocks.KitchenPart part)) {
            return;
        }
        Level level = accessor.getLevel();
        BlockPos controllerPos = KitchenBlocks.Structure.findController(level, accessor.getPosition(),
                part.getControllerBlock());
        if (controllerPos == null) {
            return;
        }
        tooltip.add(level.getBlockState(controllerPos).getBlock().getName());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

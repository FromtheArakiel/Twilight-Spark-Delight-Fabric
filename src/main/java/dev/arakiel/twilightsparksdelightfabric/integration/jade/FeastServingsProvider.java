package dev.arakiel.twilightsparksdelightfabric.integration.jade;

import dev.arakiel.twilightsparksdelightfabric.common.block.FeastBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Shows how many servings are left on a meal block. */
public enum FeastServingsProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "feast_servings");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (state.getBlock() instanceof FeastBlocks.Feast feast) {
            tooltip.add(Component.translatable("jade.twilightsparksdelightfabric.servings",
                    state.getValue(FeastBlocks.Feast.SERVINGS), feast.getMaxServings()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

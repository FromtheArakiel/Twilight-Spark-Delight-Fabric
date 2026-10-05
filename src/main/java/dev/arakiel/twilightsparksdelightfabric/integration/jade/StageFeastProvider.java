package dev.arakiel.twilightsparksdelightfabric.integration.jade;

import dev.arakiel.twilightsparksdelightfabric.common.block.FeastBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Shows the current stage of the staged meals (bites feasts, large structures, ...). */
public enum StageFeastProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "stage_feast");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (state.getBlock() instanceof FeastBlocks.StageFeast stageFeast) {
            tooltip.add(Component.translatable("jade.twilightsparksdelightfabric.stage",
                    state.getValue(stageFeast.stageProperty()), stageFeast.getMaxStage()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

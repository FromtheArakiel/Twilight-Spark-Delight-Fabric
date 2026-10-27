package dev.arakiel.twilightsparksdelight.integration.jade;

import dev.arakiel.twilightsparksdelight.common.block.FeastBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * The large meals span a whole footprint: the visible controller owns the model and the serving
 * state, the invisible cells only provide collision. Looking at any cell now reports the controller
 * through the same lookup the cells use for interactions.
 */
public enum StructurePartProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelight", "structure_part");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof FeastBlocks.StructurePart part)) {
            return;
        }
        Level level = accessor.getLevel();
        BlockPos controllerPos = part.getController().findController(level, accessor.getPosition());
        if (controllerPos == null) {
            return;
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        tooltip.add(controllerState.getBlock().getName());
        if (controllerState.getBlock() instanceof FeastBlocks.StageFeast stageFeast) {
            tooltip.add(Component.translatable("jade.twilightsparksdelight.stage",
                    controllerState.getValue(stageFeast.stageProperty()), stageFeast.getMaxStage()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

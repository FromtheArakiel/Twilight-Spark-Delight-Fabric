package dev.arakiel.twilightsparksdelightfabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import dev.arakiel.twilightsparksdelightfabric.common.block.FeastBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Block entity renderers of the mod.
 *
 * <p>The giant kitchen controllers and the placeable large feasts use the
 * {@code ENTITYBLOCK_ANIMATED} render shape, so their baked block model has to
 * be submitted directly instead of dispatching an item. The cooking positions
 * of the giant stove are drawn as items on top of the cooking surface, and the
 * crucible draws a tinted fluid surface quad.</p>
 */
public final class TSDBlockEntityRenderers {
    private static final float HALF_SCALE = 2.0F;

    private TSDBlockEntityRenderers() {
    }

    // ------------------------------------------------------------------
    // Giant kitchen (stove and cooking pot controllers)
    // ------------------------------------------------------------------

    /**
     * Renders the 2x2x2 and 4x4x4 kitchen models.
     *
     * <p>The structure is driven by the requested structure size, so the 4x4
     * controllers scale the same baked model by {@code size / 2} about the
     * centre of their footprint.</p>
     */
    public static final class Kitchen<T extends BlockEntity> implements BlockEntityRenderer<T> {
        private final BlockRenderDispatcher blockRenderer;

        public Kitchen(BlockEntityRendererProvider.Context context) {
            this.blockRenderer = context.getBlockRenderDispatcher();
        }

        public static BlockEntityRenderer<TSDBlockEntities.GiantStove> stove(
                BlockEntityRendererProvider.Context context) {
            return new Kitchen<TSDBlockEntities.GiantStove>(context);
        }

        public static BlockEntityRenderer<TSDBlockEntities.GiantPot> pot(
                BlockEntityRendererProvider.Context context) {
            return new Kitchen<TSDBlockEntities.GiantPot>(context);
        }

        @Override
        public void render(T blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            BlockState state = blockEntity.getBlockState();
            if (!(state.getBlock() instanceof KitchenBlocks.KitchenController controller)) {
                return;
            }
            int size = controller.getStructureSize();
            BlockPos pos = blockEntity.getBlockPos();
            AABB bounds = KitchenBlocks.Structure.bounds(pos, state).deflate(0.1D);
            var center = bounds.getCenter();
            Direction facing = KitchenBlocks.Structure.controllerFacing(state);
            BlockState north = northState(state);
            if (blockEntity.getLevel() != null) {
                light = LevelRenderer.getLightColor(blockEntity.getLevel(),
                        BlockPos.containing(center.x, bounds.maxY + 0.1D, center.z));
            }

            pose.pushPose();
            pose.translate(center.x - pos.getX(), 0.0D, center.z - pos.getZ());
            pose.mulPose(Axis.YP.rotationDegrees(facingRotation(facing)));
            pose.scale(size / HALF_SCALE, size / HALF_SCALE, size / HALF_SCALE);
            pose.translate(-1.0D, 0.0D, -1.0D);
            blockRenderer.getModelRenderer().renderModel(pose.last(),
                    buffers.getBuffer(Sheets.cutoutBlockSheet()), north,
                    blockRenderer.getBlockModel(north), 1.0F, 1.0F, 1.0F, light, overlay);
            pose.popPose();

            if (blockEntity instanceof TSDBlockEntities.GiantStove stove) {
                renderStoveItems(stove, size, facing, center, pos, pose, buffers, light, overlay);
            } else if (blockEntity instanceof TSDBlockEntities.GiantPot pot) {
                renderPotContent(pot, size, facing, center, pos, pose, buffers, light, overlay);
            }
        }

        private static void renderStoveItems(TSDBlockEntities.GiantStove stove, int size, Direction facing,
                                             net.minecraft.world.phys.Vec3 center, BlockPos pos,
                                             PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var itemRenderer = Minecraft.getInstance().getItemRenderer();
            int portionLimit = size == 4
                    ? Math.min(stove.getItems().getSlotCount(),
                            Math.max(1, dev.arakiel.twilightsparksdelightfabric.TSDConfig
                                    .GIANTS_STOVE_MAX_PORTIONS.get()))
                    : stove.getItems().getSlotCount();
            float spread = size == 4 ? 2.6F : 1.2F;
            for (int slot = 0; slot < portionLimit; slot++) {
                var stack = stove.getItems().getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                var offset = stove.getStoveItemOffset(slot);
                pose.pushPose();
                pose.translate(center.x - pos.getX(), size + 0.02D, center.z - pos.getZ());
                pose.mulPose(Axis.YP.rotationDegrees(facingRotation(facing)));
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.translate(offset.x * spread, offset.y * spread, 0.0F);
                pose.scale(0.75F, 0.75F, 0.75F);
                itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                        stove.getLevel(), (int) pos.asLong() + slot);
                pose.popPose();
            }
        }

        private static void renderPotContent(TSDBlockEntities.GiantPot pot, int size, Direction facing,
                                             net.minecraft.world.phys.Vec3 center, BlockPos pos,
                                             PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var itemRenderer = Minecraft.getInstance().getItemRenderer();
            double height = size - 0.35D;
            float rotation = facingRotation(facing);
            pose.pushPose();
            pose.translate(center.x - pos.getX(), height, center.z - pos.getZ());
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            pose.scale(0.9F * size / HALF_SCALE, 0.9F * size / HALF_SCALE, 0.9F * size / HALF_SCALE);
            itemRenderer.renderStatic(pot.getMeal(), ItemDisplayContext.GROUND, light, overlay, pose, buffers,
                    pot.getLevel(), (int) pos.asLong());
            pose.popPose();

            pose.pushPose();
            pose.translate(center.x - pos.getX() + 0.45D, height - 0.15D, center.z - pos.getZ());
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            pose.scale(0.6F, 0.6F, 0.6F);
            itemRenderer.renderStatic(pot.getContainer(), ItemDisplayContext.GROUND, light, overlay, pose,
                    buffers, pot.getLevel(), (int) pos.asLong() + 1);
            pose.popPose();
        }

        @Override
        public int getViewDistance() {
            // The model of a 4x4x4 structure extends far beyond its own cell.
            return 160;
        }
    }

    // ------------------------------------------------------------------
    // Large placeable feasts
    // ------------------------------------------------------------------

    /** Large feast controller: submits the baked model with its block offset. */
    public static final class LargeFeast implements BlockEntityRenderer<TSDBlockEntities.LargeFeast> {
        private final BlockRenderDispatcher blockRenderer;

        public LargeFeast(BlockEntityRendererProvider.Context context) {
            this.blockRenderer = context.getBlockRenderDispatcher();
        }

        @Override
        public int getViewDistance() {
            // Large feasts span up to a 3x3 footprint.
            return 96;
        }

        @Override
        public void render(TSDBlockEntities.LargeFeast entity, float partialTick, PoseStack pose,
                           MultiBufferSource buffers, int light, int overlay) {
            BlockState state = entity.getBlockState();
            BlockState north = northState(state);
            if (entity.getLevel() != null) {
                light = LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos().above());
            }
            pose.pushPose();
            applyModelTransform(pose, state);
            // Animated blocks must submit the baked model directly.
            blockRenderer.getModelRenderer().renderModel(pose.last(),
                    buffers.getBuffer(Sheets.cutoutBlockSheet()), north,
                    blockRenderer.getBlockModel(north), 1.0F, 1.0F, 1.0F, light, overlay);
            pose.popPose();
        }

        /**
         * Moves the baked model onto the cell that visually owns it. The naga
         * rice platter is centred, the boar knuckle tray is offset by two
         * blocks and rotated to match its own model orientation.
         */
        public static void applyModelTransform(PoseStack pose, BlockState state) {
            Direction facing = KitchenBlocks.Structure.controllerFacing(state);
            pose.translate(0.5D, 0.0D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(facingRotation(facing)));
            pose.translate(-0.5D, 0.0D, -0.5D);
            if (state.getBlock() instanceof FeastBlocks.BoarKnuckle) {
                pose.translate(2.0D, 0.0D, 1.0D / 16.0D);
                pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            } else {
                pose.translate(0.0D, 0.0D, 1.0D);
            }
        }
    }

    // ------------------------------------------------------------------
    // Glory crucible
    // ------------------------------------------------------------------

    /** Glory crucible: draws the contained fluid as a tinted surface quad. */
    public static final class Crucible implements BlockEntityRenderer<TSDBlockEntities.GloryCrucible> {
        private static final float INSET_MIN = 2.01F / 16.0F;
        private static final float INSET_MAX = 13.99F / 16.0F;

        public Crucible(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(TSDBlockEntities.GloryCrucible crucible, float partialTick, PoseStack pose,
                           MultiBufferSource buffers, int light, int overlay) {
            if (crucible.getVolumeUnits() <= 0) {
                return;
            }
            ResourceLocation texture = stillTexture(crucible.getFluidKind());
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            int color = tintColor(crucible, partialTick);
            var vertices = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            var entry = pose.last();
            float y = (float) crucible.getSurfaceHeight();
            float u0 = sprite.getU(INSET_MIN);
            float u1 = sprite.getU(INSET_MAX);
            float v0 = sprite.getV(INSET_MIN);
            float v1 = sprite.getV(INSET_MAX);
            vertices.addVertex(entry, INSET_MIN, y, INSET_MIN).setColor(color).setUv(u0, v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, 0.0F, 1.0F, 0.0F);
            vertices.addVertex(entry, INSET_MIN, y, INSET_MAX).setColor(color).setUv(u0, v1)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, 0.0F, 1.0F, 0.0F);
            vertices.addVertex(entry, INSET_MAX, y, INSET_MAX).setColor(color).setUv(u1, v1)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, 0.0F, 1.0F, 0.0F);
            vertices.addVertex(entry, INSET_MAX, y, INSET_MIN).setColor(color).setUv(u1, v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, 0.0F, 1.0F, 0.0F);
        }

        private static ResourceLocation stillTexture(Fluid fluid) {
            if (TSDRegistry.Fluids.isHeatingFluid(fluid)) {
                return TwilightSparksDelightFabric.id("block/fiery_essence_still");
            }
            if (fluid == Fluids.LAVA) {
                return ResourceLocation.withDefaultNamespace("block/lava_still");
            }
            return ResourceLocation.withDefaultNamespace("block/water_still");
        }

        private static int tintColor(TSDBlockEntities.GloryCrucible crucible, float partialTick) {
            if (crucible.hasPotion()) {
                return crucible.getPotionColor(partialTick);
            }
            Fluid fluid = crucible.getFluidKind();
            if (fluid == Fluids.WATER && crucible.getLevel() != null) {
                return net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(
                        crucible.getLevel(), crucible.getBlockPos()) | 0xFF000000;
            }
            // Lava and the fiery essences use their own fully coloured texture.
            return 0xFFFFFFFF;
        }
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    /** Y rotation matching the controller facing. */
    static float facingRotation(Direction facing) {
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    /** The model is baked facing north and rotated at render time. */
    static BlockState northState(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH) : state;
    }
}

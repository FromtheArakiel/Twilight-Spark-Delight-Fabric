package dev.arakiel.twilightsparksdelightfabric.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import vectorwing.farmersdelight.common.block.CookingPotBlock;
import vectorwing.farmersdelight.common.block.StoveBlock;

/**
 * The giant kitchen multiblock: a controller block that owns the model and a
 * set of invisible part blocks covering the remaining footprint.
 */
public final class KitchenBlocks {
    private KitchenBlocks() {
    }

    /** Marker implemented by every giant kitchen controller. */
    public interface KitchenController {
        default int getStructureSize() {
            return 2;
        }

        ItemInteractionResult useOnPart(ItemStack held, BlockState state, Level level, BlockPos pos,
                                        Player player, InteractionHand hand, BlockHitResult hit);
    }

    /** Shared footprint maths for the 2x2x2 and 4x4x4 giant kitchen structures. */
    public static final class Structure {
        private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

        private Structure() {
        }

        public static List<BlockPos> positions(BlockPos controller, Direction facing) {
            return positions(controller, facing, 2);
        }

        public static List<BlockPos> positions(BlockPos controller, Direction facing, int size) {
            int actualSize = Math.max(1, size);
            List<BlockPos> result = new ArrayList<>(actualSize * actualSize * actualSize);
            for (int y = 0; y < actualSize; y++) {
                for (int z = 0; z < actualSize; z++) {
                    for (int x = 0; x < actualSize; x++) {
                        int dx;
                        int dz;
                        switch (facing) {
                            case EAST -> {
                                dx = -z;
                                dz = x;
                            }
                            case SOUTH -> {
                                dx = -x;
                                dz = -z;
                            }
                            case WEST -> {
                                dx = z;
                                dz = -x;
                            }
                            default -> {
                                dx = x;
                                dz = z;
                            }
                        }
                        result.add(controller.offset(dx, y, dz));
                    }
                }
            }
            return result;
        }

        public static boolean canPlace(Level level, BlockPos controller, Direction facing,
                                      Supplier<? extends Block> part, int size) {
            for (BlockPos pos : positions(controller, facing, size)) {
                if (pos.equals(controller)) {
                    continue;
                }
                if (!level.getBlockState(pos).canBeReplaced()) {
                    return false;
                }
            }
            return true;
        }

        public static boolean placeParts(Level level, BlockPos controller, Direction facing,
                                         Supplier<? extends Block> part, int size) {
            List<BlockPos> placed = new ArrayList<>();
            for (BlockPos pos : positions(controller, facing, size)) {
                if (pos.equals(controller)) {
                    continue;
                }
                BlockState partState = part.get().defaultBlockState();
                if (partState.hasProperty(BlockStateProperties.LIT)) {
                    partState = partState.setValue(BlockStateProperties.LIT,
                            level.getBlockState(controller)
                                    .getOptionalValue(BlockStateProperties.LIT).orElse(false));
                }
                if (!level.setBlock(pos, partState, Block.UPDATE_CLIENTS)) {
                    for (BlockPos placedPos : placed) {
                        if (level.getBlockState(placedPos).is(part.get())) {
                            level.removeBlock(placedPos, false);
                        }
                    }
                    return false;
                }
                placed.add(pos);
            }
            return true;
        }

        public static BlockPos findController(Level level, BlockPos partPos, Block controller) {
            int size = controller instanceof KitchenController kitchen ? kitchen.getStructureSize() : 2;
            for (int y = 0; y > -size; y--) {
                for (int x = -size + 1; x < size; x++) {
                    for (int z = -size + 1; z < size; z++) {
                        BlockPos candidate = partPos.offset(x, y, z);
                        BlockState state = level.getBlockState(candidate);
                        if (state.is(controller)
                                && positions(candidate, controllerFacing(state), size).contains(partPos)) {
                            return candidate;
                        }
                    }
                }
            }
            return null;
        }

        public static Direction controllerFacing(BlockState state) {
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            }
            return Direction.NORTH;
        }

        public static AABB bounds(BlockPos controller, BlockState state) {
            int size = state.getBlock() instanceof KitchenController kitchen ? kitchen.getStructureSize() : 2;
            AABB bounds = new AABB(controller);
            for (BlockPos pos : positions(controller, controllerFacing(state), size)) {
                bounds = bounds.minmax(new AABB(pos));
            }
            return bounds;
        }

        public static void syncLit(Level level, BlockPos controller, BlockState state, Block part) {
            boolean lit = state.getValue(BlockStateProperties.LIT);
            int size = ((KitchenController) state.getBlock()).getStructureSize();
            for (BlockPos pos : positions(controller, controllerFacing(state), size)) {
                BlockState current = level.getBlockState(pos);
                if (current.is(part)) {
                    level.setBlock(pos, current.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
                }
            }
        }

        /**
         * Per-block portion of the scaled cooking pot collision shape. The
         * rendered controller model spans the whole structure while every
         * invisible part only exposes the part occupying its own block.
         */
        public static VoxelShape scaledCookingPotShape(Level level, BlockPos partPos,
                                                       Block controller, Block part) {
            BlockPos controllerPos = findController(level, partPos, controller);
            if (controllerPos == null || !(level.getBlockState(controllerPos).getBlock()
                    instanceof KitchenController kitchen)) {
                return Shapes.empty();
            }
            BlockState controllerState = level.getBlockState(controllerPos);
            int size = kitchen.getStructureSize();
            List<BlockPos> structure = positions(controllerPos, controllerFacing(controllerState), size);
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos position : structure) {
                minX = Math.min(minX, position.getX());
                maxX = Math.max(maxX, position.getX());
                minZ = Math.min(minZ, position.getZ());
                maxZ = Math.max(maxZ, position.getZ());
            }

            int yOffset = partPos.getY() - controllerPos.getY();
            double bodyHeight = size * 0.625D;
            if (yOffset < 0 || yOffset >= size || yOffset >= bodyHeight) {
                return Shapes.empty();
            }
            double edgeInset = size * 0.125D;
            double minLocalX = partPos.getX() == minX ? edgeInset : 0.0D;
            double maxLocalX = partPos.getX() == maxX ? 1.0D - edgeInset : 1.0D;
            double minLocalZ = partPos.getZ() == minZ ? edgeInset : 0.0D;
            double maxLocalZ = 1.0D;
            if (partPos.getZ() == maxZ) {
                minLocalZ = 0.0D;
                maxLocalZ = 1.0D - edgeInset;
            }
            double maxLocalY = Math.min(1.0D, bodyHeight - yOffset);
            return Shapes.box(minLocalX, 0.0D, minLocalZ, maxLocalX, maxLocalY, maxLocalZ);
        }

        public static void removeFromController(Level level, BlockPos controller, BlockState state, Block part) {
            if (REMOVING.get()) {
                return;
            }
            int size = state.getBlock() instanceof KitchenController kitchen ? kitchen.getStructureSize() : 2;
            boolean previous = REMOVING.get();
            REMOVING.set(true);
            try {
                for (BlockPos pos : positions(controller, controllerFacing(state), size)) {
                    if (!pos.equals(controller) && level.getBlockState(pos).is(part)) {
                        level.removeBlock(pos, false);
                    }
                }
            } finally {
                REMOVING.set(previous);
            }
        }

        public static void removeControllerFromPart(Level level, BlockPos partPos, Block controller, Block part) {
            if (REMOVING.get()) {
                return;
            }
            BlockPos controllerPos = findController(level, partPos, controller);
            if (controllerPos == null) {
                return;
            }
            int size = controller instanceof KitchenController kitchen ? kitchen.getStructureSize() : 2;
            boolean previous = REMOVING.get();
            REMOVING.set(true);
            try {
                BlockState controllerState = level.getBlockState(controllerPos);
                for (BlockPos pos : positions(controllerPos, controllerFacing(controllerState), size)) {
                    if (!pos.equals(partPos) && !pos.equals(controllerPos)
                            && level.getBlockState(pos).is(part)) {
                        level.removeBlock(pos, false);
                    }
                }
                level.removeBlock(controllerPos, true);
            } finally {
                REMOVING.set(previous);
            }
        }

        public static boolean isRemoving() {
            return REMOVING.get();
        }
    }

    /** Giant stove controller built on Farmer's Delight's stove behaviour. */
    public static final class GiantStove extends StoveBlock implements KitchenController {
        private final Supplier<? extends Block> part;
        private final int structureSize;

        public GiantStove(BlockBehaviour.Properties properties, Supplier<? extends Block> part, int structureSize) {
            super(properties.noOcclusion().lightLevel(state -> state.getValue(LIT) ? 13 : 0));
            this.part = part;
            this.structureSize = Math.max(1, structureSize);
        }

        @Override
        public int getStructureSize() {
            return structureSize;
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public ItemInteractionResult useOnPart(ItemStack held, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
            return useItemOn(held, state, level, pos, player, hand, hit);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            if (state == null || !Structure.canPlace(context.getLevel(), context.getClickedPos(),
                    Structure.controllerFacing(state), part, structureSize)) {
                return null;
            }
            return state;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TSDBlockEntities.GiantStove(pos, state);
        }

        @Override
        protected ItemInteractionResult tryToPlaceFoodItem(ItemStack held, BlockState state, Level level,
                                                           BlockPos pos, Player player, InteractionHand hand,
                                                           BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof TSDBlockEntities.GiantStove stove)
                    || stove.shouldDropItems()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            var recipe = stove.getCookingRecipe(held);
            if (recipe.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide && stove.placeFood(player,
                    player.isCreative() ? held.copy() : held, recipe.get())) {
                level.playSound(null, hit.getBlockPos(), net.minecraft.sounds.SoundEvents.LANTERN_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void ignite(Entity entity, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                           BlockState state) {
            super.ignite(entity, level, pos, state);
            if (level instanceof Level actual && !actual.isClientSide) {
                Structure.syncLit(actual, pos, actual.getBlockState(pos), part.get());
            }
        }

        @Override
        public void extinguish(Entity entity, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                               BlockState state) {
            super.extinguish(entity, level, pos, state);
            if (level instanceof Level actual && !actual.isClientSide) {
                Structure.syncLit(actual, pos, actual.getBlockState(pos), part.get());
            }
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                return state.getValue(LIT)
                        ? createTickerHelper(type, TSDRegistry.BlockEntities.GIANT_STOVE,
                                TSDBlockEntities.GiantStove::particleTick)
                        : null;
            }
            return createStoveTicker(level, type, TSDRegistry.BlockEntities.GIANT_STOVE);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                            boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (!level.isClientSide) {
                Structure.placeParts(level, pos, Structure.controllerFacing(state), part, structureSize);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                             boolean movedByPiston) {
            if (!state.is(newState.getBlock())) {
                Structure.removeFromController(level, pos, state, part.get());
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    /** Giant cooking pot controller built on Farmer's Delight's cooking pot. */
    public static final class GiantPot extends CookingPotBlock implements KitchenController {
        private final Supplier<? extends Block> part;
        private final int structureSize;

        public GiantPot(BlockBehaviour.Properties properties, Supplier<? extends Block> part, int structureSize) {
            super(properties.noOcclusion());
            this.part = part;
            this.structureSize = Math.max(1, structureSize);
        }

        @Override
        public int getStructureSize() {
            return structureSize;
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public ItemInteractionResult useOnPart(ItemStack held, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
            return useItemOn(held, state, level, pos, player, hand, hit);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            if (state != null && structureSize == 4) {
                Block stove = TSDRegistry.Blocks.GIANTS_STOVE;
                BlockPos support = Structure.findController(context.getLevel(),
                        context.getClickedPos().below(), stove);
                if (support != null) {
                    state = state.setValue(FACING, Structure.controllerFacing(
                            context.getLevel().getBlockState(support)));
                }
            }
            if (state == null || !Structure.canPlace(context.getLevel(), context.getClickedPos(),
                    Structure.controllerFacing(state), part, structureSize)) {
                return null;
            }
            return state;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TSDBlockEntities.GiantPot(pos, state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (level instanceof Level actualLevel) {
                return Structure.scaledCookingPotShape(actualLevel, pos, this, part.get());
            }
            return super.getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
            if (level instanceof Level actualLevel) {
                return Structure.scaledCookingPotShape(actualLevel, pos, this, part.get());
            }
            return super.getCollisionShape(state, level, pos, context);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            return createTickerHelper(type, TSDRegistry.BlockEntities.GIANT_COOKING_POT,
                    level.isClientSide
                            ? TSDBlockEntities.GiantPot::animationTick
                            : TSDBlockEntities.GiantPot::cookingTick);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                            boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (!level.isClientSide) {
                Structure.placeParts(level, pos, Structure.controllerFacing(state), part, structureSize);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                             boolean movedByPiston) {
            if (!state.is(newState.getBlock())) {
                Structure.removeFromController(level, pos, state, part.get());
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    /** Invisible structure cell of a giant kitchen multiblock. */
    public static final class KitchenPart extends Block {
        public enum Kind {
            STOVE,
            COOKING_POT
        }

        private final Supplier<? extends Block> controller;
        private final Kind kind;

        public KitchenPart(BlockBehaviour.Properties properties, Supplier<? extends Block> controller, Kind kind) {
            super(properties.noOcclusion().lightLevel(state ->
                    state.getValue(BlockStateProperties.LIT) ? 13 : 0));
            this.controller = controller;
            this.kind = kind;
            registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.LIT);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
            BlockPos controllerPos = Structure.findController(level, pos, controller.get());
            if (controllerPos == null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            BlockState controllerState = level.getBlockState(controllerPos);
            BlockHitResult redirected = new BlockHitResult(hit.getLocation(), hit.getDirection(),
                    controllerPos, hit.isInside());
            if (controller.get() instanceof KitchenController kitchenController) {
                return kitchenController.useOnPart(held, controllerState, level, controllerPos,
                        player, hand, redirected);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        @Override
        public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                   Player player, BlockHitResult hit) {
            return useItemOn(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit).result();
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                             boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && !Structure.isRemoving()) {
                Structure.removeControllerFromPart(level, pos, controller.get(), this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (kind == Kind.STOVE && entity instanceof net.minecraft.world.entity.LivingEntity
                    && !entity.isSteppingCarefully() && isControllerLit(level, pos)) {
                entity.hurt(level.damageSources().hotFloor(), 1.0F);
            }
            super.stepOn(level, pos, state, entity);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (kind == Kind.COOKING_POT && level instanceof Level actualLevel) {
                return Structure.scaledCookingPotShape(actualLevel, pos, controller.get(), this);
            }
            return Shapes.block();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
            if (kind == Kind.COOKING_POT && level instanceof Level actualLevel) {
                return Structure.scaledCookingPotShape(actualLevel, pos, controller.get(), this);
            }
            return Shapes.block();
        }

        private boolean isControllerLit(Level level, BlockPos partPos) {
            BlockPos controllerPos = Structure.findController(level, partPos, controller.get());
            if (controllerPos == null) {
                return false;
            }
            return level.getBlockState(controllerPos)
                    .getOptionalValue(BlockStateProperties.LIT).orElse(false);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return List.of();
        }
    }

    /** Aligns a Giant's Cooking Pot placed on any top cell of a Giant's Stove. */
    public static final class GiantPotItem extends net.minecraft.world.item.BlockItem {
        public GiantPotItem(Item.Properties properties) {
            super(TSDRegistry.Blocks.GIANTS_COOKING_POT, properties);
        }

        @Override
        public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
            if (context.getClickedFace() == Direction.UP) {
                BlockPos stoveController = findStoveController(context);
                if (stoveController != null) {
                    int size = ((KitchenController) TSDRegistry.Blocks.GIANTS_STOVE).getStructureSize();
                    if (context.getClickedPos().getY() == stoveController.getY() + size - 1) {
                        BlockPos topCell = stoveController.above(size - 1);
                        BlockHitResult alignedHit = new BlockHitResult(context.getClickLocation(),
                                Direction.UP, topCell, context.isInside());
                        UseOnContext aligned = new UseOnContext(context.getLevel(), context.getPlayer(),
                                context.getHand(), context.getItemInHand(), alignedHit);
                        return super.useOn(aligned);
                    }
                }
            }
            return super.useOn(context);
        }

        private static BlockPos findStoveController(UseOnContext context) {
            BlockState clicked = context.getLevel().getBlockState(context.getClickedPos());
            if (clicked.is(TSDRegistry.Blocks.GIANTS_STOVE)) {
                return context.getClickedPos();
            }
            if (clicked.is(TSDRegistry.Blocks.GIANTS_STOVE_PART)) {
                return Structure.findController(context.getLevel(), context.getClickedPos(),
                        TSDRegistry.Blocks.GIANTS_STOVE);
            }
            return null;
        }
    }
}

package dev.arakiel.twilightsparksdelightfabric.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.TSDUtil;
import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import vectorwing.farmersdelight.common.registry.ModSounds;

/**
 * Every placeable meal of the mod.
 *
 * <p>The four families are kept next to each other because they share the same
 * serving state machine: a bite counter, an optional structure footprint and a
 * serving item that is handed to the player.</p>
 */
public final class FeastBlocks {
    private FeastBlocks() {
    }

    /** Farmer's Delight style serving block with a configurable serving count. */
    public static class Feast extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 0, 16);

        protected final Supplier<Item> servingItem;
        protected final int maxServings;
        protected final boolean hasLeftovers;
        private final VoxelShape[] shapes;

        public Feast(BlockBehaviour.Properties properties, Supplier<Item> servingItem,
                     int maxServings, boolean hasLeftovers) {
            super(properties.noOcclusion().sound(SoundType.WOOD));
            this.servingItem = servingItem;
            this.maxServings = Math.max(1, Math.min(16, maxServings));
            this.hasLeftovers = hasLeftovers;
            this.shapes = makeShapes(this.maxServings);
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(SERVINGS, this.maxServings));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SERVINGS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(SERVINGS, maxServings);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return shapes[state.getValue(SERVINGS)];
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            int servings = state.getValue(SERVINGS);
            if (servings == 0) {
                return useLeftovers(held, state, level, pos, player, hand, hit);
            }

            ItemStack meal = new ItemStack(servingItem.get());
            Item requiredContainerItem = meal.getItem().getCraftingRemainingItem();
            ItemStack requiredContainer = requiredContainerItem == null
                    ? ItemStack.EMPTY : new ItemStack(requiredContainerItem);
            if (requiredContainer.isEmpty() && requiresBowl(meal)) {
                requiredContainer = Items.BOWL.getDefaultInstance();
            }
            if (!requiredContainer.isEmpty()
                    && !ItemStack.isSameItemSameComponents(held, requiredContainer)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("farmersdelight.block.feast.use_container",
                                    requiredContainer.getHoverName()), true);
                }
                return ItemInteractionResult.SUCCESS;
            }

            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild && !held.isEmpty()
                        && !requiredContainer.isEmpty()) {
                    held.shrink(1);
                }
                TSDUtil.give(player, meal);

                int next = servings - 1;
                if (next == 0 && !hasLeftovers) {
                    level.destroyBlock(pos, false);
                } else {
                    level.setBlock(pos, state.setValue(SERVINGS, next), Block.UPDATE_ALL);
                }
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        protected ItemInteractionResult useLeftovers(ItemStack held, BlockState state, Level level,
                                                     BlockPos pos, Player player,
                                                     InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                level.destroyBlock(pos, true);
                level.playSound(null, pos, SoundEvents.WOOD_BREAK,
                        SoundSource.PLAYERS, 0.8F, 0.8F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            return state.getValue(SERVINGS);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return state.getValue(SERVINGS) == maxServings ? List.of(new ItemStack(asItem())) : List.of();
        }

        private static boolean requiresBowl(ItemStack meal) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(meal.getItem());
            return id != null && (id.getPath().startsWith("bowl_of_")
                    || id.getPath().contains("_soup")
                    || id.getPath().contains("sauce"));
        }

        private static VoxelShape[] makeShapes(int max) {
            // Every migrated feast shares the 0..16 serving property, so the
            // collision cache is built for the complete declared range.
            VoxelShape[] result = new VoxelShape[17];
            result[0] = Block.box(2, 0, 2, 14, 2, 14);
            for (int index = 1; index < result.length; index++) {
                double height = 2.0D + 12.0D * index / max;
                result[index] = Block.box(2, 0, 2, 14, height, 14);
            }
            return result;
        }
    }

    /** Feast variant whose state key is {@code bites}. */
    public static class BitesFeast extends StageFeast {
        public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 3);

        public BitesFeast(BlockBehaviour.Properties properties, int maxStage,
                          Supplier<Item> servingItem, boolean requiresBowl) {
            super(properties, maxStage, servingItem, requiresBowl);
        }

        @Override
        protected IntegerProperty getStateProperty() {
            return BITES;
        }
    }

    /** Serving block that progresses through discrete stages instead of servings. */
    public static class StageFeast extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 28);

        private final IntegerProperty stage;
        protected final int maxStage;
        protected final Supplier<Item> servingItem;
        protected final boolean requiresBowl;

        public StageFeast(BlockBehaviour.Properties properties, int maxStage,
                          Supplier<Item> servingItem, boolean requiresBowl) {
            super(properties.noOcclusion().sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK));
            this.stage = getStateProperty();
            this.maxStage = maxStage;
            this.servingItem = servingItem;
            this.requiresBowl = requiresBowl;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(stage, 0));
        }

        public IntegerProperty stageProperty() {
            return stage;
        }

        protected int currentStage(BlockState state) {
            return state.getValue(stage);
        }

        protected Item servingItem() {
            return servingItem.get();
        }

        protected IntegerProperty getStateProperty() {
            return STAGE;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, getStateProperty());
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(stage, 0);
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            int currentStage = state.getValue(stage);
            if (currentStage >= maxStage) {
                if (!level.isClientSide) {
                    level.destroyBlock(pos, true);
                    level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (TSDUtil.isKnife(held)) {
                if (!level.isClientSide) {
                    TSDUtil.give(player, new ItemStack(servingItem.get()));
                    if (!player.getAbilities().instabuild) {
                        held.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    }
                    advance(level, pos, state);
                    level.playSound(null, pos, ModSounds.BLOCK_FOOD_SLICE.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (requiresBowl && !held.is(Items.BOWL)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                if (!held.isEmpty() && !player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                TSDUtil.give(player, new ItemStack(servingItem.get()));
                advance(level, pos, state);
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        private void advance(Level level, BlockPos pos, BlockState state) {
            int nextStage = state.getValue(stage) + 1;
            if (nextStage >= maxStage) {
                level.destroyBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(stage, nextStage), Block.UPDATE_ALL);
            }
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return currentStage(state) == 0 ? List.of(new ItemStack(asItem())) : List.of();
        }
    }

    /**
     * Stage feast controller spanning a rectangular footprint. The controller
     * owns the model and the serving state; the invisible parts only provide
     * collision and forward interactions.
     */
    public static class LargeStageFeast extends StageFeast implements EntityBlock {
        private final int width;
        private final int depth;
        private final Supplier<StructurePart> part;

        public LargeStageFeast(BlockBehaviour.Properties properties, int maxStage,
                               Supplier<Item> servingItem, boolean requiresBowl,
                               int width, int depth, Supplier<StructurePart> part) {
            super(properties, maxStage, servingItem, requiresBowl);
            this.width = width;
            this.depth = depth;
            this.part = part;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.getBlockEntity(pos) instanceof TSDBlockEntities.LargeFeast feast) {
                feast.readItem(stack);
            }
            placeParts(level, pos, state);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TSDBlockEntities.LargeFeast(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        public AABB structureBounds(BlockPos pos, BlockState state) {
            AABB bounds = new AABB(pos);
            for (BlockPos cell : footprint(pos, state)) {
                bounds = bounds.minmax(new AABB(cell));
            }
            return bounds;
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            if (currentStage(state) != 0) {
                return finalDrops();
            }
            ItemStack result = new ItemStack(asItem());
            Object entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (entity instanceof TSDBlockEntities.LargeFeast feast) {
                feast.writeItem(result, true);
            }
            return List.of(result);
        }

        protected List<ItemStack> finalDrops() {
            return List.of();
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            if (state == null) {
                return null;
            }
            for (BlockPos partPos : footprint(context.getClickedPos(), state)) {
                if (!partPos.equals(context.getClickedPos())
                        && !context.getLevel().getBlockState(partPos).canBeReplaced()) {
                    return null;
                }
            }
            return state;
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            removeStructure(level, pos, false);
            return super.playerWillDestroy(level, pos, state, player);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                             BlockState newState, boolean movedByPiston) {
            if (newState.getBlock() != this) {
                removePartsForState(level, pos, state);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        private void removePartsForState(Level level, BlockPos controllerPos, BlockState controllerState) {
            for (BlockPos partPos : footprint(controllerPos, controllerState)) {
                if (!partPos.equals(controllerPos) && level.getBlockState(partPos).is(part.get())) {
                    level.removeBlock(partPos, false);
                }
            }
        }

        public BlockPos findController(Level level, BlockPos origin) {
            int radius = Math.max(width, depth);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = origin.offset(dx, 0, dz);
                    if (level.getBlockState(candidate).is(this) && contains(level, candidate, origin)) {
                        return candidate;
                    }
                }
            }
            return null;
        }

        public void removeStructure(Level level, BlockPos controllerPos, boolean dropController) {
            BlockState controllerState = level.getBlockState(controllerPos);
            if (!controllerState.is(this)) {
                return;
            }
            if (dropController) {
                popResource(level, controllerPos, new ItemStack(asItem()));
            }
            for (BlockPos partPos : footprint(controllerPos, controllerState)) {
                if (!partPos.equals(controllerPos) && level.getBlockState(partPos).is(part.get())) {
                    level.removeBlock(partPos, false);
                }
            }
        }

        public void destroyFromPart(Level level, BlockPos partPos, Player player) {
            BlockPos controllerPos = findController(level, partPos);
            if (controllerPos == null) {
                level.removeBlock(partPos, false);
                return;
            }
            BlockState controllerState = level.getBlockState(controllerPos);
            for (BlockPos other : footprint(controllerPos, controllerState)) {
                if (!other.equals(controllerPos) && !other.equals(partPos)
                        && level.getBlockState(other).is(part.get())) {
                    level.removeBlock(other, false);
                }
            }
            level.destroyBlock(controllerPos, !player.getAbilities().instabuild);
        }

        private void placeParts(Level level, BlockPos controllerPos, BlockState state) {
            for (BlockPos partPos : footprint(controllerPos, state)) {
                if (!partPos.equals(controllerPos) && level.getBlockState(partPos).canBeReplaced()) {
                    level.setBlock(partPos, part.get().defaultBlockState()
                            .setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
                }
            }
        }

        private boolean contains(Level level, BlockPos controllerPos, BlockPos target) {
            for (BlockPos pos : footprint(controllerPos, level.getBlockState(controllerPos))) {
                if (pos.equals(target)) {
                    return true;
                }
            }
            return false;
        }

        protected List<BlockPos> footprint(BlockPos controllerPos, BlockState state) {
            Direction facing = state.getValue(FACING);
            Direction away = facing.getOpposite();
            Direction side = facing.getClockWise();
            List<BlockPos> positions = new ArrayList<>(width * depth);
            int sideStart = width % 2 == 0 ? 0 : -(width / 2);
            for (int d = 0; d < depth; d++) {
                for (int w = 0; w < width; w++) {
                    positions.add(controllerPos.relative(side, sideStart + w).relative(away, d));
                }
            }
            return positions;
        }
    }

    /** Invisible structure cell used by the large placeable meals. */
    public static final class StructurePart extends Block {
        private final Supplier<? extends LargeStageFeast> controller;

        public StructurePart(BlockBehaviour.Properties properties,
                             Supplier<? extends LargeStageFeast> controller) {
            super(properties.noOcclusion().pushReaction(PushReaction.BLOCK));
            this.controller = controller;
            registerDefaultState(stateDefinition.any().setValue(StageFeast.FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(StageFeast.FACING);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            if (controller.get() instanceof BoarKnuckle) {
                return BoarKnuckle.cellShape(state.getValue(StageFeast.FACING));
            }
            return Block.box(0, 0, 0, 16, 16, 16);
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            LargeStageFeast feast = controller.get();
            BlockPos controllerPos = feast.findController(level, pos);
            if (controllerPos == null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return feast.useItemOn(stack, level.getBlockState(controllerPos), level,
                    controllerPos, player, hand, hit);
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            controller.get().destroyFromPart(level, pos, player);
            return state;
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return List.of();
        }
    }

    /** Two-cell Twilight Boar Knuckle tray. */
    public static final class BoarKnuckle extends LargeStageFeast {
        public BoarKnuckle(BlockBehaviour.Properties properties, Supplier<StructurePart> part) {
            super(properties, 6, () -> TSDRegistry.Items.PLATE_OF_TWILIGHT_BOAR_KNUCKLE, true, 2, 1, part);
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
            if (!held.is(Items.BOWL)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                TSDUtil.give(player, new ItemStack(TSDRegistry.Items.PLATE_OF_TWILIGHT_BOAR_KNUCKLE));
                if (currentStage(state) >= 5) {
                    for (ItemStack drop : finalDrops()) {
                        popResource(level, pos, drop);
                    }
                    level.removeBlock(pos, false);
                } else {
                    level.setBlock(pos, state.setValue(STAGE, currentStage(state) + 1), Block.UPDATE_ALL);
                }
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected List<ItemStack> finalDrops() {
            return List.of(new ItemStack(Items.BOWL), new ItemStack(Items.BONE_MEAL, 7));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return cellShape(state.getValue(FACING));
        }

        public static VoxelShape cellShape(Direction facing) {
            return switch (facing) {
                case NORTH -> box(0, 0, 1, 16, 9, 16);
                case EAST -> box(0, 0, 0, 15, 9, 16);
                case SOUTH -> box(0, 0, 0, 16, 9, 15);
                case WEST -> box(1, 0, 0, 16, 9, 16);
                default -> throw new IllegalArgumentException("Expected horizontal facing");
            };
        }
    }

    /**
     * The naga rice follows the legacy 28-stage serving pattern: every fourth
     * serving is a scale, other servings are bowls, and the completed platter
     * yields the shield and the naga trophy.
     */
    public static final class NagaRice extends LargeStageFeast {
        private static final ResourceLocation NAGA_SCALE =
                ResourceLocation.fromNamespaceAndPath("twilightforest", "naga_scale");

        public NagaRice(BlockBehaviour.Properties properties, Supplier<Item> servingItem,
                        Supplier<StructurePart> part) {
            super(properties, 28, servingItem, true, 3, 3, part);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                    List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            TSDItems.addNagaRiceLabel(stack, tooltip);
        }

        @Override
        protected List<ItemStack> finalDrops() {
            return List.of(new ItemStack(Items.SHIELD),
                    new ItemStack(twilightforest.init.TFItems.NAGA_TROPHY.get()));
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            int stage = currentStage(state);
            if (stage >= maxStage) {
                if (!level.isClientSide) {
                    for (ItemStack drop : finalDrops()) {
                        popResource(level, pos, drop);
                    }
                    removeStructure(level, pos, false);
                    level.removeBlock(pos, false);
                    level.playSound(null, pos, twilightforest.init.TFSounds.NAGA_HURT.get(),
                            SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            ItemStack result;
            boolean scaleServing = stage % 4 == 3;
            if (scaleServing) {
                result = new ItemStack(BuiltInRegistries.ITEM.get(NAGA_SCALE));
            } else {
                if (!held.is(Items.BOWL)) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                result = new ItemStack(TSDRegistry.Items.BOWL_OF_NAGA_MIXED_RICE);
                if (level.getBlockEntity(pos) instanceof TSDBlockEntities.LargeFeast feast) {
                    feast.writeItem(result, false);
                }
            }

            if (!level.isClientSide) {
                if (!scaleServing && !player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                TSDUtil.give(player, result);
                int next = Math.min(maxStage, stage + 1);
                level.setBlock(pos, state.setValue(stageProperty(), next), Block.UPDATE_ALL);
                level.playSound(null, pos, scaleServing ? twilightforest.init.TFSounds.NAGA_HURT.get()
                                : ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Four-bite abyss pie. */
    public static final class AbyssPie extends BitesFeast {
        public AbyssPie(BlockBehaviour.Properties properties) {
            super(properties.randomTicks(), 4, () -> TSDRegistry.Items.ABYSS_PIE_SLICE, false);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return box(0, 0, 0, 16, 8, 16);
        }

        @Override
        public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level,
                               BlockPos pos, net.minecraft.util.RandomSource random) {
            if (random.nextFloat() >= 0.5F) {
                return;
            }
            List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8));
            if (players.isEmpty()) {
                return;
            }
            Player target = players.get(random.nextInt(players.size()));
            level.playSound(null, target.blockPosition(), SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.AMBIENT, 1.0F, 1.0F);
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            int bites = state.getValue(BITES);
            if (TSDUtil.isKnife(held)) {
                if (!level.isClientSide) {
                    TSDUtil.give(player, new ItemStack(TSDRegistry.Items.ABYSS_PIE_SLICE));
                    if (!player.getAbilities().instabuild) {
                        held.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    }
                    advance(level, pos, state, bites);
                    level.playSound(null, pos, ModSounds.BLOCK_FOOD_SLICE.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!player.canEat(false)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                ItemStack food = new ItemStack(TSDRegistry.Items.ABYSS_PIE_SLICE);
                ItemStack remainder = food.getItem().finishUsingItem(food, level, player);
                if (!remainder.isEmpty()) {
                    TSDUtil.give(player, remainder);
                }
                advance(level, pos, state, bites);
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        private static void advance(Level level, BlockPos pos, BlockState state, int bites) {
            if (bites >= 3) {
                level.destroyBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
            }
        }
    }

    /** Six-serving borscht pot that turns back into the glory crucible. */
    public static final class Borscht extends Feast {
        public Borscht(BlockBehaviour.Properties properties) {
            super(properties, () -> TSDRegistry.Items.BOWL_OF_TWILIGHT_BORSCHT, 6, false);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return Shapes.block();
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            if (!held.is(Items.BOWL)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                TSDUtil.give(player, new ItemStack(TSDRegistry.Items.BOWL_OF_TWILIGHT_BORSCHT));
                int next = state.getValue(SERVINGS) - 1;
                if (next <= 0) {
                    level.setBlock(pos, TSDRegistry.Blocks.GLORY_CRUCIBLE.defaultBlockState(), Block.UPDATE_ALL);
                } else {
                    level.setBlock(pos, state.setValue(SERVINGS, next), Block.UPDATE_ALL);
                }
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return List.of(new ItemStack(state.getValue(SERVINGS) == maxServings
                    ? asItem() : TSDRegistry.Blocks.GLORY_CRUCIBLE.asItem()));
        }
    }

    /**
     * Unlit fondue is state 7, ready fondue is 6..1 and state 0 is the leftover
     * cookware. The companion is the only serving tool.
     */
    public static final class CheeseFondue extends Feast {
        private static final ResourceLocation FIERY_BLOOD =
                ResourceLocation.fromNamespaceAndPath("twilightforest", "fiery_blood");
        private static final ResourceLocation FIERY_TEARS =
                ResourceLocation.fromNamespaceAndPath("twilightforest", "fiery_tears");

        public CheeseFondue(BlockBehaviour.Properties properties) {
            super(properties, () -> TSDRegistry.Items.TWILIGHT_CHEESE_FONDUE_WITH_BREAD, 7, true);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return box(0, 0, 0, 16, 14, 16);
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            int servings = state.getValue(SERVINGS);
            if (servings == 7) {
                ItemStack fuel = findFuel(player);
                if (fuel.isEmpty()) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                if (!level.isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        fuel.shrink(1);
                    }
                    level.setBlock(pos, state.setValue(SERVINGS, 6), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (servings == 0) {
                if (!level.isClientSide) {
                    level.destroyBlock(pos, true);
                    level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!held.is(TSDRegistry.Items.TWILIGHT_CHEESE_FONDUE_COMPANION)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                TSDItems.addFondueDiner(held, player.getUUID());
                if (TSDItems.getFondueChef(held) == null) {
                    TSDItems.setFondueChef(held, player.getUUID());
                }
                TSDUtil.give(player, new ItemStack(TSDRegistry.Items.TWILIGHT_CHEESE_FONDUE_WITH_BREAD));
                int next = servings - 1;
                level.setBlock(pos, state.setValue(SERVINGS, next), Block.UPDATE_ALL);
                if (next == 0) {
                    awardSharingAdvancements(level, held);
                }
                TSDItems.damageFondueCompanion(held, player);
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        private static void awardSharingAdvancements(Level level, ItemStack companion) {
            if (level.getServer() == null
                    || TSDItems.getFondueDinerCount(companion)
                    < TSDConfig.TWILIGHT_CHEESE_FONDUE_DINER_COUNT.get()) {
                return;
            }
            for (java.util.UUID diner : TSDItems.getFondueDiners(companion)) {
                net.minecraft.server.level.ServerPlayer player =
                        level.getServer().getPlayerList().getPlayer(diner);
                if (player != null) {
                    TSDRegistry.Triggers.GATHERED_AROUND.trigger(player);
                }
            }
            java.util.UUID chef = TSDItems.getFondueChef(companion);
            if (chef != null) {
                net.minecraft.server.level.ServerPlayer player =
                        level.getServer().getPlayerList().getPlayer(chef);
                if (player != null) {
                    TSDRegistry.Triggers.EXECUTIVE_CHEF.trigger(player);
                }
            }
        }

        private static ItemStack findFuel(Player player) {
            ItemStack main = player.getMainHandItem();
            if (isFuel(main)) {
                return main;
            }
            ItemStack off = player.getOffhandItem();
            return isFuel(off) ? off : ItemStack.EMPTY;
        }

        private static boolean isFuel(ItemStack stack) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return FIERY_BLOOD.equals(id) || FIERY_TEARS.equals(id);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            if (state.getValue(SERVINGS) == 7) {
                return List.of(new ItemStack(asItem()));
            }
            return List.of(new ItemStack(twilightforest.init.TFItems.CARMINITE.get(), 5),
                    new ItemStack(TSDRegistry.Items.FIERY_SLAG, 3));
        }
    }

}

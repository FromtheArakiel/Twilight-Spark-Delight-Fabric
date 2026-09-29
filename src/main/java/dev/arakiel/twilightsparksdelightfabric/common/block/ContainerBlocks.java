package dev.arakiel.twilightsparksdelightfabric.common.block;

import java.util.List;
import java.util.function.Supplier;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.TSDUtil;
import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import vectorwing.farmersdelight.common.registry.ModSounds;

/** Jars, crops and the shared serving container of the mod. */
public final class ContainerBlocks {
    private ContainerBlocks() {
    }

    /** Ripened pickled bracken jar that hands out one pickle at a time. */
    public static final class PickledJar extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

        public PickledJar(BlockBehaviour.Properties properties) {
            super(properties.noOcclusion().sound(SoundType.GLASS));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEVEL, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LEVEL);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(LEVEL, 0);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
            return SHAPE;
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
            int levelValue = state.getValue(LEVEL);
            if (levelValue >= 8) {
                if (!level.isClientSide) {
                    level.removeBlock(pos, false);
                    popResource(level, pos, twilightforest.init.TFItems.MASON_JAR.get().getDefaultInstance());
                    level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                TSDUtil.give(player, TSDRegistry.Items.PICKLED_BRACKEN.getDefaultInstance());
                level.setBlock(pos, state.setValue(LEVEL, levelValue + 1), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Jar that ripens into the pickled jar when the conditions are met. */
    public static final class UnripeJar extends Block implements EntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

        public UnripeJar(BlockBehaviour.Properties properties) {
            super(properties.noOcclusion().sound(SoundType.GLASS)
                    .randomTicks().pushReaction(PushReaction.DESTROY));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEVEL, 0));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TSDBlockEntities.UnripeJar(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            return null;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LEVEL);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(LEVEL, 0);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            TSDBlockEntities.UnripeJar jar = getJar(level, pos);
            if (jar == null) {
                return;
            }
            if (jar.getProgress() >= TSDBlockEntities.UnripeJar.MAX_PROGRESS) {
                ripen(level, pos, state);
                return;
            }

            int gained = rollProgress(random, getRipeningChance(level, pos, jar));
            if (gained <= 0) {
                return;
            }
            jar.addProgress(gained);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (jar.getProgress() >= TSDBlockEntities.UnripeJar.MAX_PROGRESS) {
                ripen(level, pos, level.getBlockState(pos));
            }
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
            if (held.is(twilightforest.init.TFItems.POCKET_WATCH.get())) {
                if (!level.isClientSide) {
                    ripen((ServerLevel) level, pos, state);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "twilightsparksdelightfabric.block.unripe_pickled_bracken_jar.not_ready"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        private static void ripen(ServerLevel level, BlockPos pos, BlockState state) {
            level.setBlock(pos, TSDRegistry.Blocks.PICKLED_BRACKEN_JAR.defaultBlockState()
                    .setValue(PickledJar.FACING, state.getValue(FACING))
                    .setValue(PickledJar.LEVEL, 0), Block.UPDATE_ALL);
        }

        private static TSDBlockEntities.UnripeJar getJar(Level level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof TSDBlockEntities.UnripeJar jar ? jar : null;
        }

        private static int rollProgress(RandomSource random, double chance) {
            int progress = 0;
            while (chance > 0.0D) {
                double roll = Math.min(1.0D, chance);
                if (random.nextDouble() > roll) {
                    break;
                }
                progress++;
                chance -= 1.0D;
            }
            return progress;
        }

        public static double getRipeningChance(ServerLevel level, BlockPos pos,
                                               TSDBlockEntities.UnripeJar jar) {
            double chance = TSDConfig.PICKLED_BRACKEN_JAR_BASE_RIPENING_CHANCE.get();
            BlockPos shade = findShade(level, pos);
            if (shade != null) {
                chance += level.getBlockState(shade).is(BlockTags.LEAVES)
                        ? TSDConfig.PICKLED_BRACKEN_JAR_LEAF_SHADE_BONUS.get()
                        : TSDConfig.PICKLED_BRACKEN_JAR_SHADE_BONUS.get();
                if (isCleanShadeArea(level, shade)) {
                    chance += TSDConfig.PICKLED_BRACKEN_JAR_CLEAN_AREA_BONUS.get();
                }
            }
            if (hasNearby(level, pos, twilightforest.init.TFBlocks.TIME_LOG_CORE.get())) {
                chance += TSDConfig.PICKLED_BRACKEN_JAR_MAGIC_LOG_BONUS.get();
            }
            if (hasNearby(level, pos, TSDRegistry.Blocks.LABYRINTH_MUSHROOM_COLONY)) {
                chance += TSDConfig.PICKLED_BRACKEN_JAR_MUSHROOM_COLONY_BONUS.get();
            }
            if (jar.wasPeacockFanUsed()) {
                chance += TSDConfig.PICKLED_BRACKEN_JAR_PEACOCK_FAN_BONUS.get();
            }
            if (isInAcceleratingStructure(level, pos)) {
                chance += TSDConfig.PICKLED_BRACKEN_JAR_STRUCTURE_BONUS.get();
            }
            return chance;
        }

        private static boolean isInAcceleratingStructure(ServerLevel level, BlockPos pos) {
            var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            var structures = java.util.Set.of(twilightforest.init.TFStructures.HOLLOW_HILL_SMALL,
                    twilightforest.init.TFStructures.HOLLOW_HILL_MEDIUM,
                    twilightforest.init.TFStructures.HOLLOW_HILL_LARGE,
                    twilightforest.init.TFStructures.MUSHROOM_TOWER, twilightforest.init.TFStructures.LABYRINTH,
                    twilightforest.init.TFStructures.KNIGHT_STRONGHOLD,
                    twilightforest.init.TFStructures.QUEST_GROVE);
            for (var start : level.structureManager().startsForStructure(
                    new net.minecraft.world.level.ChunkPos(pos),
                    structure -> registry.getResourceKey(structure).map(structures::contains).orElse(false))) {
                var bounds = start.getBoundingBox();
                if (pos.getX() >= bounds.minX() && pos.getX() <= bounds.maxX()
                        && pos.getZ() >= bounds.minZ() && pos.getZ() <= bounds.maxZ()) {
                    return true;
                }
            }
            return false;
        }

        private static BlockPos findShade(ServerLevel level, BlockPos pos) {
            for (int y = 1; y <= 5; y++) {
                BlockPos candidate = pos.above(y);
                if (!level.getBlockState(candidate).isAir()) {
                    return candidate;
                }
            }
            return null;
        }

        private static boolean isCleanShadeArea(ServerLevel level, BlockPos center) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockState state = level.getBlockState(center.offset(dx, 0, dz));
                    if (!state.isAir() && !isCatalystBlock(state)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean isCatalystBlock(BlockState state) {
            return state.is(TSDRegistry.Blocks.UNRIPE_PICKLED_BRACKEN_JAR)
                    || state.is(TSDRegistry.Blocks.PICKLED_BRACKEN_JAR)
                    || state.is(TSDRegistry.Blocks.LABYRINTH_MUSHROOM_COLONY)
                    || state.is(twilightforest.init.TFBlocks.TIME_LOG_CORE.get())
                    || state.is(twilightforest.init.TFBlocks.TIME_LOG.get())
                    || state.is(twilightforest.init.TFBlocks.TIME_LEAVES.get())
                    || state.is(twilightforest.init.TFBlocks.TIME_SAPLING.get());
        }

        private static boolean hasNearby(ServerLevel level, BlockPos pos, Block block) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (level.getBlockState(pos.offset(dx, 0, dz)).is(block)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Salt roasted helmet crab: the first serving consumes three bowls and
     * gives three plated servings; later servings need a knife.
     */
    public static final class SaltCrab extends FeastBlocks.Feast implements EntityBlock {
        public SaltCrab(BlockBehaviour.Properties properties) {
            super(properties, () -> TSDRegistry.Items.BOWL_OF_SALTED_CRAB_MEAT, 8, true);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return box(1, 0, 1, 15, 12, 15);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TSDBlockEntities.SharingFeast(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            return null;
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
            int servings = state.getValue(SERVINGS);
            if (servings == 0) {
                if (!level.isClientSide) {
                    level.destroyBlock(pos, true);
                    level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (servings == maxServings) {
                if (!held.is(Items.BOWL) || countBowls(player) < 3) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                if (!level.isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        consumeBowls(player, 3);
                    }
                    recordDiner(level, pos, player);
                    TSDUtil.give(player, new ItemStack(TSDRegistry.Items.BOWL_OF_SALTED_CRAB_MEAT, 3));
                    level.setBlock(pos, state.setValue(SERVINGS, servings - 1), Block.UPDATE_ALL);
                    level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!TSDUtil.isKnife(held)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                TSDUtil.give(player, new ItemStack(servings == 1
                        ? TSDRegistry.Items.SALT_ROASTED_HELMET_CRAB_CLAW
                        : TSDRegistry.Items.COOKED_HERMIT_CRAB_LEG));
                recordDiner(level, pos, player);
                if (!player.getAbilities().instabuild) {
                    held.hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }
                int next = servings - 1;
                level.setBlock(pos, state.setValue(SERVINGS, next), Block.UPDATE_ALL);
                if (next == 0) {
                    awardSharingAdvancements(level, pos);
                }
                level.playSound(null, pos, ModSounds.BLOCK_FOOD_TAKE_PORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        private static void recordDiner(Level level, BlockPos pos, Player player) {
            if (level.getBlockEntity(pos) instanceof TSDBlockEntities.SharingFeast feast) {
                feast.recordDiner(player);
            }
        }

        private static void awardSharingAdvancements(Level level, BlockPos pos) {
            if (level.getServer() == null
                    || !(level.getBlockEntity(pos) instanceof TSDBlockEntities.SharingFeast feast)
                    || feast.getDiners().size() < TSDConfig.TWILIGHT_CHEESE_FONDUE_DINER_COUNT.get()) {
                return;
            }
            for (java.util.UUID diner : feast.getDiners()) {
                var player = level.getServer().getPlayerList().getPlayer(diner);
                if (player != null) {
                    TSDRegistry.Triggers.GATHERED_AROUND.trigger(player);
                }
            }
            java.util.UUID chef = feast.getChef();
            if (chef != null) {
                var player = level.getServer().getPlayerList().getPlayer(chef);
                if (player != null) {
                    TSDRegistry.Triggers.EXECUTIVE_CHEF.trigger(player);
                }
            }
        }

        private static int countBowls(Player player) {
            int count = 0;
            if (player.getMainHandItem().is(Items.BOWL)) {
                count += player.getMainHandItem().getCount();
            }
            if (player.getOffhandItem().is(Items.BOWL)) {
                count += player.getOffhandItem().getCount();
            }
            return count;
        }

        private static void consumeBowls(Player player, int amount) {
            int left = consume(player.getMainHandItem(), amount);
            if (left > 0) {
                consume(player.getOffhandItem(), left);
            }
        }

        private static int consume(ItemStack stack, int amount) {
            int count = Math.min(amount, stack.is(Items.BOWL) ? stack.getCount() : 0);
            stack.shrink(count);
            return amount - count;
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            if (state.getValue(SERVINGS) == maxServings) {
                return List.of(new ItemStack(asItem()));
            }
            ItemStack helmet = new ItemStack(twilightforest.init.TFItems.KNIGHTMETAL_HELMET.get());
            helmet.setDamageValue(helmet.getMaxDamage() * 2 / 3);
            helmet.enchant(params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE), 1);
            return List.of(helmet,
                    new ItemStack(vectorwing.farmersdelight.common.registry.ModItems.CANVAS.get(), 2));
        }
    }

    /** Shared five stage crop colony used by the two legacy colony ingredients. */
    public static class Colony extends Block implements BonemealableBlock {
        public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
        private static final VoxelShape[] SHAPES = {
                box(5, 0, 5, 11, 6, 11),
                box(4, 0, 4, 12, 8, 12),
                box(3, 0, 3, 13, 10, 13),
                box(2, 0, 2, 14, 12, 14),
                box(1, 0, 1, 15, 14, 15)
        };

        private final Supplier<Item> harvestItem;

        public Colony(BlockBehaviour.Properties properties, Supplier<Item> harvestItem) {
            super(properties.sound(SoundType.GRASS).noCollission().randomTicks());
            this.harvestItem = harvestItem;
            registerDefaultState(stateDefinition.any().setValue(AGE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            return SHAPES[state.getValue(AGE)];
        }

        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil").equals(id);
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            return canSurvive(state, level, pos)
                    ? super.updateShape(state, direction, neighbor, level, pos, neighborPos)
                    : Blocks.AIR.defaultBlockState();
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(AGE) < 4 && canSurvive(state, level, pos) && random.nextInt(4) == 0) {
                level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.InteractionHand hand,
                                               BlockHitResult hit) {
            if (stack.is(Items.SHEARS) && state.getValue(AGE) > 0) {
                if (!level.isClientSide) {
                    popResource(level, pos, new ItemStack(harvestItem.get()));
                    level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) - 1), Block.UPDATE_CLIENTS);
                    if (!player.getAbilities().instabuild) {
                        stack.hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    }
                }
                level.playSound(player, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return state.getValue(AGE) < 4;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            level.setBlock(pos, state.setValue(AGE,
                    Math.min(4, state.getValue(AGE) + 1 + random.nextInt(2))), Block.UPDATE_CLIENTS);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
            if (state.getValue(AGE) == 4 && tool != null && tool.is(Items.SHEARS)) {
                return List.of(new ItemStack(asItem()));
            }
            return List.of(new ItemStack(harvestItem.get(), state.getValue(AGE) + 1));
        }
    }

    /** Colony item that is placed fully grown. */
    public static class ColonyItem extends net.minecraft.world.item.BlockItem {
        public ColonyItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        protected BlockState getPlacementState(BlockPlaceContext context) {
            BlockState state = super.getPlacementState(context);
            return state == null ? null : state.setValue(Colony.AGE, 4);
        }
    }
}

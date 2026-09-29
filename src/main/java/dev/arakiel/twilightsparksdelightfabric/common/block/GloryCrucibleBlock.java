package dev.arakiel.twilightsparksdelightfabric.common.block;

import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Glory crucible block.
 *
 * <p>The NeoForge edition delegated buckets to the fluid handler capability.
 * Fabric has no such capability, so bucket handling is performed here while
 * bottles, potions and Twilight Forest fluids keep their original order.</p>
 */
public final class GloryCrucibleBlock extends Block implements EntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 6);
    private static final VoxelShape LEGS = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape WALL_WEST = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WALL_EAST = Block.box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE = Shapes.or(LEGS, WALL_NORTH, WALL_SOUTH, WALL_WEST, WALL_EAST);

    public GloryCrucibleBlock(Properties properties) {
        super(properties.noOcclusion().sound(SoundType.METAL)
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TSDBlockEntities.GloryCrucible(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type != TSDRegistry.BlockEntities.GLORY_CRUCIBLE) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<TSDBlockEntities.GloryCrucible>)
                TSDBlockEntities.GloryCrucible::tick;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TSDBlockEntities.GloryCrucible crucible)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (crucible.canUseAsWaterCauldron() && cleanWithWater(player, hand, stack, crucible)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(twilightforest.init.TFItems.FIERY_BLOOD.get())) {
            return fillFromFieryItem(player, hand, stack, crucible, TSDRegistry.Fluids.FIERY_BLOOD);
        }
        if (stack.is(twilightforest.init.TFItems.FIERY_TEARS.get())) {
            return fillFromFieryItem(player, hand, stack, crucible, TSDRegistry.Fluids.FIERY_TEARS);
        }
        if (stack.getItem() instanceof PotionItem) {
            if (crucible.pourPotion(stack)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    giveResult(player, hand, stack, new ItemStack(Items.GLASS_BOTTLE));
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            ItemStack filled = crucible.bottleFieryFluid();
            if (filled.isEmpty()) {
                filled = crucible.bottlePotion();
            }
            if (!filled.isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                giveResult(player, hand, stack, filled);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (TSDBlockEntities.BucketInteraction.interact(player, hand, crucible)) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (crucible.tryProcessIngredient(player.getAbilities().instabuild ? stack.copy() : stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemInteractionResult fillFromFieryItem(Player player, InteractionHand hand,
                                                           ItemStack stack,
                                                           TSDBlockEntities.GloryCrucible crucible,
                                                           net.minecraft.world.level.material.Fluid kind) {
        int amount = TSDBlockEntities.GloryCrucible.FIERY_ITEM_MB;
        if (crucible.fill(kind, amount, false) != amount) {
            return ItemInteractionResult.CONSUME;
        }
        crucible.fill(kind, amount, true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    private static boolean cleanWithWater(Player player, InteractionHand hand, ItemStack held,
                                          TSDBlockEntities.GloryCrucible crucible) {
        if (held.is(ItemTags.DYEABLE) && held.has(DataComponents.DYED_COLOR)) {
            held.remove(DataComponents.DYED_COLOR);
            crucible.consumeBottle();
            player.awardStat(net.minecraft.stats.Stats.CLEAN_ARMOR);
            return true;
        }
        BannerPatternLayers patterns = held.getOrDefault(DataComponents.BANNER_PATTERNS,
                BannerPatternLayers.EMPTY);
        if (held.getItem() instanceof BannerItem && !patterns.layers().isEmpty()) {
            ItemStack clean = held.copyWithCount(1);
            clean.set(DataComponents.BANNER_PATTERNS, patterns.removeLast());
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                crucible.consumeBottle();
            }
            giveResult(player, hand, held, clean);
            player.awardStat(net.minecraft.stats.Stats.CLEAN_BANNER);
            return true;
        }
        return false;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !entity.isOnFire()
                || !(level.getBlockEntity(pos) instanceof TSDBlockEntities.GloryCrucible crucible)) {
            return;
        }
        if (crucible.canUseAsWaterCauldron()
                && entity.getBoundingBox().minY <= pos.getY() + crucible.getSurfaceHeight()) {
            entity.clearFire();
            crucible.consumeBottle();
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos,
                                    net.minecraft.world.level.biome.Biome.Precipitation precipitation) {
        if (!level.isClientSide && precipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN
                && level.random.nextInt(20) == 1
                && level.getBlockEntity(pos) instanceof TSDBlockEntities.GloryCrucible crucible) {
            crucible.addFluidBottle(Fluids.WATER, TSDBlockEntities.GloryCrucible.BOTTLE_MB);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TSDBlockEntities.GloryCrucible crucible
                ? crucible.getLiquidLevel() : state.getValue(LEVEL);
    }

    private static void giveResult(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        if (held.isEmpty() && player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, result);
        } else {
            player.getInventory().placeItemBackInInventory(result);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TSDBlockEntities.GloryCrucible crucible
                && crucible.canBubble() && crucible.isHeated(level, pos) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + crucible.getSurfaceHeight(),
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
}

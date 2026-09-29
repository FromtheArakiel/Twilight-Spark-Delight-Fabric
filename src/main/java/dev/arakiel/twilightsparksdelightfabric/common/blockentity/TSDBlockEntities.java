package dev.arakiel.twilightsparksdelightfabric.common.blockentity;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.TSDUtil;
import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.VoxelShape;
import vectorwing.farmersdelight.common.block.StoveBlock;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.HeatableBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;
import vectorwing.farmersdelight.refabricated.inventory.ItemStackHandler;
import vectorwing.farmersdelight.refabricated.inventory.RecipeWrapper;

/**
 * All block entities of the mod.
 *
 * <p>The giant kitchen entities extend Farmer's Delight's stove and cooking pot
 * so that recipes, containers and the upstream menus keep working, while the
 * crucible carries its own small tank implementation.</p>
 */
public final class TSDBlockEntities {
    private TSDBlockEntities() {
    }

    /** Carries the ingredient variant of a large placeable feast. */
    public static final class LargeFeast extends BlockEntity {
        private String ingredient = "";
        private boolean cooked;

        public LargeFeast(BlockPos pos, BlockState state) {
            super(TSDRegistry.BlockEntities.LARGE_FEAST, pos, state);
        }

        public void readItem(ItemStack stack) {
            ingredient = stack.getOrDefault(TSDRegistry.Components.NAGA_INGREDIENT, "");
            cooked = stack.getOrDefault(TSDRegistry.Components.COOKED_ADVANCEMENT, false);
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        public ItemStack writeItem(ItemStack stack, boolean wholeFeast) {
            if (!ingredient.isEmpty()) {
                stack.set(TSDRegistry.Components.NAGA_INGREDIENT, ingredient);
            }
            if (wholeFeast && cooked) {
                stack.set(TSDRegistry.Components.COOKED_ADVANCEMENT, true);
            }
            return stack;
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            ingredient = tag.getString("Ingredient");
            cooked = tag.getBoolean("Cooked");
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putString("Ingredient", ingredient);
            tag.putBoolean("Cooked", cooked);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveWithoutMetadata(registries);
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }

    /**
     * Persists the chef and the distinct diners of a staged feast that does not
     * use an item based serving tool.
     */
    public static final class SharingFeast extends BlockEntity {
        private static final String CHEF = "Chef";
        private static final String DINERS = "Diners";

        private UUID chef;
        private final Set<UUID> diners = new LinkedHashSet<>();

        public SharingFeast(BlockPos pos, BlockState state) {
            super(TSDRegistry.BlockEntities.SHARING_FEAST, pos, state);
        }

        public void recordDiner(Player player) {
            if (chef == null) {
                chef = player.getUUID();
            }
            diners.add(player.getUUID());
            setChanged();
        }

        public UUID getChef() {
            return chef;
        }

        public Set<UUID> getDiners() {
            return Set.copyOf(diners);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            chef = tag.hasUUID(CHEF) ? tag.getUUID(CHEF) : null;
            diners.clear();
            ListTag stored = tag.getList(DINERS, StringTag.TAG_STRING);
            for (int index = 0; index < stored.size(); index++) {
                try {
                    diners.add(UUID.fromString(stored.getString(index)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed entries from externally edited save data.
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            if (chef != null) {
                tag.putUUID(CHEF, chef);
            }
            ListTag stored = new ListTag();
            for (UUID diner : diners) {
                stored.add(StringTag.valueOf(diner.toString()));
            }
            tag.put(DINERS, stored);
        }
    }

    /** Ripening progress of the unripe pickled bracken jar. */
    public static final class UnripeJar extends BlockEntity {
        public static final int MAX_PROGRESS = 5;

        private int progress;
        private boolean peacockFanUsed;

        public UnripeJar(BlockPos pos, BlockState state) {
            super(TSDRegistry.BlockEntities.UNRIPE_PICKLED_BRACKEN_JAR, pos, state);
        }

        public int getProgress() {
            return progress;
        }

        public boolean wasPeacockFanUsed() {
            return peacockFanUsed;
        }

        public void addProgress(int amount) {
            if (amount <= 0) {
                return;
            }
            progress = Math.min(MAX_PROGRESS, progress + amount);
            setChanged();
            sync();
        }

        public void markPeacockFanUsed() {
            if (!peacockFanUsed) {
                peacockFanUsed = true;
                setChanged();
                sync();
            }
        }

        private void sync() {
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Progress", progress);
            tag.putBoolean("PeacockFanUsed", peacockFanUsed);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            progress = Mth.clamp(tag.getInt("Progress"), 0, MAX_PROGRESS);
            peacockFanUsed = tag.getBoolean("PeacockFanUsed");
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }

    /**
     * The regular giant stove has six processing positions; the 4x4 Giant's
     * Stove exposes sixteen positions while retaining Farmer's Delight's stove
     * contract.
     */
    public static class GiantStove extends AbstractStoveBlockEntity {
        public GiantStove(BlockPos pos, BlockState state) {
            super(TSDRegistry.BlockEntities.GIANT_STOVE, pos, state, RecipeType.CAMPFIRE_COOKING);
        }

        @Override
        public BlockEntityType<?> getType() {
            return TSDRegistry.BlockEntities.GIANT_STOVE;
        }

        @Override
        public boolean shouldDropItems() {
            if (level == null) {
                return false;
            }
            /*
             * Farmer's Delight only checks the controller block. The giant
             * stove has a cooking surface spanning the whole structure, so
             * every top cell must be clear before cooking can continue.
             */
            int size = getStructureSize();
            Direction facing = KitchenBlocks.Structure.controllerFacing(getBlockState());
            for (BlockPos top : KitchenBlocks.Structure.positions(worldPosition, facing, size)) {
                if (top.getY() != worldPosition.getY() + size - 1) {
                    continue;
                }
                BlockPos above = top.above();
                VoxelShape collision = level.getBlockState(above).getCollisionShape(level, above);
                if (!collision.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected int getInventorySlotCount() {
            return isLarge(getBlockState()) ? 16 : 6;
        }

        @Override
        public int getNextEmptySlot() {
            int slotLimit = isLarge(getBlockState())
                    ? Math.min(getItems().getSlotCount(),
                            Math.max(1, TSDConfig.GIANTS_STOVE_MAX_PORTIONS.get()))
                    : getItems().getSlotCount();
            for (int slot = 0; slot < slotLimit; slot++) {
                if (getItems().getStackInSlot(slot).isEmpty()) {
                    return slot;
                }
            }
            return -1;
        }

        @Override
        public Vec2 getStoveItemOffset(int slot) {
            boolean large = isLarge(getBlockState());
            int columns = large ? 4 : 3;
            int rows = (getInventorySlotCount() + columns - 1) / columns;
            float columnSpacing = large ? 0.9F : 0.6F;
            float rowSpacing = 0.8F;
            return new Vec2(
                    (columns - 1) * 0.5F * columnSpacing - (slot % columns) * columnSpacing,
                    (rows - 1) * 0.5F * rowSpacing - (slot / columns) * rowSpacing);
        }

        public static void particleTick(Level level, BlockPos pos, BlockState state, GiantStove stove) {
            ItemStackHandler items = stove.getItems();
            var random = level.random;
            Direction facing = state.getValue(StoveBlock.FACING).getOpposite();
            int size = stove.getStructureSize();
            var center = KitchenBlocks.Structure.bounds(pos, state).getCenter();
            int portionLimit = isLarge(state)
                    ? Math.min(items.getSlotCount(), Math.max(1, TSDConfig.GIANTS_STOVE_MAX_PORTIONS.get()))
                    : items.getSlotCount();
            for (int slot = 0; slot < portionLimit; slot++) {
                if (items.getStackInSlot(slot).isEmpty() || random.nextFloat() >= 0.2F) {
                    continue;
                }
                Vec2 offset = stove.getStoveItemOffset(slot);
                float xOffset = offset.x;
                float zOffset = offset.y;
                if (facing.getAxis() == Direction.Axis.Z) {
                    float swap = xOffset;
                    xOffset = zOffset;
                    zOffset = swap;
                }
                double x = center.x - facing.getStepX() * zOffset
                        + facing.getClockWise().getStepX() * xOffset;
                double z = center.z - facing.getStepZ() * zOffset
                        + facing.getClockWise().getStepZ() * xOffset;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        x, pos.getY() + size + 0.02D, z, 0.0D, 0.0005D, 0.0D);
            }
        }

        private int getStructureSize() {
            return getBlockState().getBlock() instanceof KitchenBlocks.KitchenController controller
                    ? controller.getStructureSize() : 2;
        }

        private static boolean isLarge(BlockState state) {
            return state.is(TSDRegistry.Blocks.GIANTS_STOVE);
        }
    }

    /**
     * Farmer's Delight cooking pot logic with a larger, configurable batch
     * capacity. The inherited inventory and menu are intentionally retained so
     * recipes and external item handlers keep using the upstream contract.
     */
    public static class GiantPot extends CookingPotBlockEntity {
        private static final int INPUT_SLOT_COUNT = 6;
        private long completedBatches;

        public GiantPot(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<?> getType() {
            return TSDRegistry.BlockEntities.GIANT_COOKING_POT;
        }

        @Override
        public Component getName() {
            if (getBlockState().is(TSDRegistry.Blocks.GIANTS_COOKING_POT)) {
                return Component.translatable("twilightsparksdelightfabric.container.giants_cooking_pot");
            }
            return Component.translatable("twilightsparksdelightfabric.container.giant_cooking_pot");
        }

        public static void cookingTick(Level level, BlockPos pos, BlockState state, GiantPot pot) {
            if (level.isClientSide) {
                CookingPotBlockEntity.cookingTick(level, pos, state, pot);
                return;
            }

            Optional<RecipePlan> initialPlan = findRecipePlan(level, pot);
            if (initialPlan.isEmpty() || !pot.isHeated()
                    || !pot.canProcessResult(initialPlan.get().recipe().value())) {
                CookingPotBlockEntity.cookingTick(level, pos, state, pot);
                return;
            }

            RecipeHolder<CookingPotRecipe> recipe = initialPlan.get().recipe();
            int completed = pot.processOneBatch(level, pos, state, initialPlan.get(), false);
            if (completed == 0) {
                return;
            }

            /*
             * A normal pot finishes one batch per cooking tick. The enlarged
             * pot keeps the same cooking time but chains additional complete
             * batches while inputs and output capacity allow it, and every
             * batch still enters the upstream implementation.
             */
            for (int batch = completed; batch < pot.getBatchLimit(); batch++) {
                Optional<RecipePlan> nextPlan = findRecipePlan(level, pot);
                if (nextPlan.isEmpty() || !nextPlan.get().recipe().id().equals(recipe.id())
                        || !pot.canProcessResult(nextPlan.get().recipe().value())) {
                    break;
                }
                pot.cookingPotData.set(0, Math.max(0, recipe.value().getCookTime() - 1));
                pot.cookingPotData.set(1, Math.max(1, recipe.value().getCookTime()));
                if (pot.processOneBatch(level, pos, state, nextPlan.get(), true) == 0) {
                    break;
                }
            }
        }

        public static void animationTick(Level level, BlockPos pos, BlockState state, GiantPot pot) {
            if (!pot.isHeated()) {
                return;
            }
            var bounds = KitchenBlocks.Structure.bounds(pos, state);
            var center = bounds.getCenter();
            double size = bounds.getXsize();
            var random = level.random;
            if (random.nextFloat() < 0.2F) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP,
                        center.x + (random.nextDouble() - 0.5) * 0.6 * size, pos.getY() + 0.7 * size,
                        center.z + (random.nextDouble() - 0.5) * 0.6 * size, 0, 0, 0);
            }
            if (random.nextFloat() < 0.05F) {
                level.addParticle(vectorwing.farmersdelight.common.registry.ModParticleTypes.STEAM.get(),
                        center.x + (random.nextDouble() - 0.5) * 0.4 * size, pos.getY() + 0.65 * size,
                        center.z + (random.nextDouble() - 0.5) * 0.4 * size, 0, 0.015, 0);
            }
        }

        public int getBatchLimit() {
            return Math.max(1, getBlockState().is(TSDRegistry.Blocks.GIANTS_COOKING_POT)
                    ? TSDConfig.GIANTS_COOKING_POT_MAX_BATCHES.get()
                    : TSDConfig.GIANT_COOKING_POT_MAX_BATCHES.get());
        }

        @Override
        protected boolean canCook(CookingPotRecipe recipe) {
            return canProcessResult(recipe) && super.canCook(recipe);
        }

        @Override
        public void setRecipeUsed(RecipeHolder<?> recipe) {
            super.setRecipeUsed(recipe);
            if (recipe != null) {
                completedBatches++;
            }
        }

        private boolean canProcessResult(CookingPotRecipe recipe) {
            ItemStack result = recipe.assemble(new RecipeWrapper(getInventory()), level.registryAccess());
            if (result.isEmpty()) {
                return false;
            }
            ItemStack meal = getMeal();
            ItemStack output = getInventory().getStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT);
            if ((!meal.isEmpty() && !ItemStack.isSameItem(meal, result))
                    || (!output.isEmpty() && !ItemStack.isSameItem(output, result))) {
                return false;
            }
            int stored = (meal.isEmpty() ? 0 : meal.getCount())
                    + (output.isEmpty() ? 0 : output.getCount());
            return stored + result.getCount() <= result.getMaxStackSize();
        }

        private static Optional<RecipePlan> findRecipePlan(Level level, GiantPot pot) {
            ItemStackHandler inventory = pot.getInventory();
            Optional<RecipeHolder<CookingPotRecipe>> direct = level.getRecipeManager().getRecipeFor(
                    ModRecipeTypes.COOKING.get(), new RecipeWrapper(inventory), level);
            if (direct.isPresent()) {
                Optional<RecipePlan> directPlan = createPlan(direct.get(), inventory);
                if (directPlan.isPresent()) {
                    return directPlan;
                }
            }

            for (RecipeHolder<CookingPotRecipe> candidate
                    : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get())) {
                Optional<RecipePlan> plan = createPlan(candidate, inventory);
                if (plan.isPresent()) {
                    return plan;
                }
            }
            return Optional.empty();
        }

        private static Optional<RecipePlan> createPlan(RecipeHolder<CookingPotRecipe> recipe,
                                                       ItemStackHandler inventory) {
            int[] sources = new int[recipe.value().getIngredients().size()];
            ItemStack[] original = new ItemStack[INPUT_SLOT_COUNT];
            int nonEmpty = 0;
            for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                original[slot] = inventory.getStackInSlot(slot).copy();
                if (!original[slot].isEmpty()) {
                    nonEmpty++;
                }
            }
            if (sources.length == 0 || sources.length > INPUT_SLOT_COUNT || nonEmpty > sources.length) {
                return Optional.empty();
            }
            int[] remaining = new int[INPUT_SLOT_COUNT];
            int[] consumed = new int[INPUT_SLOT_COUNT];
            for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                remaining[slot] = original[slot].getCount();
            }
            if (!assignIngredients(recipe.value().getIngredients(), original, remaining,
                    consumed, sources, 0)) {
                return Optional.empty();
            }
            return Optional.of(new RecipePlan(recipe, original, consumed, sources));
        }

        private static boolean assignIngredients(java.util.List<Ingredient> ingredients, ItemStack[] inputs,
                                                 int[] remaining, int[] consumed, int[] sources,
                                                 int ingredientIndex) {
            if (ingredientIndex >= ingredients.size()) {
                for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                    if (!inputs[slot].isEmpty() && consumed[slot] <= 0) {
                        return false;
                    }
                }
                return true;
            }
            Ingredient ingredient = ingredients.get(ingredientIndex);
            for (int pass = 0; pass < 2; pass++) {
                for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                    boolean used = consumed[slot] > 0;
                    if ((pass == 0 && used) || (pass == 1 && !used)
                            || remaining[slot] <= 0 || !ingredient.test(inputs[slot])) {
                        continue;
                    }
                    remaining[slot]--;
                    consumed[slot]++;
                    sources[ingredientIndex] = slot;
                    if (assignIngredients(ingredients, inputs, remaining, consumed, sources,
                            ingredientIndex + 1)) {
                        return true;
                    }
                    remaining[slot]++;
                    consumed[slot]--;
                }
            }
            return false;
        }

        private int processOneBatch(Level level, BlockPos pos, BlockState state,
                                    RecipePlan plan, boolean forceCompletion) {
            ItemStackHandler inventory = getInventory();
            long batchesBefore = completedBatches;
            for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
            for (int ingredient = 0; ingredient < plan.sources().length; ingredient++) {
                ItemStack one = plan.originalInputs()[plan.sources()[ingredient]].copy();
                one.setCount(1);
                inventory.setStackInSlot(ingredient, one);
            }

            if (forceCompletion) {
                cookingPotData.set(0, Math.max(0, plan.recipe().value().getCookTime() - 1));
                cookingPotData.set(1, Math.max(1, plan.recipe().value().getCookTime()));
            }
            try {
                CookingPotBlockEntity.cookingTick(level, pos, state, this);
            } finally {
                // Read the real remaining logical ingredients: upstream hooks
                // can retain a catalyst or change the consumed amount.
                int[] consumed = new int[INPUT_SLOT_COUNT];
                if (completedBatches > batchesBefore) {
                    for (int ingredient = 0; ingredient < plan.sources().length; ingredient++) {
                        if (inventory.getStackInSlot(ingredient).isEmpty()) {
                            consumed[plan.sources()[ingredient]]++;
                        }
                    }
                }
                for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                    ItemStack restored = plan.originalInputs()[slot].copy();
                    restored.shrink(consumed[slot]);
                    inventory.setStackInSlot(slot, restored);
                }
            }
            setChanged();
            return completedBatches > batchesBefore ? 1 : 0;
        }

        private record RecipePlan(RecipeHolder<CookingPotRecipe> recipe, ItemStack[] originalInputs,
                                  int[] consumedCounts, int[] sources) {
        }
    }

    /**
     * Glory crucible: a small tank that stores one fluid at a time and can
     * convert potions as well as heat cooking ingredients.
     *
     * <p>The original NeoForge implementation exposed an {@code IFluidHandler}
     * capability. Fabric has no equivalent capability interface, so the same
     * tank is implemented directly and bucket, bottle and potion handling is
     * performed by the block. Amounts are kept in thirds of a millibucket so
     * that three bottles fill one bucket exactly.</p>
     */
    public static final class GloryCrucible extends BlockEntity implements HeatableBlockEntity {
        // Thirds of a millibucket preserve the exact three-bottles-per-bucket ratio.
        public static final int UNITS_PER_MB = 3;
        public static final int BOTTLE_UNITS = 1000;
        public static final int BOTTLE_MB = BOTTLE_UNITS / UNITS_PER_MB;
        // One Twilight Forest blood/tears item supplies a full bucket.
        public static final int FIERY_ITEM_MB = 1000;

        private Fluid fluid = Fluids.EMPTY;
        private int volume;
        private ItemStack potion = ItemStack.EMPTY;
        private ItemStack brewingTarget = ItemStack.EMPTY;
        private int brewingProgress;
        private int brewingDuration;

        public GloryCrucible(BlockPos pos, BlockState state) {
            super(TSDRegistry.BlockEntities.GLORY_CRUCIBLE, pos, state);
        }

        public Fluid getFluidKind() {
            if (!potion.isEmpty()) {
                return Fluids.WATER;
            }
            return volume <= 0 ? Fluids.EMPTY : fluid;
        }

        public int getFluidAmountMb() {
            return volume / UNITS_PER_MB;
        }

        public int getVolumeUnits() {
            return volume;
        }

        public int getLiquidLevel() {
            return volume == 0 ? 0 : Math.min(6, (volume * 6 + capacityUnits() - 1) / capacityUnits());
        }

        public double getSurfaceHeight() {
            return (6.0D + 9.0D * volume / capacityUnits()) / 16.0D;
        }

        public boolean isBrewing() {
            return !brewingTarget.isEmpty();
        }

        public boolean hasPotion() {
            return !potion.isEmpty();
        }

        public int getPotionColor(float partialTick) {
            int start = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            if (!isBrewing()) {
                return start | 0xFF000000;
            }
            int end = brewingTarget.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            float fraction = Mth.clamp((brewingProgress + partialTick) / Math.max(1, brewingDuration), 0.0F, 1.0F);
            int red = (int) Mth.lerp(fraction, (start >> 16) & 255, (end >> 16) & 255);
            int green = (int) Mth.lerp(fraction, (start >> 8) & 255, (end >> 8) & 255);
            int blue = (int) Mth.lerp(fraction, start & 255, end & 255);
            return 0xFF000000 | red << 16 | green << 8 | blue;
        }

        public boolean isPlainWater() {
            return potion.isEmpty() && fluid == Fluids.WATER && volume > 0;
        }

        public boolean canUseAsWaterCauldron() {
            return isPlainWater() && !isBrewing() && volume >= BOTTLE_UNITS;
        }

        /** Adds one bottle worth of fluid, used for water bottles and rain. */
        public boolean addFluidBottle(Fluid kind, int millibuckets) {
            if (!canAccept(kind, millibuckets) || capacityUnits() - volume < BOTTLE_UNITS) {
                return false;
            }
            if (volume == 0) {
                fluid = kind;
            }
            volume += BOTTLE_UNITS;
            markAndSync();
            return true;
        }

        public boolean pourPotion(ItemStack input) {
            if (!(input.getItem() instanceof PotionItem) || isBrewing()
                    || capacityUnits() - volume < BOTTLE_UNITS) {
                return false;
            }
            if (ItemStack.isSameItemSameComponents(input, waterBottle())) {
                return addFluidBottle(Fluids.WATER, BOTTLE_MB);
            }
            if (volume > 0 && (potion.isEmpty() || !ItemStack.isSameItemSameComponents(potion, input))) {
                return false;
            }
            potion = input.copyWithCount(1);
            fluid = Fluids.EMPTY;
            volume += BOTTLE_UNITS;
            markAndSync();
            return true;
        }

        public ItemStack bottlePotion() {
            if (isBrewing() || volume < BOTTLE_UNITS || (potion.isEmpty() && !isPlainWater())) {
                return ItemStack.EMPTY;
            }
            ItemStack output = potion.isEmpty() ? waterBottle() : potion.copyWithCount(1);
            consumeBottle();
            return output;
        }

        public ItemStack bottleFieryFluid() {
            int units = FIERY_ITEM_MB * UNITS_PER_MB;
            if (isBrewing() || hasPotion() || volume < units) {
                return ItemStack.EMPTY;
            }
            ItemStack output;
            if (fluid == TSDRegistry.Fluids.FIERY_BLOOD) {
                output = new ItemStack(twilightforest.init.TFItems.FIERY_BLOOD.get());
            } else if (fluid == TSDRegistry.Fluids.FIERY_TEARS) {
                output = new ItemStack(twilightforest.init.TFItems.FIERY_TEARS.get());
            } else {
                return ItemStack.EMPTY;
            }
            volume -= units;
            clearIfEmpty();
            markAndSync();
            return output;
        }

        public boolean consumeBottle() {
            if (isBrewing() || volume < BOTTLE_UNITS) {
                return false;
            }
            volume -= BOTTLE_UNITS;
            clearIfEmpty();
            markAndSync();
            return true;
        }

        /** Fills the tank from a bucket or another container. */
        public int fill(Fluid kind, int millibuckets, boolean execute) {
            if (!canAccept(kind, millibuckets)) {
                return 0;
            }
            int accepted = Math.min(millibuckets, Math.max(0, capacityUnits() - volume) / UNITS_PER_MB);
            if (accepted > 0 && execute) {
                if (volume == 0) {
                    fluid = kind;
                }
                volume += accepted * UNITS_PER_MB;
                markAndSync();
            }
            return accepted;
        }

        /** Drains the tank into a bucket. */
        public int drain(Fluid kind, int millibuckets, boolean execute) {
            if (kind != getFluidKind() || millibuckets <= 0 || isBrewing() || !potion.isEmpty()) {
                return 0;
            }
            int drained = Math.min(millibuckets, getFluidAmountMb());
            if (drained <= 0) {
                return 0;
            }
            if (execute) {
                volume -= drained * UNITS_PER_MB;
                clearIfEmpty();
                markAndSync();
            }
            return drained;
        }

        public static void tick(Level level, BlockPos pos, BlockState state, GloryCrucible crucible) {
            if (crucible.volume <= 0) {
                return;
            }
            if (crucible.isBrewing()) {
                if (crucible.isHeated(level, pos)) {
                    crucible.brewingProgress++;
                    if (!level.isClientSide) {
                        if (crucible.brewingProgress >= crucible.brewingDuration) {
                            crucible.potion = crucible.brewingTarget;
                            crucible.fluid = Fluids.EMPTY;
                            crucible.brewingTarget = ItemStack.EMPTY;
                            crucible.brewingProgress = 0;
                            level.levelEvent(1035, pos, 0);
                            crucible.markAndSync();
                        } else if (crucible.brewingProgress % 20 == 0) {
                            crucible.markAndSync();
                        }
                    }
                }
                return;
            }
            if (level.isClientSide) {
                return;
            }
            AABB bounds = new AABB(pos.getX() + 0.125D, pos.getY() + 0.25D, pos.getZ() + 0.125D,
                    pos.getX() + 0.875D, pos.getY() + crucible.getSurfaceHeight() + 0.05D,
                    pos.getZ() + 0.875D);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds,
                    entity -> entity.isAlive() && !entity.getItem().isEmpty())) {
                if (crucible.tryProcessIngredient(item.getItem())) {
                    if (item.getItem().isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(item.getItem().copy());
                    }
                    break;
                }
            }
        }

        public boolean tryProcessIngredient(ItemStack ingredient) {
            if (level == null || level.isClientSide || volume == 0 || isBrewing() || ingredient.isEmpty()) {
                return false;
            }
            if (potion.isEmpty() && TSDRegistry.Fluids.isHeatingFluid(fluid)) {
                ItemStack output = findHeatingResult(level, ingredient);
                if (output.isEmpty()) {
                    return false;
                }
                consumeIngredient(ingredient);
                eject(output);
                return true;
            }
            if (!isHeated(level, worldPosition) || (potion.isEmpty() && !isPlainWater())) {
                return false;
            }
            ItemStack input = potion.isEmpty() ? waterBottle() : potion;
            if (!level.potionBrewing().hasMix(input, ingredient)) {
                return false;
            }
            ItemStack target = level.potionBrewing().mix(ingredient, input.copyWithCount(1));
            if (!(target.getItem() instanceof PotionItem) || ItemStack.isSameItemSameComponents(input, target)) {
                return false;
            }
            consumeIngredient(ingredient);
            potion = input.copyWithCount(1);
            fluid = Fluids.EMPTY;
            brewingTarget = target.copyWithCount(1);
            brewingDuration = TSDConfig.GLORY_CRUCIBLE_BREWING_TICKS.get();
            brewingProgress = 0;
            markAndSync();
            return true;
        }

        public static ItemStack findHeatingResult(Level level, ItemStack stack) {
            SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
            ItemStack smelting = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level)
                    .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
                    .orElse(ItemStack.EMPTY);
            return smelting.isEmpty()
                    ? level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level)
                            .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
                            .orElse(ItemStack.EMPTY)
                    : smelting;
        }

        private void consumeIngredient(ItemStack input) {
            Item remainderItem = input.getItem().getCraftingRemainingItem();
            ItemStack remainder = remainderItem == null ? ItemStack.EMPTY : new ItemStack(remainderItem);
            input.shrink(1);
            eject(remainder);
        }

        private void eject(ItemStack output) {
            if (level == null || output.isEmpty()) {
                return;
            }
            ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.05D, worldPosition.getZ() + 0.5D, output.copy());
            entity.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.12D, 0.18D,
                    (level.random.nextDouble() - 0.5D) * 0.12D);
            entity.setPickUpDelay(10);
            level.addFreshEntity(entity);
        }

        public boolean canBubble() {
            return volume > 0 && fluid != Fluids.LAVA && !TSDRegistry.Fluids.isHeatingFluid(fluid);
        }

        private int capacityUnits() {
            return getTankCapacityMb() * UNITS_PER_MB;
        }

        public int getTankCapacityMb() {
            return Math.max(1000, TSDConfig.GLORY_CRUCIBLE_CAPACITY_MB.get() / 1000 * 1000);
        }

        private boolean canAccept(Fluid kind, int millibuckets) {
            return kind != Fluids.EMPTY && millibuckets > 0 && !isBrewing() && potion.isEmpty()
                    && (volume == 0 || fluid == kind);
        }

        private static ItemStack waterBottle() {
            return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        }

        private void clearIfEmpty() {
            if (volume <= 0) {
                volume = 0;
                fluid = Fluids.EMPTY;
                potion = ItemStack.EMPTY;
                brewingTarget = ItemStack.EMPTY;
                brewingProgress = 0;
            }
        }

        private void markAndSync() {
            setChanged();
            if (level == null || level.isClientSide) {
                return;
            }
            BlockState current = level.getBlockState(worldPosition);
            if (current.getBlock() instanceof dev.arakiel.twilightsparksdelightfabric.common.block.GloryCrucibleBlock) {
                BlockState updated = current.setValue(
                        dev.arakiel.twilightsparksdelightfabric.common.block.GloryCrucibleBlock.LEVEL,
                        getLiquidLevel());
                if (updated != current) {
                    level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
                }
                // Send the payload even for changes within one quantized level.
                level.sendBlockUpdated(worldPosition, updated, updated, 3);
                level.updateNeighbourForOutputSignal(worldPosition, updated.getBlock());
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("VolumeUnits", volume);
            if (fluid != Fluids.EMPTY) {
                tag.putString("Fluid", net.minecraft.core.registries.BuiltInRegistries.FLUID
                        .getKey(fluid).toString());
            }
            if (!potion.isEmpty()) {
                tag.put("Potion", potion.save(registries));
            }
            if (isBrewing()) {
                tag.put("BrewingTarget", brewingTarget.save(registries));
                tag.putInt("BrewingProgress", brewingProgress);
                tag.putInt("BrewingDuration", brewingDuration);
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            fluid = Fluids.EMPTY;
            if (tag.contains("Fluid")) {
                ResourceLocation id = ResourceLocation.tryParse(tag.getString("Fluid"));
                if (id != null && net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)) {
                    fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
                }
            }
            volume = Mth.clamp(tag.contains("VolumeUnits") ? tag.getInt("VolumeUnits")
                    : (fluid == Fluids.EMPTY ? 0 : FIERY_ITEM_MB * UNITS_PER_MB), 0, capacityUnits());
            potion = ItemStack.parseOptional(registries, tag.getCompound("Potion"));
            brewingTarget = ItemStack.parseOptional(registries, tag.getCompound("BrewingTarget"));
            brewingProgress = Math.max(0, tag.getInt("BrewingProgress"));
            brewingDuration = Math.max(1, tag.getInt("BrewingDuration"));
            if (!potion.isEmpty()) {
                potion.setCount(1);
                fluid = Fluids.EMPTY;
            } else if (fluid == Fluids.EMPTY) {
                volume = 0;
            }
            clearIfEmpty();
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }

    /** Small helper describing the fluids the crucible accepts from buckets. */
    public static final class BucketInteraction {
        private BucketInteraction() {
        }

        public static boolean interact(Player player, net.minecraft.world.InteractionHand hand,
                                       GloryCrucible crucible) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.WATER_BUCKET)) {
                return fillFromBucket(player, hand, held, crucible, Fluids.WATER, Items.BUCKET);
            }
            if (held.is(Items.LAVA_BUCKET)) {
                return fillFromBucket(player, hand, held, crucible, Fluids.LAVA, Items.BUCKET);
            }
            if (held.is(Items.BUCKET)) {
                ItemStack filled = drainToBucket(crucible);
                if (filled.isEmpty()) {
                    return false;
                }
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    TSDUtil.give(player, filled);
                }
                return true;
            }
            return false;
        }

        private static boolean fillFromBucket(Player player, net.minecraft.world.InteractionHand hand,
                                              ItemStack held, GloryCrucible crucible,
                                              Fluid kind, Item emptyContainer) {
            if (crucible.fill(kind, 1000, false) < 1000) {
                return false;
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                TSDUtil.give(player, new ItemStack(emptyContainer));
            }
            crucible.fill(kind, 1000, true);
            return true;
        }

        private static ItemStack drainToBucket(GloryCrucible crucible) {
            Fluid kind = crucible.getFluidKind();
            int available = crucible.drain(kind, 1000, false);
            if (available < 1000) {
                return ItemStack.EMPTY;
            }
            if (kind == Fluids.WATER) {
                crucible.drain(kind, 1000, true);
                return new ItemStack(Items.WATER_BUCKET);
            }
            if (kind == Fluids.LAVA) {
                crucible.drain(kind, 1000, true);
                return new ItemStack(Items.LAVA_BUCKET);
            }
            ItemStack fiery = crucible.bottleFieryFluid();
            return fiery;
        }
    }

}

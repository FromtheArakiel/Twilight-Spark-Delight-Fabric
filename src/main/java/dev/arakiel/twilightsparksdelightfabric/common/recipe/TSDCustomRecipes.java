package dev.arakiel.twilightsparksdelightfabric.common.recipe;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.experiment.Experiment250Logic;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.ingredient.ChanceResult;
import vectorwing.farmersdelight.refabricated.inventory.RecipeWrapper;

/** All custom recipe serializers of the mod, kept together for reviewability. */
public final class TSDCustomRecipes {
    private TSDCustomRecipes() {
    }

    /** Dynamic Experiment 250 replication performed on the crafting table. */
    public static final class Experiment250Replication implements CraftingRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            return findMatch(input) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            Match match = findMatch(input);
            if (match == null) {
                return ItemStack.EMPTY;
            }
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(match.outputId());
            if (item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, match.result().outputCount());
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            Match match = findMatch(input);
            if (match == null) {
                return remaining;
            }
            ItemStack experiment = input.getItem(match.experimentSlot()).copyWithCount(1);
            if (Experiment250Logic.consume(experiment, match.result())) {
                remaining.set(match.experimentSlot(), experiment);
            }
            return remaining;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TSDRegistry.Serials.EXPERIMENT_250_REPLICATION;
        }

        @Override
        public CraftingBookCategory category() {
            return CraftingBookCategory.MISC;
        }

        @Override
        public NonNullList<Ingredient> getIngredients() {
            return NonNullList.create();
        }

        @Override
        public boolean isSpecial() {
            return true;
        }

        private static Match findMatch(CraftingInput input) {
            if (TSDConfig.EXPERIMENT_WORKSTATION.get() != TSDConfig.ExperimentWorkstation.CRAFTING_TABLE) {
                return null;
            }
            int experimentSlot = -1;
            int inputSlot = -1;
            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack stack = input.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() instanceof TSDItems.Experiment250 && experimentSlot < 0) {
                    experimentSlot = slot;
                } else if (inputSlot < 0) {
                    inputSlot = slot;
                } else {
                    return null;
                }
            }
            if (experimentSlot < 0 || inputSlot < 0) {
                return null;
            }
            ItemStack experiment = input.getItem(experimentSlot);
            ResourceLocation outputId = Experiment250Logic.resolveTarget(experiment, input.getItem(inputSlot));
            Experiment250Logic.ReplicationResult result = Experiment250Logic.calculate(experiment, outputId);
            return result.valid() ? new Match(experimentSlot, outputId, result) : null;
        }

        private record Match(int experimentSlot, ResourceLocation outputId,
                             Experiment250Logic.ReplicationResult result) {
        }

        public static final class Serializer implements RecipeSerializer<Experiment250Replication> {
            private static final Experiment250Replication INSTANCE = new Experiment250Replication();
            private static final MapCodec<Experiment250Replication> CODEC = MapCodec.unit(INSTANCE);
            private static final StreamCodec<RegistryFriendlyByteBuf, Experiment250Replication> STREAM_CODEC =
                    StreamCodec.unit(INSTANCE);

            @Override
            public MapCodec<Experiment250Replication> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, Experiment250Replication> streamCodec() {
                return STREAM_CODEC;
            }
        }
    }

    /** Shapeless recipe whose water container survives the craft. */
    public static final class ReusableWaterShapeless extends CustomRecipe {
        public ReusableWaterShapeless() {
            super(CraftingBookCategory.MISC);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            int flour = -1;
            int water = -1;
            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack stack = input.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.is(TSDRegistry.Items.LIVEROOT_FLOUR) && flour < 0) {
                    flour = slot;
                } else if (isWaterContainer(stack) && water < 0) {
                    water = slot;
                } else {
                    return false;
                }
            }
            return flour >= 0 && water >= 0;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return matches(input, null) ? TSDRegistry.Items.LIVEROOT_DOUGH.getDefaultInstance() : ItemStack.EMPTY;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            for (int slot = 0; slot < input.size(); slot++) {
                if (isWaterContainer(input.getItem(slot))) {
                    remaining.set(slot, input.getItem(slot).copyWithCount(1));
                }
            }
            return remaining;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TSDRegistry.Serials.REUSABLE_WATER_SHAPELESS;
        }

        @Override
        public NonNullList<Ingredient> getIngredients() {
            return NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(TSDRegistry.Items.LIVEROOT_FLOUR),
                    WaterSourceIngredient.INSTANCE.toVanilla());
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return new ItemStack(TSDRegistry.Items.LIVEROOT_DOUGH);
        }

        private static boolean isWaterContainer(ItemStack stack) {
            // The same rule the pot recipes use through the custom ingredient.
            return WaterSourceIngredient.isWaterSource(stack);
        }

        public static final class Serializer implements RecipeSerializer<ReusableWaterShapeless> {
            private static final ReusableWaterShapeless INSTANCE = new ReusableWaterShapeless();
            private static final MapCodec<ReusableWaterShapeless> CODEC = MapCodec.unit(INSTANCE);
            private static final StreamCodec<RegistryFriendlyByteBuf, ReusableWaterShapeless> STREAM_CODEC =
                    StreamCodec.unit(INSTANCE);

            @Override
            public MapCodec<ReusableWaterShapeless> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, ReusableWaterShapeless> streamCodec() {
                return STREAM_CODEC;
            }
        }
    }

    /** Shapeless recipe that keeps the Twilight Forest pocket watch. */
    public static final class KeepingItemShapeless extends ShapelessRecipe {
        private KeepingItemShapeless(ShapelessRecipe recipe) {
            super(recipe.getGroup(), recipe.category(), recipe.getResultItem(null), recipe.getIngredients());
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> result = super.getRemainingItems(input);
            for (int slot = 0; slot < input.size(); slot++) {
                if (input.getItem(slot).is(twilightforest.init.TFItems.POCKET_WATCH.get())) {
                    result.set(slot, input.getItem(slot).copyWithCount(1));
                }
            }
            return result;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TSDRegistry.Serials.KEEPING_ITEM_SHAPELESS;
        }

        public static final class Serializer implements RecipeSerializer<KeepingItemShapeless> {
            private static final ShapelessRecipe.Serializer BASE = new ShapelessRecipe.Serializer();
            private static final MapCodec<KeepingItemShapeless> CODEC =
                    BASE.codec().xmap(KeepingItemShapeless::new, recipe -> recipe);
            private static final StreamCodec<RegistryFriendlyByteBuf, KeepingItemShapeless> STREAM_CODEC =
                    BASE.streamCodec().map(KeepingItemShapeless::new, recipe -> recipe);

            @Override
            public MapCodec<KeepingItemShapeless> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, KeepingItemShapeless> streamCodec() {
                return STREAM_CODEC;
            }
        }
    }

    /** Cutting board recipe that rolls the configured helmet crab bundles. */
    public static final class HelmetCrabCutting extends CuttingBoardRecipe {
        private HelmetCrabCutting(CuttingBoardRecipe recipe) {
            super(recipe.getGroup(), recipe.getIngredients().getFirst(), recipe.getTool(),
                    recipe.getRollableResults(), recipe.getSoundEvent());
        }

        @Override
        public NonNullList<ChanceResult> getRollableResults() {
            NonNullList<ChanceResult> results = NonNullList.create();
            results.addAll(super.getRollableResults());
            if (results.size() >= 3) {
                results.set(1, new ChanceResult(results.get(1).stack(),
                        TSDConfig.HELMET_CRAB_CUTTING_EXTRA_LEGS_CHANCE.get().floatValue()));
                results.set(2, new ChanceResult(results.get(2).stack(),
                        TSDConfig.HELMET_CRAB_CUTTING_ARMOR_CHANCE.get().floatValue()));
            }
            return results;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TSDRegistry.Serials.HELMET_CRAB_CUTTING;
        }

        @Override
        public List<ItemStack> rollResults(RandomSource random, int fortuneLevel, RecipeWrapper inventory) {
            List<ItemStack> results = new ArrayList<>();
            double fortune = vectorwing.farmersdelight.common.Configuration.CUTTING_BOARD_FORTUNE_BONUS.get()
                    * fortuneLevel;
            // Each optional bundle is a single roll: three legs or five clusters.
            for (ChanceResult result : getRollableResults()) {
                if (random.nextDouble() < result.chance() + fortune) {
                    results.add(result.stack().copy());
                }
            }
            return results;
        }

        public static final class Serializer implements RecipeSerializer<HelmetCrabCutting> {
            private static final CuttingBoardRecipe.Serializer BASE = new CuttingBoardRecipe.Serializer();
            private static final MapCodec<HelmetCrabCutting> CODEC =
                    BASE.codec().xmap(HelmetCrabCutting::new, recipe -> recipe);
            private static final StreamCodec<RegistryFriendlyByteBuf, HelmetCrabCutting> STREAM_CODEC =
                    BASE.streamCodec().map(HelmetCrabCutting::new, recipe -> recipe);

            @Override
            public MapCodec<HelmetCrabCutting> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, HelmetCrabCutting> streamCodec() {
                return STREAM_CODEC;
            }
        }
    }
}

package dev.arakiel.twilightsparksdelightfabric.client.integration.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.common.experiment.Experiment250Logic;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Data of every JEI page of the mod.
 *
 * <p>The NeoForge edition showed the crucible as a fluid recipe. Fabric JEI has
 * no shared fluid ingredient type, so the fiery fluids are listed through their
 * Twilight Forest items, which are exactly the items the block accepts.</p>
 */
public final class TSDJeiRecipes {
    public record Pickling(ItemStack unripe, ItemStack finished, List<ItemStack> catalysts) {
    }

    public record Crucible(List<ItemStack> inputs, List<ItemStack> fluids, List<ItemStack> reagents,
                           List<ItemStack> outputs, boolean brewing) {
    }

    public record Upgrade(int inputCount, ItemStack input, ItemStack output) {
    }

    public record Row(int level, ItemStack experiment, ItemStack output, double cost, double batchCost) {
    }

    public record Differentiation(ResourceLocation item, List<Row> rows) {
    }

    public record Drop(ResourceLocation entity, ItemStack output, boolean burning, boolean knifeOnly,
                       boolean extraLegSlot, boolean transformedRabbit) {
    }

    public record Harvest(ItemStack plant, ItemStack result) {
    }

    private TSDJeiRecipes() {
    }

    // ------------------------------------------------------------------
    // Pickling
    // ------------------------------------------------------------------

    public static Pickling pickling() {
        List<ItemStack> catalysts = new ArrayList<>();
        catalysts.add(TSDRegistry.Blocks.UNRIPE_PICKLED_BRACKEN_JAR.asItem().getDefaultInstance());
        catalysts.add(TSDRegistry.Blocks.PICKLED_BRACKEN_JAR.asItem().getDefaultInstance());
        catalysts.add(new ItemStack(twilightforest.init.TFBlocks.TIME_LOG_CORE.get()));
        catalysts.add(TSDRegistry.Blocks.LABYRINTH_MUSHROOM_COLONY.asItem().getDefaultInstance());
        catalysts.add(new ItemStack(twilightforest.init.TFItems.PEACOCK_FEATHER_FAN.get()));
        return new Pickling(
                TSDRegistry.Blocks.UNRIPE_PICKLED_BRACKEN_JAR.asItem().getDefaultInstance(),
                TSDRegistry.Blocks.PICKLED_BRACKEN_JAR.asItem().getDefaultInstance(),
                List.copyOf(catalysts));
    }

    // ------------------------------------------------------------------
    // Glory crucible
    // ------------------------------------------------------------------

    public static List<Crucible> crucibleHeating() {
        List<Crucible> result = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return result;
        }
        List<ItemStack> candidates = new ArrayList<>();
        for (var holder : level.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            if (recipe.getType() != RecipeType.SMELTING
                    && recipe.getType() != RecipeType.CAMPFIRE_COOKING) {
                continue;
            }
            for (var ingredient : recipe.getIngredients()) {
                for (ItemStack input : ingredient.getItems()) {
                    if (candidates.stream().noneMatch(stack ->
                            ItemStack.isSameItemSameComponents(stack, input))) {
                        candidates.add(input.copyWithCount(1));
                    }
                }
            }
        }
        List<ItemStack> fluids = List.of(
                new ItemStack(twilightforest.init.TFItems.FIERY_BLOOD.get()),
                new ItemStack(twilightforest.init.TFItems.FIERY_TEARS.get()));
        for (ItemStack input : candidates) {
            // The same resolver as the block, including the smelting priority.
            ItemStack output = TSDBlockEntities.GloryCrucible.findHeatingResult(level, input);
            if (!output.isEmpty()) {
                result.add(new Crucible(List.of(input), fluids, List.of(), List.of(output), false));
            }
        }
        return result;
    }

    public static List<Crucible> crucibleBrewing(Collection<ItemStack> ingredients) {
        List<Crucible> result = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return result;
        }
        var brewing = level.potionBrewing();
        var reagents = ingredients.stream().filter(brewing::isIngredient).toList();
        List<ItemStack> potions = new ArrayList<>();
        for (ItemStack stack : ingredients) {
            if (stack.getItem() instanceof PotionItem) {
                addUnique(potions, stack);
            }
        }
        // JEI may only expose the default bottle, so enumerate the registered
        // potion contents explicitly while keeping supplied custom stacks.
        for (var container : potions.stream().map(ItemStack::getItem).distinct().toList()) {
            for (var potion : BuiltInRegistries.POTION.holders().toList()) {
                addUnique(potions, PotionContents.createItemStack(container, potion));
            }
        }
        for (ItemStack input : potions) {
            for (ItemStack reagent : reagents) {
                if (!brewing.hasMix(input, reagent)) {
                    continue;
                }
                ItemStack output = brewing.mix(reagent, input.copyWithCount(1));
                if (!(output.getItem() instanceof PotionItem)
                        || ItemStack.isSameItemSameComponents(input, output)) {
                    continue;
                }
                result.add(new Crucible(List.of(input.copyWithCount(1)), List.of(),
                        List.of(reagent.copyWithCount(1)), List.of(output.copyWithCount(1)), true));
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Experiment 250
    // ------------------------------------------------------------------

    public static List<Upgrade> upgrades() {
        List<Upgrade> recipes = new ArrayList<>();
        for (int level = 1; level < TSDItems.Experiment250.MAX_LEVEL; level++) {
            for (int count : new int[]{2, 4}) {
                ItemStack input = TSDItems.Experiment250.createStack(TSDRegistry.Items.EXPERIMENT_250, level, 0);
                var result = Experiment250Logic.upgrade(java.util.Collections.nCopies(count, input));
                if (result.valid()) {
                    recipes.add(new Upgrade(count, input,
                            result.outputs().getFirst().copyWithCount(result.outputs().size())));
                }
            }
        }
        return recipes;
    }

    public static List<Differentiation> differentiations() {
        List<Differentiation> recipes = new ArrayList<>();
        for (ResourceLocation id : TSDConfig.getExperimentMeatActivityCosts().keySet()) {
            var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) {
                continue;
            }
            List<Row> rows = new ArrayList<>();
            for (int level = 1; level <= TSDItems.Experiment250.MAX_LEVEL; level++) {
                ItemStack experiment = TSDItems.Experiment250.createStack(TSDRegistry.Items.EXPERIMENT_250,
                        level, TSDItems.Experiment250.getCapacity(level));
                if (TSDConfig.EXPERIMENT_BINDING_MODE_ENABLED.get()) {
                    TSDItems.Experiment250.setBoundMeat(experiment, id);
                }
                var result = Experiment250Logic.calculate(experiment, id);
                rows.add(new Row(level, experiment,
                        result.valid() ? new ItemStack(item, result.outputCount()) : ItemStack.EMPTY,
                        Experiment250Logic.getCost(id, level), result.activityCost()));
            }
            if (rows.stream().anyMatch(row -> !row.output().isEmpty())) {
                recipes.add(new Differentiation(id, List.copyOf(rows)));
            }
        }
        return recipes;
    }

    // ------------------------------------------------------------------
    // World drops
    // ------------------------------------------------------------------

    public static List<Harvest> harvesting() {
        return List.of(new Harvest(new ItemStack(twilightforest.init.TFBlocks.FIDDLEHEAD.get()),
                new ItemStack(TSDRegistry.Items.BRACKEN)));
    }

    public static List<Drop> hunting() {
        List<Drop> recipes = new ArrayList<>();
        pair(recipes, "wild_boar", TSDRegistry.Items.RAW_WILD_BOAR_MEAT,
                TSDRegistry.Items.COOKED_WILD_BOAR_MEAT, false);
        pair(recipes, "bighorn_sheep", TSDRegistry.Items.RAW_BIGHORN_MUTTON,
                TSDRegistry.Items.COOKED_BIGHORN_MUTTON, false);
        pair(recipes, "helmet_crab", TSDRegistry.Items.HERMIT_CRAB_LEG,
                TSDRegistry.Items.COOKED_HERMIT_CRAB_LEG, true);
        pair(recipes, "fire_beetle", TSDRegistry.Items.FIRE_BEETLE_LEG,
                TSDRegistry.Items.COOKED_FIRE_BEETLE_LEG, true);
        pair(recipes, "slime_beetle", TSDRegistry.Items.SLIME_BEETLE_LEG,
                TSDRegistry.Items.COOKED_SLIME_BEETLE_LEG, true);
        pair(recipes, "pinch_beetle", TSDRegistry.Items.PINCH_BEETLE_LEG,
                TSDRegistry.Items.COOKED_PINCH_BEETLE_LEG, true);
        add(recipes, "fire_beetle", TSDRegistry.Items.FIRE_BEETLE_FLAME_SAC, true);
        add(recipes, "slime_beetle", TSDRegistry.Items.SLIME_BEETLE_HONEY_GLAND, true);
        add(recipes, "helmet_crab", TSDRegistry.Items.HERMIT_CRAB, true);
        add(recipes, "redcap", TSDRegistry.Items.REDCAP_SPICE, true);
        add(recipes, "redcap_sapper", TSDRegistry.Items.REDCAP_SPICE, true);
        add(recipes, "death_tome", twilightforest.init.TFItems.TRANSFORMATION_POWDER.get(), true);
        add(recipes, "snow_queen", TSDRegistry.Items.GELID_CRYSTAL, false);
        add(recipes, "minoshroom", TSDRegistry.Items.LABYRINTH_MUSHROOM, false);
        add(recipes, "knight_phantom", TSDRegistry.Items.EXPERIMENT_PROTOTYPE, false);
        recipes.add(new Drop(ResourceLocation.withDefaultNamespace("rabbit"),
                new ItemStack(twilightforest.init.TFItems.POCKET_WATCH.get()),
                false, true, false, true));
        return List.copyOf(recipes);
    }

    private static void pair(List<Drop> recipes, String entity, net.minecraft.world.item.Item raw,
                             net.minecraft.world.item.Item cooked, boolean legs) {
        var id = ResourceLocation.fromNamespaceAndPath("twilightforest", entity);
        recipes.add(new Drop(id, new ItemStack(raw), false, false, legs, false));
        recipes.add(new Drop(id, new ItemStack(cooked), true, false, legs, false));
    }

    private static void add(List<Drop> recipes, String entity, net.minecraft.world.item.Item output,
                            boolean knife) {
        recipes.add(new Drop(ResourceLocation.fromNamespaceAndPath("twilightforest", entity),
                new ItemStack(output), false, knife, false, false));
    }

    private static void addUnique(List<ItemStack> stacks, ItemStack stack) {
        if (stacks.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack))) {
            stacks.add(stack.copyWithCount(1));
        }
    }
}

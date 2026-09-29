package dev.arakiel.twilightsparksdelightfabric.client.integration.jei;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vectorwing.farmersdelight.common.registry.ModBlocks;

/** Optional JEI integration of the Fabric edition. */
@JeiPlugin
public final class TSDJeiPlugin implements IModPlugin {
    public static final RecipeType<TSDJeiRecipes.Upgrade> CULTIVATION =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "cultivation",
                    TSDJeiRecipes.Upgrade.class);
    public static final RecipeType<TSDJeiRecipes.Differentiation> DIFFERENTIATION =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "differentiation",
                    TSDJeiRecipes.Differentiation.class);
    public static final RecipeType<TSDJeiRecipes.Pickling> PICKLING =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "pickling",
                    TSDJeiRecipes.Pickling.class);
    public static final RecipeType<TSDJeiRecipes.Crucible> CRUCIBLE_HEATING =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "glory_crucible_heating",
                    TSDJeiRecipes.Crucible.class);
    public static final RecipeType<TSDJeiRecipes.Crucible> CRUCIBLE_BREWING =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "glory_crucible_brewing",
                    TSDJeiRecipes.Crucible.class);
    public static final RecipeType<TSDJeiRecipes.Drop> HUNTING =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "hunting", TSDJeiRecipes.Drop.class);
    public static final RecipeType<TSDJeiRecipes.Harvest> HARVESTING =
            RecipeType.create(TwilightSparksDelightFabric.MOD_ID, "harvesting", TSDJeiRecipes.Harvest.class);

    @Override
    public ResourceLocation getPluginUid() {
        return TwilightSparksDelightFabric.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new TSDJeiCategories.UpgradeCategory(helper),
                new TSDJeiCategories.DifferentiationCategory(helper),
                new TSDJeiCategories.PicklingCategory(helper),
                new TSDJeiCategories.HuntingCategory(helper),
                new TSDJeiCategories.HarvestCategory(helper),
                new TSDJeiCategories.CrucibleCategory(helper, false),
                new TSDJeiCategories.CrucibleCategory(helper, true));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CULTIVATION, TSDJeiRecipes.upgrades());
        registration.addRecipes(DIFFERENTIATION, TSDJeiRecipes.differentiations());
        registration.addRecipes(PICKLING, List.of(TSDJeiRecipes.pickling()));
        registration.addRecipes(CRUCIBLE_HEATING, TSDJeiRecipes.crucibleHeating());
        registration.addRecipes(CRUCIBLE_BREWING, TSDJeiRecipes.crucibleBrewing(
                registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)));
        registration.addRecipes(HUNTING, TSDJeiRecipes.hunting());
        registration.addRecipes(HARVESTING, TSDJeiRecipes.harvesting());
        registerInfo(registration);
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeCategoryDecorator(RecipeTypes.CRAFTING, new TSDWatchRecipeDecorator<>());
        registration.addRecipeCategoryDecorator(
                vectorwing.farmersdelight.integration.jei.FDRecipeTypes.COOKING,
                new TSDWatchRecipeDecorator<>());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack experiment = new ItemStack(TSDRegistry.Items.EXPERIMENT_250);
        registration.addRecipeCatalyst(experiment, CULTIVATION, DIFFERENTIATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COOKING_POT.get()), CULTIVATION);
        registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.GIANT_COOKING_POT), CULTIVATION);
        registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.GIANTS_COOKING_POT), CULTIVATION);
        switch (TSDConfig.EXPERIMENT_WORKSTATION.get()) {
            case COOKING_POT -> {
                registration.addRecipeCatalyst(new ItemStack(ModBlocks.COOKING_POT.get()), DIFFERENTIATION);
                registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.GIANT_COOKING_POT),
                        DIFFERENTIATION);
                registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.GIANTS_COOKING_POT),
                        DIFFERENTIATION);
            }
            case CUTTING_BOARD -> registration.addRecipeCatalyst(
                    new ItemStack(ModBlocks.CUTTING_BOARD.get()), DIFFERENTIATION);
            case CRAFTING_TABLE -> registration.addRecipeCatalyst(
                    new ItemStack(Items.CRAFTING_TABLE), DIFFERENTIATION);
        }
        for (var stove : List.of(TSDRegistry.Blocks.GIANT_STOVE, TSDRegistry.Blocks.GIANTS_STOVE)) {
            registration.addRecipeCatalyst(new ItemStack(stove), RecipeTypes.CAMPFIRE_COOKING);
        }
        for (var pot : List.of(TSDRegistry.Blocks.GIANT_COOKING_POT, TSDRegistry.Blocks.GIANTS_COOKING_POT)) {
            registration.addRecipeCatalyst(new ItemStack(pot),
                    vectorwing.farmersdelight.integration.jei.FDRecipeTypes.COOKING);
        }
        registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.UNRIPE_PICKLED_BRACKEN_JAR), PICKLING);
        registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.PICKLED_BRACKEN_JAR), PICKLING);
        registration.addRecipeCatalyst(new ItemStack(TSDRegistry.Blocks.GLORY_CRUCIBLE),
                CRUCIBLE_HEATING, CRUCIBLE_BREWING);
    }

    /** Ingredient info pages of the Experiment 250 item. */
    private static void registerInfo(IRecipeRegistration registration) {
        String prefix = "twilightsparksdelightfabric.jei.experiment_250.function.";
        ItemStack stack = new ItemStack(TSDRegistry.Items.EXPERIMENT_250);
        for (String function : new String[]{"scepter", "pedestal", "binding"}) {
            if (function.equals("binding") && !TSDConfig.EXPERIMENT_BINDING_MODE_ENABLED.get()) {
                continue;
            }
            registration.addIngredientInfo(stack, VanillaTypes.ITEM_STACK,
                    Component.translatable(prefix + function),
                    Component.translatable(prefix + function + ".desc"));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(prefix + "fatal"));
        String location = switch (TSDConfig.EXPERIMENT_FATAL_ITEM_LOCATION.get()) {
            case OFFHAND -> "offhand";
            case INVENTORY -> "inventory";
            default -> "baubles_or_offhand";
        };
        String activity = new DecimalFormat("0.##")
                .format(TSDConfig.EXPERIMENT_FATAL_ACTIVITY_COST.get());
        lines.add(Component.translatable(prefix + "fatal.desc",
                Component.translatable(prefix + "fatal.location." + location),
                activity, activity, TSDConfig.EXPERIMENT_FATAL_TRIGGERS_PER_SLEEP.get()));
        if (!TSDConfig.EXPERIMENT_FATAL_ALLOWS_BOSS_ATTACKS.get()) {
            lines.add(Component.translatable(prefix + "fatal.boss_blocked"));
        } else if (!TSDConfig.EXPERIMENT_FATAL_BOSS_ATTACKS_CONSUME_TRIGGER.get()) {
            lines.add(Component.translatable(prefix + "fatal.boss_free"));
        }
        var attackers = TSDConfig.EXPERIMENT_FATAL_NON_CONSUMING_ATTACKERS.get();
        if (!attackers.isEmpty()) {
            lines.add(Component.translatable(prefix + "fatal.non_consuming",
                    String.join(", ", attackers)));
        }
        registration.addIngredientInfo(stack, VanillaTypes.ITEM_STACK, lines.toArray(Component[]::new));
    }
}

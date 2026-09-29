package dev.arakiel.twilightsparksdelightfabric.client.integration.jei;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Marks the Twilight Forest pocket watch as not consumed on every recipe of
 * this mod, including the Farmer's Delight cooking pot pages.
 */
public final class TSDWatchRecipeDecorator<R extends Recipe<?>>
        implements IRecipeCategoryDecorator<RecipeHolder<R>> {
    private final Set<IRecipeSlotDrawable> attached = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    @SuppressWarnings("removal")
    public void draw(RecipeHolder<R> recipe, IRecipeCategory<RecipeHolder<R>> category,
                     IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (!recipe.id().getNamespace().equals(TwilightSparksDelightFabric.MOD_ID)) {
            return;
        }
        for (var view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            if (!(view instanceof IRecipeSlotDrawable slot) || !attached.add(slot)) {
                continue;
            }
            // Decorator tooltips are skipped while hovering a slot, so the
            // tooltip is attached to that slot instead.
            slot.addTooltipCallback((display, tooltip) -> display.getDisplayedItemStack().ifPresent(stack -> {
                if (stack.is(twilightforest.init.TFItems.POCKET_WATCH.get())) {
                    tooltip.add(Component.translatable("twilightsparksdelightfabric.jei.not_consumed"));
                }
            }));
        }
    }
}

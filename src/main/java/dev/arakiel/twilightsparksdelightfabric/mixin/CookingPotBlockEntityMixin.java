package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.event.TSDCookingPotRecipes;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.refabricated.inventory.RecipeWrapper;

/**
 * Hooks the actual recipe completion, not a later inventory scan, so retained
 * ingredients stay in their slot and crafted-only markers cannot leak.
 */
@Mixin(value = CookingPotBlockEntity.class, remap = false)
public abstract class CookingPotBlockEntityMixin {
    /**
     * Farmer's Delight's cooking pot constructor always passes its own block
     * entity type to the super constructor, but this Minecraft version
     * validates that type against the placed block. The giant cooking pots have
     * their own type, so it is corrected here for those block entities before
     * the validation runs.
     */
    @ModifyArg(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lvectorwing/farmersdelight/common/block/entity/SyncedBlockEntity;"
                            + "<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;)V",
                    remap = true),
            index = 0)
    private static BlockEntityType<?> tsd$giantPotType(BlockEntityType<?> original,
                                                       BlockPos pos, BlockState state) {
        // The handler runs before the super constructor, so "this" does not
        // exist yet: the placed block identifies our giant cooking pots.
        return state.getBlock() instanceof KitchenBlocks.GiantPot
                ? TSDRegistry.BlockEntities.GIANT_COOKING_POT : original;
    }

    @Inject(method = "isContainerValid", at = @At("HEAD"), cancellable = true, require = 0)
    private void tsd$requireEmptyPicklingJar(ItemStack container, CallbackInfoReturnable<Boolean> callback) {
        if (!container.is(twilightforest.init.TFItems.MASON_JAR.get())) {
            return;
        }
        ItemStack meal = ((CookingPotBlockEntity) (Object) this).getMeal();
        if ((meal.is(TSDRegistry.Blocks.UNRIPE_PICKLED_BRACKEN_JAR.asItem())
                || meal.is(TSDRegistry.Blocks.PICKLED_BRACKEN_JAR.asItem()))
                && container.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        .nonEmptyItems().iterator().hasNext()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "cookingTick", at = @At("TAIL"), require = 0)
    private static void tsd$dynamicRecipes(Level level, BlockPos pos, BlockState state,
                                           CookingPotBlockEntity pot, CallbackInfo callback) {
        if (!level.isClientSide) {
            TSDCookingPotRecipes.tickPot(pot);
        }
    }

    @Redirect(method = "processCooking", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V", remap = true), require = 0)
    private void tsd$retainWatch(ItemStack stack, int count, RecipeHolder<CookingPotRecipe> recipe,
                                 CookingPotBlockEntity pot) {
        if (recipe.id().getNamespace().equals(TwilightSparksDelightFabric.MOD_ID)
                && stack.is(twilightforest.init.TFItems.POCKET_WATCH.get())) {
            return;
        }
        stack.shrink(count);
    }

    @Redirect(method = "processCooking", at = @At(value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/crafting/CookingPotRecipe;assemble"
                    + "(Lvectorwing/farmersdelight/refabricated/inventory/RecipeWrapper;"
                    + "Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            remap = true),
            require = 0)
    private ItemStack tsd$prepareMeal(CookingPotRecipe recipe, RecipeWrapper input,
                                      HolderLookup.Provider registries) {
        ItemStack output = recipe.assemble(input, registries);
        if (output.is(TSDRegistry.Items.TWILIGHT_CHEESE_FONDUE_COMPANION)) {
            TSDItems.withFullDurability(output);
        }
        if (output.is(TSDRegistry.Items.MILLION_POUND_MEAL)
                || output.is(TSDRegistry.Blocks.NAGA_MIXED_RICE.asItem())) {
            output.set(TSDRegistry.Components.COOKED_ADVANCEMENT, true);
        }
        if (output.is(TSDRegistry.Blocks.NAGA_MIXED_RICE.asItem())) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    TwilightSparksDelightFabric.id("naga_mixed_rice_experiment"));
            String ingredient = "hydra";
            for (int slot = 0; slot < 6; slot++) {
                if (input.getItem(slot).is(tag)) {
                    ingredient = "experiment";
                    break;
                }
            }
            output.set(TSDRegistry.Components.NAGA_INGREDIENT, ingredient);
        }
        return output;
    }
}

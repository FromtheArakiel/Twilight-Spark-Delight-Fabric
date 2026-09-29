package dev.arakiel.twilightsparksdelightfabric.mixin;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

/** Access to the private state of Farmer's Delight's cooking pot. */
@Mixin(value = CookingPotBlockEntity.class, remap = false)
public interface CookingPotAccess {
    @Accessor("mealContainerStack")
    void tsd$setMealContainer(ItemStack stack);

    @Accessor("cookingPotData")
    ContainerData tsd$getCookingData();
}

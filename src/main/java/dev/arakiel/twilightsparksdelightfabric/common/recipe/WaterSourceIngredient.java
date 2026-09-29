package dev.arakiel.twilightsparksdelightfabric.common.recipe;

import java.util.List;

import com.mojang.serialization.MapCodec;
import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Ingredient that accepts a water bucket or a bottle filled with water.
 *
 * <p>The NeoForge edition expressed this with
 * {@code neoforge:compound} around {@code neoforge:components}. Fabric has no
 * equivalent JSON file format, so the same rule is implemented as a custom
 * ingredient and used from the recipe files with
 * {@code "fabric:type": "twilightsparksdelightfabric:water_source"}.</p>
 */
public final class WaterSourceIngredient implements CustomIngredient {
    public static final WaterSourceIngredient INSTANCE = new WaterSourceIngredient();
    public static final ResourceLocation ID =
            TwilightSparksDelightFabric.id("water_source");
    public static final CustomIngredientSerializer<WaterSourceIngredient> SERIALIZER = new Serializer();

    private static final ItemStack WATER_BOTTLE =
            PotionContents.createItemStack(Items.POTION, Potions.WATER);

    private WaterSourceIngredient() {
    }

    /** Registers the serializer; must run during mod initialization. */
    public static void register() {
        CustomIngredientSerializer.register(SERIALIZER);
    }

    /** True for a water bucket or a water bottle, components included. */
    public static boolean isWaterSource(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            return true;
        }
        return stack.is(Items.POTION) && ItemStack.isSameItemSameComponents(stack, WATER_BOTTLE);
    }

    @Override
    public boolean test(ItemStack stack) {
        return isWaterSource(stack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return List.of(new ItemStack(Items.WATER_BUCKET), WATER_BOTTLE.copy());
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer implements CustomIngredientSerializer<WaterSourceIngredient> {
        @Override
        public ResourceLocation getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<WaterSourceIngredient> getCodec(boolean allowEmpty) {
            return MapCodec.unit(INSTANCE);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WaterSourceIngredient> getPacketCodec() {
            return StreamCodec.unit(INSTANCE);
        }
    }
}

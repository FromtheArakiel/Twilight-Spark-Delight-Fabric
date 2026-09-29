package dev.arakiel.twilightsparksdelightfabric.common;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vectorwing.farmersdelight.common.item.KnifeItem;

/** Small helpers shared by the migrated content. */
public final class TSDUtil {
    public static final TagKey<net.minecraft.world.item.Item> COMMON_KNIFE_TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/knife"));
    public static final TagKey<net.minecraft.world.item.Item> FARMERS_KNIFE_TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("farmersdelight", "tools/knives"));

    private TSDUtil() {
    }

    public static ResourceLocation id(String path) {
        return TwilightSparksDelightFabric.id(path);
    }

    /** Farmer's Delight knives, the common knife tag and the Farmer's Delight tag. */
    public static boolean isKnife(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(COMMON_KNIFE_TAG) || stack.is(FARMERS_KNIFE_TAG)
                || stack.getItem() instanceof KnifeItem;
    }

    public static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}

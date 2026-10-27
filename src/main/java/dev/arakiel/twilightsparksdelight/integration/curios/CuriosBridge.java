package dev.arakiel.twilightsparksdelight.integration.curios;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CuriosBridge {
    @FunctionalInterface
    public interface Finder {
        ItemStack find(Player player, Predicate<ItemStack> predicate);
    }

    @FunctionalInterface
    public interface Iterator {
        void forEach(Player player, Consumer<ItemStack> action);
    }

    private static volatile Finder finder = (player, predicate) -> ItemStack.EMPTY;
    private static volatile Iterator iterator = (player, action) -> {
    };

    private CuriosBridge() {
    }

    public static void install(Finder find, Iterator forEach) {
        finder = Objects.requireNonNull(find, "find");
        iterator = Objects.requireNonNull(forEach, "forEach");
    }

    public static ItemStack find(Player player, Predicate<ItemStack> predicate) {
        return finder.find(player, predicate);
    }

    public static void forEach(Player player, Consumer<ItemStack> action) {
        iterator.forEach(player, action);
    }

    public static boolean hasWatch(Player player) {
        return !find(player, stack -> stack.is(twilightforest.init.TFItems.POCKET_WATCH.get())).isEmpty();
    }
}

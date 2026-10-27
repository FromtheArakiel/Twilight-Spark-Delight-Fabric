package dev.arakiel.twilightsparksdelight.integration.curios;

import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.type.capability.ICurioItem;
import dev.arakiel.gemini.event.GeminiEvents;
import dev.arakiel.gemini.event.server.ServerLifecycleEvent;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.arakiel.gemini.integration.GeminiIntegrationType;
import dev.arakiel.twilightsparksdelight.registry.TSDRegistry;
import net.minecraft.world.item.ItemStack;

@GeminiIntegration(
        value = "curiosfabric",
        version = "[9.5.1,)",
        type = GeminiIntegrationType.COMMON
)
public final class CuriosIntegration {
    private boolean itemsRegistered;

    public CuriosIntegration() {
    }

    public void apply() {
        CuriosBridge.install(
                (player, predicate) -> CuriosApi.getCuriosInventory(player)
                        .flatMap(inventory -> inventory.findFirstCurio(predicate))
                        .map(result -> result.stack())
                        .orElse(ItemStack.EMPTY),
                (player, action) -> CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                        inventory.getCurios().values().forEach(handler -> {
                            var stacks = handler.getStacks();
                            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                                action.accept(stacks.getStackInSlot(slot));
                            }
                        })));
        GeminiEvents.listen(ServerLifecycleEvent.class, event -> {
            if (event.getStage() == ServerLifecycleEvent.Stage.STARTING) {
                registerItems();
            }
        });
    }

    private void registerItems() {
        if (itemsRegistered) {
            return;
        }
        itemsRegistered = true;
        CuriosApi.registerCurio(
                twilightforest.init.TFItems.POCKET_WATCH.get(),
                new ICurioItem() {
                });
        CuriosApi.registerCurio(TSDRegistry.Items.EXPERIMENT_250, new ICurioItem() {
        });
    }
}

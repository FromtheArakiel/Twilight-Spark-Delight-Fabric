package dev.arakiel.twilightsparksdelightfabric.client;

import dev.arakiel.twilightsparksdelightfabric.client.render.TSDBlockEntityRenderers;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.common.network.TSDPayloads;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Client side setup: payload receivers and block entity renderers. */
public final class TSDClientSetup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((graphics, partialTick) -> TSDExtraFoodHud.render(graphics));
        if (FabricLoader.getInstance().isModLoaded("appleskin")) {
            TSDAppleSkinCompat.register();
        }

        ClientPlayNetworking.registerGlobalReceiver(TSDPayloads.FoodState.TYPE,
                (payload, context) -> context.client().execute(() -> TSDClientEvents.applyFoodState(payload)));
        ClientPlayNetworking.registerGlobalReceiver(TSDPayloads.ExperimentActivation.TYPE,
                (payload, context) -> context.client().execute(
                        () -> TSDClientEvents.showExperimentActivation(payload.stack())));

        // The thrown pickled bracken is a normal item projectile.
        EntityRendererRegistry.register(TSDRegistry.Entities.PICKLED_BRACKEN, ThrownItemRenderer::new);

        BlockEntityRenderers.register(TSDRegistry.BlockEntities.LARGE_FEAST, TSDBlockEntityRenderers.LargeFeast::new);
        BlockEntityRenderers.register(TSDRegistry.BlockEntities.GIANT_STOVE, TSDBlockEntityRenderers.Kitchen::stove);
        BlockEntityRenderers.register(TSDRegistry.BlockEntities.GIANT_COOKING_POT,
                TSDBlockEntityRenderers.Kitchen::pot);
        BlockEntityRenderers.register(TSDRegistry.BlockEntities.GLORY_CRUCIBLE,
                TSDBlockEntityRenderers.Crucible::new);

        registerExperimentModelProperty();
    }

    /**
     * Drives the stage models of Experiment 250.
     *
     * <p>This registry is the only item model property API of the Fabric API
     * build for this Minecraft version; it is deprecated in favour of an API
     * that this version does not ship yet.</p>
     */
    @SuppressWarnings("deprecation")
    private static void registerExperimentModelProperty() {
        FabricModelPredicateProviderRegistry.register(TSDRegistry.Items.EXPERIMENT_250,
                TwilightSparksDelightFabric.id("activity_stage"),
                (stack, level, entity, seed) -> TSDItems.Experiment250.getActivityStage(stack));
    }
}

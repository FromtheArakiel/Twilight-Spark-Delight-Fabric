package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.event.TSDEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity join hook for the migrated world behaviour.
 *
 * <p>Fabric has no entity-join callback in this API version, so the armored
 * giant variant is rolled when an entity is added to a server level. Giants
 * loaded from a save keep the variant stored in their persistent data.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void tsd$onEntityAdded(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            TSDEvents.onEntityLoad(entity, (ServerLevel) (Object) this);
        }
    }
}

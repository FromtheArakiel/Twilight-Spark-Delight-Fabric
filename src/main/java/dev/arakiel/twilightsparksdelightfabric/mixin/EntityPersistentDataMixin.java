package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the persistent data tag used by the migrated systems to every entity.
 *
 * <p>{@code Entity#addAdditionalSaveData} and {@code readAdditionalSaveData}
 * are abstract in this Minecraft version, so the injections target the concrete
 * serialization entry points instead: every entity serializer calls
 * {@code saveWithoutId} and {@code load}.</p>
 */
@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements TSDPersistentData.Holder {
    @Unique
    private CompoundTag tsd$data;

    @Override
    public CompoundTag tsd$persistentData() {
        if (tsd$data == null) {
            tsd$data = new CompoundTag();
        }
        return tsd$data;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void tsd$saveData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
        CompoundTag result = callback.getReturnValue();
        if (result != null && tsd$data != null && !tsd$data.isEmpty()) {
            result.put(TSDPersistentData.TAG_KEY, tsd$data.copy());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void tsd$loadData(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(TSDPersistentData.TAG_KEY, Tag.TAG_COMPOUND)) {
            tsd$data = tag.getCompound(TSDPersistentData.TAG_KEY).copy();
        }
    }
}

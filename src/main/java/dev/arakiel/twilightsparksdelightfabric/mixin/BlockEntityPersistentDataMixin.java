package dev.arakiel.twilightsparksdelightfabric.mixin;

import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the persistent data tag to every block entity. */
@Mixin(BlockEntity.class)
public abstract class BlockEntityPersistentDataMixin implements TSDPersistentData.Holder {
    @Unique
    private CompoundTag tsd$data;

    @Override
    public CompoundTag tsd$persistentData() {
        if (tsd$data == null) {
            tsd$data = new CompoundTag();
        }
        return tsd$data;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void tsd$saveData(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (tsd$data != null && !tsd$data.isEmpty()) {
            tag.put(TSDPersistentData.TAG_KEY, tsd$data.copy());
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void tsd$loadData(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (tag.contains(TSDPersistentData.TAG_KEY, Tag.TAG_COMPOUND)) {
            tsd$data = tag.getCompound(TSDPersistentData.TAG_KEY).copy();
        }
    }
}

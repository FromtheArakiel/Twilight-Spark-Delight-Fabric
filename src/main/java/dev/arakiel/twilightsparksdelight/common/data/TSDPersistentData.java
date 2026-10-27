package dev.arakiel.twilightsparksdelight.common.data;

import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TSDPersistentData {
    private TSDPersistentData() {
    }

    public static CompoundTag of(Entity entity) {
        return TSDDataComponents.of(entity);
    }

    public static CompoundTag of(BlockEntity blockEntity) {
        return TSDDataComponents.of(blockEntity);
    }
}

package dev.arakiel.twilightsparksdelightfabric.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;

/**
 * Per-instance scratch data for entities and block entities.
 *
 * <p>The NeoForge edition stored counters and markers in an attached compound
 * tag. The same tag is provided here by a mixin on {@link Entity} and
 * {@link BlockEntity}; it is saved with the owning object, exactly like the
 * original attachment.</p>
 */
public final class TSDPersistentData {
    /** Key used inside the saved data of entities and block entities. */
    public static final String TAG_KEY = "TwilightSparkDelightData";

    private TSDPersistentData() {
    }

    /** Implemented by the mixins that own the tag. */
    public interface Holder {
        CompoundTag tsd$persistentData();
    }

    public static CompoundTag of(Entity entity) {
        return ((Holder) entity).tsd$persistentData();
    }

    public static CompoundTag of(BlockEntity blockEntity) {
        return ((Holder) blockEntity).tsd$persistentData();
    }
}

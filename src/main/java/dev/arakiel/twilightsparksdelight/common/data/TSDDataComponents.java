package dev.arakiel.twilightsparksdelight.common.data;

import dev.arakiel.gemini.component.Component;
import dev.arakiel.gemini.component.ComponentFactoryRegistries;
import dev.arakiel.gemini.component.ComponentInitializers;
import dev.arakiel.gemini.component.ComponentKey;
import dev.arakiel.gemini.component.GeminiComponents;
import dev.arakiel.twilightsparksdelight.TwilightSparksDelight;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TSDDataComponents implements ComponentInitializers.Entity, ComponentInitializers.Block {
    private static ComponentKey<PersistentData> entityData;
    private static ComponentKey<PersistentData> blockEntityData;

    @Override
    public void registerEntityComponentFactories(ComponentFactoryRegistries.Entity registry) {
        registry.registerFor(Entity.class, entityData(), ignored -> new PersistentData());
    }

    @Override
    public void registerBlockComponentFactories(ComponentFactoryRegistries.Block registry) {
        registry.registerFor(BlockEntity.class, blockEntityData(), ignored -> new PersistentData());
    }

    public static CompoundTag of(Entity entity) {
        return GeminiComponents.get(entityData(), entity).tag();
    }

    public static CompoundTag of(BlockEntity blockEntity) {
        return GeminiComponents.get(blockEntityData(), blockEntity).tag();
    }

    private static ComponentKey<PersistentData> entityData() {
        if (entityData == null) {
            entityData = GeminiComponents.key(
                    TwilightSparksDelight.id("entity_data"),
                    PersistentData.class);
        }
        return entityData;
    }

    private static ComponentKey<PersistentData> blockEntityData() {
        if (blockEntityData == null) {
            blockEntityData = GeminiComponents.key(
                    TwilightSparksDelight.id("block_entity_data"),
                    PersistentData.class);
        }
        return blockEntityData;
    }

    public static final class PersistentData implements Component {
        private CompoundTag tag = new CompoundTag();

        public CompoundTag tag() {
            return tag;
        }

        @Override
        public void readFromNbt(CompoundTag source, HolderLookup.Provider registries) {
            tag = source.copy();
        }

        @Override
        public void writeToNbt(CompoundTag target, HolderLookup.Provider registries) {
            if (!tag.isEmpty()) {
                target.merge(tag.copy());
            }
        }
    }

    private TSDDataComponents() {
    }
}

package dev.arakiel.twilightsparksdelightfabric.common.network;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Client bound payloads of the mod. */
public final class TSDPayloads {
    private TSDPayloads() {
    }

    /** Registers both client bound payload types. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(FoodState.TYPE, FoodState.CODEC);
        PayloadTypeRegistry.playS2C().register(ExperimentActivation.TYPE, ExperimentActivation.CODEC);
    }

    /** Full hunger state of the player, used by the extended food bar. */
    public record FoodState(int food, float saturation, float exhaustion, int cap)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FoodState> TYPE =
                new CustomPacketPayload.Type<>(TwilightSparksDelightFabric.id("food_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FoodState> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FoodState::food,
                ByteBufCodecs.FLOAT, FoodState::saturation,
                ByteBufCodecs.FLOAT, FoodState::exhaustion,
                ByteBufCodecs.VAR_INT, FoodState::cap, FoodState::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Tells the client that a fatal protection charge was consumed. */
    public record ExperimentActivation(ItemStack stack) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ExperimentActivation> TYPE =
                new CustomPacketPayload.Type<>(TwilightSparksDelightFabric.id("experiment_activation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExperimentActivation> CODEC =
                ItemStack.STREAM_CODEC.map(ExperimentActivation::new, ExperimentActivation::stack);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

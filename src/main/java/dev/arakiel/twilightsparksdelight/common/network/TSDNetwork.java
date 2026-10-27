package dev.arakiel.twilightsparksdelight.common.network;

import java.util.Objects;
import java.util.function.Consumer;

import dev.arakiel.gemini.network.GeminiNetwork;
import dev.arakiel.gemini.network.NetworkChannel;
import dev.arakiel.gemini.network.RecordCodecs;
import dev.arakiel.twilightsparksdelight.TwilightSparksDelight;
import net.minecraft.world.item.ItemStack;

public final class TSDNetwork {
    public static final NetworkChannel CHANNEL =
            GeminiNetwork.channel(TwilightSparksDelight.id("state")).optional();

    private static volatile Consumer<FoodState> foodStateHandler = ignored -> {
    };
    private static volatile Consumer<ExperimentActivation> experimentActivationHandler = ignored -> {
    };

    private TSDNetwork() {
    }

    public static void register() {
        GeminiNetwork.init();
        CHANNEL.registerClientbound(
                FoodState.class,
                RecordCodecs.create(FoodState.class),
                TSDNetwork::acceptFoodState);
        CHANNEL.registerClientbound(
                ExperimentActivation.class,
                RecordCodecs.create(ExperimentActivation.class),
                TSDNetwork::acceptExperimentActivation);
    }

    public static void setClientHandlers(
            Consumer<FoodState> foodStateConsumer,
            Consumer<ExperimentActivation> activationConsumer
    ) {
        foodStateHandler = Objects.requireNonNull(foodStateConsumer, "foodStateConsumer");
        experimentActivationHandler = Objects.requireNonNull(activationConsumer, "activationConsumer");
    }

    private static void acceptFoodState(FoodState state) {
        foodStateHandler.accept(state);
    }

    private static void acceptExperimentActivation(ExperimentActivation activation) {
        experimentActivationHandler.accept(activation);
    }

    public record FoodState(int food, float saturation, float exhaustion, int cap) {
    }

    public record ExperimentActivation(ItemStack stack) {
    }
}

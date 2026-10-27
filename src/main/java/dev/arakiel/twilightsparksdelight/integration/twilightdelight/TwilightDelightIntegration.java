package dev.arakiel.twilightsparksdelight.integration.twilightdelight;

import java.util.ArrayList;
import java.util.List;

import dev.arakiel.gemini.event.EventResult;
import dev.arakiel.gemini.event.GeminiEvents;
import dev.arakiel.gemini.event.effect.MobEffectApplicableEvent;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.arakiel.gemini.integration.GeminiIntegrationType;
import dev.arakiel.twilightsparksdelight.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelight.common.effect.TSDMobEffects;
import dev.arakiel.twilightsparksdelight.registry.TSDRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@GeminiIntegration(
        value = "twilightdelight",
        version = "[3.2.0,)",
        type = GeminiIntegrationType.COMMON
)
public final class TwilightDelightIntegration implements TwilightDelightBridge.Hooks {
    private static final String MOD_ID = "twilightdelight";

    public TwilightDelightIntegration() {
    }

    public void apply() {
        TwilightDelightBridge.install(this);
        GeminiEvents.listen(MobEffectApplicableEvent.class, this::onEffectApplicable);
    }

    @Override
    public List<MobEffectInstance> visibleEffects(ItemStack stack, List<MobEffectInstance> effects) {
        List<MobEffectInstance> result = new ArrayList<>(effects.size() + 1);
        boolean frozen = stack.is(TSDRegistry.Items.GELID_CRYSTAL)
                || stack.is(TSDRegistry.Items.DOUBLE_CROWN_ICE_CREAM)
                || stack.is(TSDRegistry.Items.TWIN_RADIANCE_ICE_POP);
        boolean fire = stack.is(TSDRegistry.Items.FIRE_BEETLE_FLAME_SAC)
                || stack.is(TSDRegistry.Items.LABYRINTH_FLAVOR_SKEWER)
                || stack.is(TSDRegistry.Items.STIR_FRIED_BRACKEN)
                || "hydra".equals(stack.get(TSDRegistry.Components.NAGA_INGREDIENT));
        for (MobEffectInstance instance : effects) {
            Holder<MobEffect> replacement = frozen && instance.is(twilightforest.init.TFMobEffects.FROSTY)
                    ? effect("frozen_range")
                    : fire && instance.is(MobEffects.FIRE_RESISTANCE) ? effect("fire_range") : null;
            result.add(replacement == null ? instance : copy(instance, replacement));
        }
        Holder<MobEffect> aurora = effect("aurora_glowing");
        if (aurora != null && stack.is(TSDRegistry.Items.TWIN_RADIANCE_ICE_POP)) {
            result.add(new MobEffectInstance(aurora, 1800, 0, false, false, true));
        }
        return result;
    }

    @Override
    public void afterEating(LivingEntity entity, ItemStack stack) {
        List<MobEffectInstance> current = List.copyOf(entity.getActiveEffects());
        List<MobEffectInstance> replacements = visibleEffects(stack, current);
        for (int index = 0; index < current.size(); index++) {
            MobEffectInstance before = current.get(index);
            MobEffectInstance after = replacements.get(index);
            if (!before.getEffect().equals(after.getEffect())) {
                entity.removeEffect(before.getEffect());
                entity.addEffect(after);
            }
        }
        for (int index = current.size(); index < replacements.size(); index++) {
            entity.addEffect(replacements.get(index));
        }
    }

    private static MobEffectInstance copy(MobEffectInstance source, Holder<MobEffect> effect) {
        return new MobEffectInstance(
                effect,
                source.getDuration(),
                source.getAmplifier(),
                source.isAmbient(),
                source.isVisible(),
                source.showIcon());
    }

    private static Holder<MobEffect> effect(String path) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, path))
                .orElse(null);
    }

    private void onEffectApplicable(MobEffectApplicableEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        Holder<MobEffect> sadness = effect("temporal_sadness");
        if (sadness == null) {
            return;
        }
        MobEffectInstance added = event.getEffect();
        MobEffectInstance sorrow = entity.getEffect(TSDMobEffects.holder(TSDRegistry.Effects.SORROW));
        boolean grief = entity.hasEffect(TSDMobEffects.holder(TSDRegistry.Effects.GRIEF));
        if (added.is(sadness) && (sorrow != null || grief)) {
            event.setResult(EventResult.DENY);
            entity.removeEffect(sadness);
            if (sorrow != null) {
                turnToGrief(entity, sorrow);
            }
        } else if (added.is(TSDMobEffects.holder(TSDRegistry.Effects.SORROW))
                && entity.hasEffect(sadness)) {
            event.setResult(EventResult.DENY);
            entity.removeEffect(sadness);
            turnToGrief(entity, added);
        } else if (added.is(TSDMobEffects.holder(TSDRegistry.Effects.GRIEF))) {
            entity.removeEffect(sadness);
        }
    }

    private static void turnToGrief(LivingEntity entity, MobEffectInstance sorrow) {
        entity.removeEffect(TSDMobEffects.holder(TSDRegistry.Effects.SORROW));
        TSDPersistentData.of(entity).remove("tsd_sorrow_damage");
        entity.addEffect(new MobEffectInstance(
                TSDMobEffects.holder(TSDRegistry.Effects.GRIEF),
                sorrow.getDuration(),
                sorrow.getAmplifier() + 1,
                false,
                false,
                true));
    }
}

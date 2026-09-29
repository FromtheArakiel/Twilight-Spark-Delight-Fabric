package dev.arakiel.twilightsparksdelightfabric.event;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelightfabric.common.effect.SizeEffectHelper;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;

/** Combat behaviour of the mod effects, the size system and the charge effect. */
public final class TSDCombatEvents {
    private static final ResourceLocation CHARGE_SPEED =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "charge_speed");
    private static final ResourceLocation CHARGE_STEP =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "charge_step");
    private static final ResourceLocation BLOCK_REACH_ID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "size_block_reach");
    private static final ResourceLocation ENTITY_REACH_ID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "size_entity_reach");
    private static final ResourceLocation STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", "size_step_height");

    private TSDCombatEvents() {
    }

    // ------------------------------------------------------------------
    // Effect bookkeeping
    // ------------------------------------------------------------------

    /** Called when an effect instance is added to a living entity. */
    public static void onEffectAdded(LivingEntity entity, MobEffectInstance instance,
                                     MobEffectInstance oldInstance) {
        if (oldInstance != null || entity.level().isClientSide) {
            return;
        }
        var effect = instance.getEffect();
        var data = TSDPersistentData.of(entity);
        // Holder#is(Holder) is deprecated; the registry holders wrap the same
        // singleton effect instances, so the values are compared directly.
        if (effect.value() == TSDRegistry.Effects.SYMBIOSIS.value()) {
            data.remove("tsd_symbiosis_hunger");
            data.remove("tsd_symbiosis_heal_ticks");
        } else if (effect.value() == TSDRegistry.Effects.SORROW.value()) {
            data.remove("tsd_sorrow_damage");
        }
    }

    /** Mining fatigue is ignored while a pocket watch is carried. */
    public static boolean shouldBlockEffect(LivingEntity entity, MobEffectInstance instance) {
        return entity instanceof Player player && instance.is(MobEffects.DIG_SLOWDOWN)
                && TSDEvents.blocksMiningFatigue(player);
    }

    // ------------------------------------------------------------------
    // Incoming damage
    // ------------------------------------------------------------------

    /**
     * Applies every damage modifier of the mod.
     *
     * @return the new damage amount
     */
    public static float modifyIncomingDamage(LivingEntity entity,
                                             net.minecraft.world.damagesource.DamageSource source,
                                             float amount) {
        float result = amount;
        var data = TSDPersistentData.of(entity);

        MobEffectInstance sorrow = entity.getEffect(TSDRegistry.Effects.SORROW);
        if (sorrow != null && result > 0.0F) {
            float raw = result;
            float reduction = Math.min(0.95F, 0.1F * (sorrow.getAmplifier() + 1));
            result = raw * (1.0F - reduction);
            double total = data.getDouble("tsd_sorrow_damage") + raw;
            if (total >= 18.0D) {
                data.remove("tsd_sorrow_damage");
                entity.removeEffect(TSDRegistry.Effects.SORROW);
                entity.addEffect(new MobEffectInstance(TSDRegistry.Effects.GRIEF, sorrow.getDuration(),
                        sorrow.getAmplifier() + 1, false, false, true));
            } else {
                data.putDouble("tsd_sorrow_damage", total);
            }
        } else if (sorrow == null) {
            data.remove("tsd_sorrow_damage");
        }

        MobEffectInstance symbiosis = entity.getEffect(TSDRegistry.Effects.SYMBIOSIS);
        if (symbiosis != null) {
            float reduction = Math.min(0.95F,
                    TSDConfig.SYMBIOSIS_DAMAGE_REDUCTION.get().floatValue()
                            + TSDConfig.SYMBIOSIS_DAMAGE_REDUCTION_PER_LEVEL.get().floatValue()
                            * symbiosis.getAmplifier());
            result *= 1.0F - reduction;
        }

        if (entity instanceof Player player) {
            float scale = SizeEffectHelper.getScale(player);
            if (scale > 1.0F) {
                result *= scale;
            }
            if (TSDEvents.onPocketWatchShieldHit(player, source.getEntity(),
                    source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE))) {
                return result;
            }
        }

        if (source.getDirectEntity() instanceof Player attacker) {
            float scale = SizeEffectHelper.getScale(attacker);
            if (scale > 1.0F) {
                result *= (float) outgoingDamageMultiplier(attacker);
            }
            MobEffectInstance grief = attacker.getEffect(TSDRegistry.Effects.GRIEF);
            if (grief != null) {
                float increase = TSDConfig.GRIEF_DAMAGE_BONUS_PER_LEVEL.get().floatValue()
                        * (grief.getAmplifier() + 1);
                result *= 1.0F + increase;
            }
            if (attacker.hasEffect(TSDRegistry.Effects.ABYSS_CALL)
                    && entity instanceof net.minecraft.world.entity.monster.Enemy) {
                entity.addEffect(new MobEffectInstance(TSDRegistry.Effects.SYMBIOSIS, 300, 0,
                        false, false, true));
            }
            boolean critical = attacker.fallDistance > 0.0F && !attacker.onGround()
                    && !attacker.onClimbable() && !attacker.isInWater()
                    && !attacker.hasEffect(MobEffects.BLINDNESS) && !attacker.isPassenger();
            AttributeInstance speed = attacker.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attacker.hasEffect(TSDRegistry.Effects.CHARGE) && (attacker.isSprinting() || critical)
                    && speed != null && speed.getBaseValue() > 0.0D) {
                double bonus = Math.max(0.0D, Math.min(TSDConfig.CHARGE_MAX_ATTACK_BONUS.get(),
                        speed.getValue() / speed.getBaseValue() - 1.0D));
                result *= (float) (1.0D + bonus);
            }
        }
        return result;
    }

    /**
     * Damage that must be cancelled completely instead of being reduced.
     *
     * <p>Shrunk players dodge part of all incoming damage and the special
     * killer rabbits created by the transformation powder cannot hurt players.</p>
     */
    public static boolean shouldCancelDamage(LivingEntity entity,
                                             net.minecraft.world.damagesource.DamageSource source) {
        if (entity instanceof Player && (TSDEvents.isSpecialKillerRabbit(source.getEntity())
                || TSDEvents.isSpecialKillerRabbit(source.getDirectEntity()))) {
            return true;
        }
        if (entity instanceof Player player) {
            double dodgeChance = SizeEffectHelper.getDodgeChance(player);
            return dodgeChance > 0.0D && player.getRandom().nextDouble() < dodgeChance;
        }
        return false;
    }

    public static double outgoingDamageMultiplier(Player player) {
        AttributeInstance reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach == null || reach.getValue() <= 0) {
            return 1.0D;
        }
        double original = SizeEffectHelper.attributeValueWithout(reach, ENTITY_REACH_ID, true);
        return Math.min(1.0D, Math.max(0.0D, original / reach.getValue()));
    }

    // ------------------------------------------------------------------
    // Size and charge attributes
    // ------------------------------------------------------------------

    /** Applies the size, charge and reach attributes of a player. */
    public static void onPlayerTick(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (speed != null) {
            speed.removeModifier(CHARGE_SPEED);
            if (player.hasEffect(TSDRegistry.Effects.CHARGE)) {
                speed.addTransientModifier(new AttributeModifier(CHARGE_SPEED,
                        TSDConfig.CHARGE_SPEED_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (step != null) {
            step.removeModifier(CHARGE_STEP);
            if (player.hasEffect(TSDRegistry.Effects.CHARGE)) {
                step.addTransientModifier(new AttributeModifier(CHARGE_STEP,
                        TSDConfig.CHARGE_STEP_HEIGHT_BONUS.get(), AttributeModifier.Operation.ADD_VALUE));
            }
        }

        float scale = SizeEffectHelper.getScale(player);
        AttributeInstance size = player.getAttribute(Attributes.SCALE);
        if (size != null && updateModifierAmount(size, SizeEffectHelper.SCALE_MODIFIER_ID, scale - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
            player.refreshDimensions();
        }
        updateReach(player, scale);
        if (step != null) {
            updateModifierAmount(step, STEP_HEIGHT_ID, scale > 1.0F ? 0.6D * (scale - 1.0F) : 0.0D,
                    AttributeModifier.Operation.ADD_VALUE);
        }
    }

    /** Jump height follows the current scale. */
    public static void onJump(Player player) {
        float scale = SizeEffectHelper.getScale(player);
        if (scale <= 1.0F) {
            return;
        }
        var movement = player.getDeltaMovement();
        double velocity = SizeEffectHelper.jumpVelocityForHeight(scale, player.getGravity());
        // Preserve jump bonuses supplied by potions or other mods.
        velocity += Math.max(0.0D, movement.y() - 0.42D);
        player.setDeltaMovement(movement.x(), velocity, movement.z());
    }

    private static void updateReach(Player player, float scale) {
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        double multiplier = SizeEffectHelper.getReachMultiplier(player);
        updateModifier(block, BLOCK_REACH_ID, multiplier);
        updateModifier(entity, ENTITY_REACH_ID, multiplier);
    }

    private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double multiplier) {
        if (attribute == null) {
            return;
        }
        double factor = SizeEffectHelper.additiveModifierFactor(attribute);
        double delta = attribute.getAttribute().value().getDefaultValue() * (multiplier - 1.0D);
        // Leave external flat and multiplicative reach bonuses unchanged.
        updateModifierAmount(attribute, id, factor > 0 ? delta / factor : 0.0D,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static boolean updateModifierAmount(AttributeInstance attribute, ResourceLocation id,
                                                double amount, AttributeModifier.Operation operation) {
        var existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.00001D) {
            return false;
        }
        if (existing == null && Math.abs(amount) < 0.00001D) {
            return false;
        }
        attribute.removeModifier(id);
        if (Math.abs(amount) > 0.00001D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
        return true;
    }
}

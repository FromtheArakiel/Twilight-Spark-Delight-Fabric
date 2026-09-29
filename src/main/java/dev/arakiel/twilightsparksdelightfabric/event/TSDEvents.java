package dev.arakiel.twilightsparksdelightfabric.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.TSDUtil;
import dev.arakiel.twilightsparksdelightfabric.common.block.KitchenBlocks;
import dev.arakiel.twilightsparksdelightfabric.common.blockentity.TSDBlockEntities;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Interaction, structure and world behaviour of the mod.
 *
 * <p>The NeoForge edition used several bus events for this. Fabric exposes
 * player interactions through callbacks and the remaining hooks are invoked
 * from the mixin package instead.</p>
 */
public final class TSDEvents {
    private static final String PACIFIED_TARGET = "TsdPickledBrackenPacifiedTarget";
    private static final String PACIFIED_TICKS = "TsdPickledBrackenPacifiedTicks";
    private static final String SPECIAL_RABBIT_TAG = "TwilightSparkDelightSpecialKillerRabbit";
    private static final String PEDESTAL_POS = "twilightsparksdelightfabric.pedestal_pos";
    private static final String CREATIVE_DISPLAY = "twilightsparksdelightfabric.creative_display";
    private static final float NORMAL_PEDESTAL_DAMAGE = 1.0F;
    private static final float SLOWED_PEDESTAL_DAMAGE = 3.0F;

    private TSDEvents() {
    }

    // ------------------------------------------------------------------
    // Block interactions
    // ------------------------------------------------------------------

    /** Handles the cutting board, the trophy pedestal and colony planting. */
    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
                                               BlockHitResult hit) {
        InteractionResult cutting = experimentCuttingBoard(player, level, hand, hit);
        if (cutting != InteractionResult.PASS) {
            return cutting;
        }
        InteractionResult pedestal = pedestalInteraction(player, level, hand, hit);
        if (pedestal != InteractionResult.PASS) {
            return pedestal;
        }
        return plantColony(player, level, hand, hit);
    }

    private static InteractionResult experimentCuttingBoard(Player player, Level level, InteractionHand hand,
                                                            BlockHitResult hit) {
        if (TSDConfig.EXPERIMENT_WORKSTATION.get() != TSDConfig.ExperimentWorkstation.CUTTING_BOARD) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(hit.getBlockPos())
                instanceof vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity board)) {
            return InteractionResult.PASS;
        }
        ItemStack source = board.getStoredItem();
        ResourceLocation target = dev.arakiel.twilightsparksdelightfabric.common.experiment.Experiment250Logic
                .resolveTarget(held, source);
        var result = dev.arakiel.twilightsparksdelightfabric.common.experiment.Experiment250Logic
                .calculate(held, target);
        if (!result.valid()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!dev.arakiel.twilightsparksdelightfabric.common.experiment.Experiment250Logic
                .consume(held, result)) {
            return InteractionResult.PASS;
        }
        // Keep the real board's extraction, synchronisation, particles and sound.
        board.removeItem();
        board.spawnCuttingParticles(serverLevel, hit.getBlockPos(), source);
        board.playProcessingSound(SoundEvents.SLIME_SQUISH, source, held);
        BlockPos pos = hit.getBlockPos();
        ItemEntity output = new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.4D,
                pos.getZ() + 0.5D,
                new ItemStack(BuiltInRegistries.ITEM.get(target), result.outputCount()));
        output.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(output);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult pedestalInteraction(Player player, Level level, InteractionHand hand,
                                                         BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).is(twilightforest.init.TFBlocks.TROPHY_PEDESTAL.get())) {
            return InteractionResult.PASS;
        }
        ItemEntity display = findPedestalDisplay(level, pos);
        if (display != null) {
            if (!level.isClientSide) {
                ItemStack stack = display.getItem().copyWithCount(1);
                boolean createdInCreative = TSDPersistentData.of(display).getBoolean(CREATIVE_DISPLAY);
                display.discard();
                if (!createdInCreative && !player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        ItemStack held = player.getItemInHand(hand);
        if (!isPedestalDisplayItem(held)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack displayed = held.copyWithCount(1);
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.35D,
                    pos.getZ() + 0.5D, displayed);
            var data = TSDPersistentData.of(entity);
            data.putLong(PEDESTAL_POS, pos.asLong());
            data.putBoolean(CREATIVE_DISPLAY, player.getAbilities().instabuild);
            configurePedestalDisplay(entity, pos);
            level.addFreshEntity(entity);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult plantColony(Player player, Level level, InteractionHand hand,
                                                 BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP
                || !level.getBlockState(hit.getBlockPos()).is(
                        vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL.get())) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        net.minecraft.world.level.block.Block colony;
        if (held.is(TSDRegistry.Items.LABYRINTH_MUSHROOM)) {
            colony = TSDRegistry.Blocks.LABYRINTH_MUSHROOM_COLONY;
        } else if (held.is(TSDRegistry.Items.BRACKEN)
                || held.is(twilightforest.init.TFBlocks.FIDDLEHEAD.get().asItem())) {
            colony = TSDRegistry.Blocks.TWILIGHT_BRACKEN_COLONY;
        } else {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos().above();
        if (!level.isEmptyBlock(pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)
                || !level.mayInteract(player, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, colony.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Peacock feather fan marks every unripe jar in a small radius. */
    public static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(twilightforest.init.TFItems.PEACOCK_FEATHER_FAN.get())
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockPos min = player.blockPosition().offset(-4, -4, -4);
        BlockPos max = player.blockPosition().offset(4, 4, 4);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (serverLevel.getBlockEntity(pos) instanceof TSDBlockEntities.UnripeJar jar) {
                jar.markPeacockFanUsed();
            }
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------
    // Entity interactions
    // ------------------------------------------------------------------

    public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
                                                Entity entity, EntityHitResult hit) {
        InteractionResult rabbit = convertRabbit(player, level, hand, entity);
        if (rabbit != InteractionResult.PASS) {
            return rabbit;
        }
        return milkQuestRam(player, level, hand, entity);
    }

    private static InteractionResult convertRabbit(Player player, Level level, InteractionHand hand,
                                                  Entity target) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide
                || !held.is(twilightforest.init.TFItems.TRANSFORMATION_POWDER.get())
                || !isRabbitTarget(target)
                || player.getRandom().nextFloat()
                >= TSDConfig.RABBIT_POCKET_WATCH_KILLER_RABBIT_CHANCE.get()) {
            return InteractionResult.PASS;
        }
        if (convertToKillerRabbit(target) == null) {
            return InteractionResult.PASS;
        }
        if (!player.isCreative()) {
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult milkQuestRam(Player player, Level level, InteractionHand hand,
                                                  Entity target) {
        ItemStack held = player.getItemInHand(hand);
        if (!(target instanceof twilightforest.entity.passive.QuestRam) || !held.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            TSDUtil.give(player, new ItemStack(TSDRegistry.Items.QUEST_RAM_MILK));
            level.playSound(null, target.blockPosition(), SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                TSDRegistry.Triggers.MILK_QUEST_RAM.trigger(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------
    // Mob pacification
    // ------------------------------------------------------------------

    public static boolean isHostileTo(Mob mob, Player player) {
        return mob.isAlive() && mob instanceof Enemy && !isBoss(mob.getType())
                && (mob.getTarget() == player || mob.getLastHurtByMob() == player
                || memoryValue(mob, MemoryModuleType.ATTACK_TARGET) == player);
    }

    public static void pacify(Mob mob, Player player, int ticks) {
        if (mob.level().isClientSide || ticks <= 0) {
            return;
        }
        var data = TSDPersistentData.of(mob);
        data.putUUID(PACIFIED_TARGET, player.getUUID());
        data.putInt(PACIFIED_TICKS, ticks);
        clearPlayerTargets(mob);
    }

    /** Called from the mob tick mixin for every loaded mob. */
    public static void tickPacification(Mob mob) {
        if (mob.level().isClientSide) {
            return;
        }
        var data = TSDPersistentData.of(mob);
        int ticks = data.getInt(PACIFIED_TICKS);
        if (ticks <= 0) {
            return;
        }
        clearPlayerTargets(mob);
        if (ticks == 1) {
            clearPacification(mob);
        } else {
            data.putInt(PACIFIED_TICKS, ticks - 1);
        }
    }

    /** Called before a mob picks a new target. */
    public static boolean shouldCancelTargetChange(Mob mob, LivingEntity newTarget) {
        return !mob.level().isClientSide && isPacified(mob, newTarget);
    }

    /** Called when a pacified mob is attacked by its pacifier. */
    public static void onPacifiedMobAttacked(Mob mob, Entity attacker) {
        if (!mob.level().isClientSide && attacker instanceof Player && isPacified(mob, (LivingEntity) attacker)) {
            clearPacification(mob);
        }
    }

    private static void clearPlayerTargets(Mob mob) {
        if (isPacified(mob, mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (isPacified(mob, mob.getLastHurtByMob())) {
            mob.setLastHurtByMob(null);
        }
        if (isPacified(mob, memoryValue(mob, MemoryModuleType.ATTACK_TARGET))) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            mob.getNavigation().stop();
        }
        if (isPacified(mob, memoryValue(mob, MemoryModuleType.HURT_BY_ENTITY))) {
            mob.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        }
    }

    private static LivingEntity memoryValue(Mob mob, MemoryModuleType<LivingEntity> type) {
        var memory = mob.getBrain().getMemoryInternal(type);
        return memory == null ? null : memory.orElse(null);
    }

    private static boolean isPacified(Mob mob, LivingEntity target) {
        var data = TSDPersistentData.of(mob);
        return target instanceof Player && data.getInt(PACIFIED_TICKS) > 0 && data.hasUUID(PACIFIED_TARGET)
                && target.getUUID().equals(data.getUUID(PACIFIED_TARGET));
    }

    private static void clearPacification(Mob mob) {
        var data = TSDPersistentData.of(mob);
        data.remove(PACIFIED_TARGET);
        data.remove(PACIFIED_TICKS);
    }

    // ------------------------------------------------------------------
    // Rabbit pocket watch
    // ------------------------------------------------------------------

    public static boolean isSpecialKillerRabbit(Entity entity) {
        return entity instanceof net.minecraft.world.entity.animal.Rabbit
                && TSDPersistentData.of(entity).getBoolean(SPECIAL_RABBIT_TAG);
    }

    public static boolean canDropPocketWatch(LivingEntity entity, Player player) {
        return entity != null && player != null && isSpecialKillerRabbit(entity)
                && TSDUtil.isKnife(player.getMainHandItem());
    }

    /** Gives the quiet movement bonuses of the pocket watch. */
    public static void tickPocketWatch(Player player) {
        if (player.level().isClientSide || !hasPocketWatchInHotbar(player)) {
            return;
        }
        giveQuietEffect(player, MobEffects.MOVEMENT_SPEED);
        giveQuietEffect(player, MobEffects.JUMP);
        if (hasPocketWatchInHand(player)) {
            giveQuietEffect(player, MobEffects.DIG_SPEED);
        }
    }

    public static boolean blocksMiningFatigue(Player player) {
        return hasPocketWatchInHotbar(player);
    }

    /** Projectile hitting the shield of a pocket watch holder disables it. */
    public static boolean onPocketWatchShieldHit(Player target, Entity attacker,
                                                 boolean projectile) {
        if (!projectile || !(attacker instanceof net.minecraft.server.level.ServerPlayer shooter)
                || !hasPocketWatchInHand(shooter)
                || !target.isBlocking()
                || !target.getUseItem().is(Items.SHIELD)) {
            return false;
        }
        target.disableShield();
        target.getCooldowns().addCooldown(Items.SHIELD, 100);
        TSDRegistry.Triggers.GUNFIRE_BREAKS_RABBIT_WATCH.trigger(shooter);
        return true;
    }

    private static net.minecraft.world.entity.animal.Rabbit convertToKillerRabbit(Entity target) {
        Level level = target.level();
        var originalName = target.getCustomName();
        net.minecraft.world.entity.animal.Rabbit rabbit;
        if (target instanceof net.minecraft.world.entity.animal.Rabbit existing) {
            rabbit = existing;
            rabbit.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.EVIL);
        } else {
            rabbit = EntityType.RABBIT.create(level);
            if (rabbit == null) {
                return null;
            }
            rabbit.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            if (target instanceof LivingEntity living && living.isBaby()) {
                rabbit.setBaby(true);
            }
            target.discard();
            level.addFreshEntity(rabbit);
            rabbit.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.EVIL);
        }
        // Keep the converted rabbit unnamed; the marker only drives addon behaviour.
        rabbit.setCustomName(originalName);
        TSDPersistentData.of(rabbit).putBoolean(SPECIAL_RABBIT_TAG, true);
        return rabbit;
    }

    private static boolean isRabbitTarget(Entity entity) {
        return entity instanceof net.minecraft.world.entity.animal.Rabbit
                || entity instanceof twilightforest.entity.passive.DwarfRabbit;
    }

    public static boolean hasPocketWatchInHand(Player player) {
        return player.getMainHandItem().is(twilightforest.init.TFItems.POCKET_WATCH.get())
                || player.getOffhandItem().is(twilightforest.init.TFItems.POCKET_WATCH.get());
    }

    private static boolean hasPocketWatchInHotbar(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(twilightforest.init.TFItems.POCKET_WATCH.get())) {
                return true;
            }
        }
        return player.getOffhandItem().is(twilightforest.init.TFItems.POCKET_WATCH.get());
    }

    private static void giveQuietEffect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>
            effect) {
        player.addEffect(new MobEffectInstance(effect, 40, 0, true, false, false));
    }

    // ------------------------------------------------------------------
    // Trophy pedestal network
    // ------------------------------------------------------------------

    /** Called every level tick; drives the Experiment 250 pedestal network. */
    public static void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        List<ItemEntity> displays = getPedestalDisplays(level);
        for (ItemEntity display : displays) {
            BlockPos pos = pedestalPos(display);
            if (pos == null || !level.getBlockState(pos).is(twilightforest.init.TFBlocks.TROPHY_PEDESTAL.get())
                    || !isPedestalDisplayItem(display.getItem())) {
                releasePedestalDisplay(display);
                continue;
            }
            configurePedestalDisplay(display, pos);
        }
        displays.removeIf(Entity::isRemoved);
        if (level.getGameTime() % 60L == 0L) {
            runPedestalNetwork(level, displays);
        }
    }

    private static void runPedestalNetwork(ServerLevel level, List<ItemEntity> displays) {
        List<ItemEntity> scepters = new ArrayList<>();
        List<ItemEntity> experiments = new ArrayList<>();
        for (ItemEntity display : displays) {
            ItemStack stack = display.getItem();
            if (stack.is(twilightforest.init.TFItems.LIFEDRAIN_SCEPTER.get())) {
                scepters.add(display);
            } else if (stack.getItem()
                    instanceof dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250
                    && dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250.getActivity(stack)
                    + 1.0E-9D
                    < dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250.getCapacity(stack)) {
                experiments.add(display);
            }
        }
        for (ItemEntity scepter : scepters) {
            ItemEntity experiment = selectExperiment(scepter, experiments);
            if (experiment != null) {
                drainCreature(level, scepter, experiment);
            }
        }
    }

    private static ItemEntity selectExperiment(ItemEntity scepter, List<ItemEntity> experiments) {
        BlockPos source = pedestalPos(scepter);
        if (source == null) {
            return null;
        }
        return experiments.stream()
                .filter(candidate -> isInNetwork(source, pedestalPos(candidate)))
                .filter(candidate -> dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250
                        .getActivity(candidate.getItem())
                        < dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250
                                .getCapacity(candidate.getItem()))
                .min(java.util.Comparator
                        .comparingDouble((ItemEntity candidate) ->
                                horizontalDistanceSq(source, pedestalPos(candidate)))
                        .thenComparingInt(candidate -> directionPriority(source, pedestalPos(candidate)))
                        .thenComparingInt(candidate -> pedestalPos(candidate).getX())
                        .thenComparingInt(candidate -> pedestalPos(candidate).getZ()))
                .orElse(null);
    }

    private static void drainCreature(ServerLevel level, ItemEntity scepter, ItemEntity experiment) {
        BlockPos pedestal = pedestalPos(scepter);
        if (pedestal == null) {
            return;
        }
        AABB search = new AABB(pedestal).inflate(3.0D);
        List<LivingEntity> normal = new ArrayList<>();
        List<LivingEntity> slowed = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, TSDEvents::isValidTarget)) {
            if (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                slowed.add(target);
            } else {
                normal.add(target);
            }
        }
        List<LivingEntity> candidates = slowed.isEmpty() ? normal : slowed;
        if (candidates.isEmpty()) {
            return;
        }
        LivingEntity target = candidates.get(level.random.nextInt(candidates.size()));
        boolean wasSlowed = target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        var damage = new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TSDRegistry.DamageTypes.LIFEDRAIN_PEDESTAL));
        boolean damaged = target.hurt(damage, wasSlowed ? SLOWED_PEDESTAL_DAMAGE : NORMAL_PEDESTAL_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, true, true));
        if (damaged) {
            ItemStack charged = experiment.getItem().copy();
            dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250.addActivity(charged,
                    wasSlowed ? 1.5D : 0.5D);
            experiment.setItem(charged);
        }
        pedestalTrail(level, scepter.position().add(0, 0.25, 0), target.getEyePosition());
        pedestalTrail(level, scepter.position().add(0, 0.25, 0), experiment.position().add(0, 0.25, 0));
    }

    private static boolean isValidTarget(LivingEntity target) {
        return target.isAlive() && !(target instanceof Player) && !target.hasCustomName()
                && !(target instanceof net.minecraft.world.entity.npc.AbstractVillager)
                && !(target instanceof net.minecraft.world.entity.monster.Witch)
                && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand)
                && !(target instanceof net.minecraft.world.entity.OwnableEntity ownable
                        && ownable.getOwnerUUID() != null)
                && !(target instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse
                        && horse.isTamed())
                && !isBoss(target.getType())
                && !target.getType().equals(twilightforest.init.TFEntities.QUEST_RAM.get());
    }

    /** Registry id based boss check, mirroring the legacy fallback list. */
    public static boolean isBoss(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }
        if (id.equals(ResourceLocation.withDefaultNamespace("ender_dragon"))
                || id.equals(ResourceLocation.withDefaultNamespace("wither"))
                || id.equals(ResourceLocation.withDefaultNamespace("warden"))) {
            return true;
        }
        return id.getNamespace().equals("twilightforest") && Set.of("naga", "lich", "minoshroom", "hydra",
                "knight_phantom", "snow_queen", "ur_ghast", "alpha_yeti").contains(id.getPath());
    }

    private static List<ItemEntity> getPedestalDisplays(ServerLevel level) {
        List<ItemEntity> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && TSDPersistentData.of(item).contains(PEDESTAL_POS)
                    && !item.isRemoved()) {
                result.add(item);
            }
        }
        return result;
    }

    private static ItemEntity findPedestalDisplay(Level level, BlockPos pos) {
        AABB bounds = new AABB(pos).inflate(0.75D).expandTowards(0.0D, 1.5D, 0.0D);
        return level.getEntitiesOfClass(ItemEntity.class, bounds,
                entity -> pos.equals(pedestalPos(entity))).stream().findFirst().orElse(null);
    }

    private static void configurePedestalDisplay(ItemEntity entity, BlockPos pos) {
        entity.setPos(pos.getX() + 0.5D, pos.getY() + 1.35D, pos.getZ() + 0.5D);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.setNeverPickUp();
        entity.setUnlimitedLifetime();
        entity.setInvulnerable(true);
    }

    private static void releasePedestalDisplay(ItemEntity display) {
        if (!TSDPersistentData.of(display).getBoolean(CREATIVE_DISPLAY)) {
            ItemStack stack = display.getItem().copyWithCount(1);
            display.level().addFreshEntity(new ItemEntity(display.level(), display.getX(), display.getY(),
                    display.getZ(), stack));
        }
        display.discard();
    }

    private static BlockPos pedestalPos(ItemEntity entity) {
        var data = TSDPersistentData.of(entity);
        return data.contains(PEDESTAL_POS) ? BlockPos.of(data.getLong(PEDESTAL_POS)) : null;
    }

    private static boolean isPedestalDisplayItem(ItemStack stack) {
        return stack.is(twilightforest.init.TFItems.LIFEDRAIN_SCEPTER.get())
                || stack.getItem()
                instanceof dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems.Experiment250;
    }

    private static boolean isInNetwork(BlockPos origin, BlockPos target) {
        return target != null && origin.getY() == target.getY() && !origin.equals(target)
                && Math.abs(origin.getX() - target.getX()) <= 2
                && Math.abs(origin.getZ() - target.getZ()) <= 2;
    }

    private static double horizontalDistanceSq(BlockPos origin, BlockPos target) {
        int dx = target.getX() - origin.getX();
        int dz = target.getZ() - origin.getZ();
        return dx * dx + dz * dz;
    }

    private static int directionPriority(BlockPos origin, BlockPos target) {
        int dx = target.getX() - origin.getX();
        int dz = target.getZ() - origin.getZ();
        return Math.abs(dz) >= Math.abs(dx) ? (dz < 0 ? 0 : 2) : (dx > 0 ? 1 : 3);
    }

    private static void pedestalTrail(ServerLevel level, Vec3 from, Vec3 to) {
        var particle = net.minecraft.core.particles.ColorParticleOption.create(
                net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0.75F, 0.05F, 0.05F);
        for (int step = 0; step <= 20; step++) {
            Vec3 point = from.lerp(to, step / 20.0D);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------
    // Armored giant skillet variant
    // ------------------------------------------------------------------

    public static final String SKILLET_VARIANT_TAG = "TwilightSparkDelightSkilletArmoredGiant";
    private static final String ROLLED_TAG = "TwilightSparkDelightSkilletArmoredGiantRolled";

    public static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof twilightforest.entity.monster.ArmoredGiant giant)) {
            return;
        }
        var data = TSDPersistentData.of(giant);
        if (!data.getBoolean(ROLLED_TAG)) {
            data.putBoolean(ROLLED_TAG, true);
            data.putBoolean(SKILLET_VARIANT_TAG, giant.getRandom().nextDouble()
                    < TSDConfig.ARMORED_GIANT_SKILLET_VARIANT_CHANCE.get());
        }
        if (data.getBoolean(SKILLET_VARIANT_TAG)) {
            giant.setItemSlot(EquipmentSlot.HEAD,
                    new ItemStack(vectorwing.farmersdelight.common.registry.ModItems.SKILLET.get()));
            giant.setItemSlot(EquipmentSlot.MAINHAND,
                    new ItemStack(vectorwing.farmersdelight.common.registry.ModItems.IRON_KNIFE.get()));
            giant.setDropChance(EquipmentSlot.HEAD, 0.0F);
            giant.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    public static boolean isSkilletVariant(Entity entity) {
        return entity instanceof twilightforest.entity.monster.ArmoredGiant
                && TSDPersistentData.of(entity).getBoolean(SKILLET_VARIANT_TAG);
    }

    /** Surface position of a giant stove output, used by the stove mixin. */
    public static Vec3 stoveOutputPosition(BlockPos pos, BlockState state) {
        var bounds = KitchenBlocks.Structure.bounds(pos, state);
        var center = bounds.getCenter();
        return new Vec3(center.x, bounds.maxY + 0.1D, center.z);
    }
}

package dev.arakiel.twilightsparksdelightfabric.event;

import java.util.List;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.TSDUtil;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Loot and drop adjustments.
 *
 * <p>The NeoForge edition used {@code BlockDropsEvent}, {@code LivingDropsEvent}
 * and {@code LootTableLoadEvent}. Fabric offers block break callbacks and loot
 * table callbacks; entity drops are adjusted right after the entity died by
 * looking at the item entities spawned in that same tick.</p>
 */
public final class TSDLootEvents {
    private static final ResourceLocation FIDDLEHEAD =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "fiddlehead");
    private static final ResourceKey<LootTable> DARK_TOWER_BOSS = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("twilightforest", "darktower_boss"));
    private static final ResourceKey<LootTable> HILL_1 = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("twilightforest", "hill_1"));
    private static final ResourceKey<LootTable> HILL_2 = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("twilightforest", "hill_2"));
    private static final ResourceKey<LootTable> HILL_3 = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("twilightforest", "hill_3"));

    private TSDLootEvents() {
    }

    /** Fiddlehead blocks drop bracken when harvested with a knife. */
    public static void onBlockBreak(Level level, Player player, Block block,
                                    net.minecraft.core.BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)
                || !FIDDLEHEAD.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!TSDUtil.isKnife(tool)) {
            return;
        }
        var enchantments = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool) > 0) {
            return;
        }
        int looting = EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.LOOTING), tool);
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.FORTUNE), tool);
        int amount = 1 + Math.max(0, looting) + Math.max(0, fortune) * 2;
        serverLevel.addFreshEntity(new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, new ItemStack(TSDRegistry.Items.BRACKEN, amount)));
    }

    /** Adjusts the drops of Twilight Forest mobs right after death. */
    public static void onAfterDeath(LivingEntity entity,
                                    net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Player killer = source.getEntity() instanceof Player player ? player : null;
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                entity.getBoundingBox().inflate(2.0D), item -> item.tickCount <= 1);

        if (TSDEvents.canDropPocketWatch(entity, killer)) {
            addDrop(level, entity, new ItemStack(twilightforest.init.TFItems.POCKET_WATCH.get()));
            if (killer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                TSDRegistry.Triggers.LATE_FOR_DATE.trigger(serverPlayer);
            }
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null || !"twilightforest".equals(id.getNamespace())) {
            return;
        }

        if (TSDEvents.isSkilletVariant(entity)
                && entity.getRandom().nextDouble() < TSDConfig.ARMORED_GIANT_KITCHEN_SET_DROP_CHANCE.get()) {
            addDrop(level, entity, new ItemStack(TSDRegistry.Blocks.GIANTS_STOVE));
            addDrop(level, entity, new ItemStack(TSDRegistry.Blocks.GIANTS_COOKING_POT));
        }

        if ("wild_boar".equals(id.getPath())) {
            replaceDrop(drops, Items.PORKCHOP, entity.isOnFire()
                    ? TSDRegistry.Items.COOKED_WILD_BOAR_MEAT : TSDRegistry.Items.RAW_WILD_BOAR_MEAT);
            replaceDrop(drops, Items.COOKED_PORKCHOP, TSDRegistry.Items.COOKED_WILD_BOAR_MEAT);
        } else if ("bighorn_sheep".equals(id.getPath())) {
            replaceDrop(drops, Items.MUTTON, entity.isOnFire()
                    ? TSDRegistry.Items.COOKED_BIGHORN_MUTTON : TSDRegistry.Items.RAW_BIGHORN_MUTTON);
            replaceDrop(drops, Items.COOKED_MUTTON, TSDRegistry.Items.COOKED_BIGHORN_MUTTON);
        }

        ItemStack leg = switch (id.getPath()) {
            case "helmet_crab" -> new ItemStack(entity.isOnFire()
                    ? TSDRegistry.Items.COOKED_HERMIT_CRAB_LEG : TSDRegistry.Items.HERMIT_CRAB_LEG);
            case "pinch_beetle" -> new ItemStack(entity.isOnFire()
                    ? TSDRegistry.Items.COOKED_PINCH_BEETLE_LEG : TSDRegistry.Items.PINCH_BEETLE_LEG);
            case "slime_beetle" -> new ItemStack(entity.isOnFire()
                    ? TSDRegistry.Items.COOKED_SLIME_BEETLE_LEG : TSDRegistry.Items.SLIME_BEETLE_LEG);
            case "fire_beetle" -> new ItemStack(entity.isOnFire()
                    ? TSDRegistry.Items.COOKED_FIRE_BEETLE_LEG : TSDRegistry.Items.FIRE_BEETLE_LEG);
            default -> ItemStack.EMPTY;
        };
        ItemEntity spawnedLeg = null;
        if (!leg.isEmpty()) {
            if ("helmet_crab".equals(id.getPath())) {
                drops.removeIf(drop -> drop.getItem().is(Items.COD)
                        || drop.getItem().is(Items.SALMON)
                        || drop.getItem().is(Items.TROPICAL_FISH)
                        || drop.getItem().is(Items.PUFFERFISH));
            }
            leg.setCount(2 + entity.getRandom().nextInt(5) + getLootingLevel(killer));
            spawnedLeg = addDrop(level, entity, leg);
            if (killer != null && TSDUtil.isKnife(killer.getMainHandItem())) {
                addDrop(level, entity, leg.copyWithCount(2));
            }
        }

        final ItemEntity spawnedLegRef = spawnedLeg;
        if (killer != null && TSDUtil.isKnife(killer.getMainHandItem())) {
            switch (id.getPath()) {
                case "fire_beetle" -> addDrop(level, entity,
                        new ItemStack(TSDRegistry.Items.FIRE_BEETLE_FLAME_SAC));
                case "slime_beetle" -> addDrop(level, entity,
                        new ItemStack(TSDRegistry.Items.SLIME_BEETLE_HONEY_GLAND));
                case "helmet_crab" -> {
                    // The living crab replaces every other drop, including the
                    // legs added above and the vanilla fish.
                    drops.forEach(ItemEntity::discard);
                    if (spawnedLegRef != null) {
                        spawnedLegRef.discard();
                    }
                    addDrop(level, entity, new ItemStack(TSDRegistry.Items.HERMIT_CRAB));
                }
                case "redcap", "redcap_sapper" -> addDrop(level, entity,
                        new ItemStack(TSDRegistry.Items.REDCAP_SPICE));
                case "death_tome" -> addDrop(level, entity,
                        new ItemStack(twilightforest.init.TFItems.TRANSFORMATION_POWDER.get()));
                default -> {
                }
            }
        }

        switch (id.getPath()) {
            case "snow_queen" -> addDrop(level, entity, new ItemStack(TSDRegistry.Items.GELID_CRYSTAL,
                    6 + entity.getRandom().nextInt(5) + getLootingLevel(killer)));
            case "minoshroom" -> addDrop(level, entity, new ItemStack(TSDRegistry.Items.LABYRINTH_MUSHROOM,
                    6 + entity.getRandom().nextInt(5) + getLootingLevel(killer)));
            case "knight_phantom" -> addDrop(level, entity,
                    new ItemStack(TSDRegistry.Items.EXPERIMENT_PROTOTYPE,
                            1 + entity.getRandom().nextInt(2) + getLootingLevel(killer)));
            case "quest_ram" -> {
                if (entity.isOnFire()) {
                    addDrop(level, entity, new ItemStack(TSDRegistry.Items.COOKED_BIGHORN_MUTTON, 32));
                    addDrop(level, entity, new ItemStack(TSDRegistry.Items.QUEST_RAM_CHEESE, 10));
                    awardNearby(level, entity, "burning_ram_lord");
                    if (killer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        TSDRegistry.Triggers.BURNING_RAM_LORD.trigger(serverPlayer);
                    }
                }
            }
            default -> {
            }
        }
    }

    /** Loot table additions of the mod. */
    public static void onLootTableModify(ResourceKey<LootTable> key, LootTable.Builder builder) {
        if (key.equals(DARK_TOWER_BOSS)) {
            builder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(TSDRegistry.Items.EXPERIMENT_234)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 8.0F))))
                    );
            return;
        }
        // A dedicated, weighted pool keeps the pocket watch rare without
        // touching the existing village loot pools.
        if (key.equals(HILL_1) || key.equals(HILL_2)) {
            builder.withPool(pocketWatchPool(1));
        } else if (key.equals(HILL_3)) {
            builder.withPool(pocketWatchPool(5));
        }
    }

    private static LootPool.Builder pocketWatchPool(int weight) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(twilightforest.init.TFItems.POCKET_WATCH.get())
                        .setWeight(weight))
                .add(EmptyLootItem.emptyItem().setWeight(12));
    }

    private static ItemEntity addDrop(Level level, LivingEntity entity, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        level.addFreshEntity(item);
        return item;
    }

    private static void replaceDrop(List<ItemEntity> drops, net.minecraft.world.item.Item original,
                                    net.minecraft.world.item.Item replacement) {
        for (ItemEntity drop : drops) {
            if (drop.getItem().is(original)) {
                drop.setItem(new ItemStack(replacement, drop.getItem().getCount()));
            }
        }
    }

    private static int getLootingLevel(Player player) {
        if (player == null) {
            return 0;
        }
        var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.LOOTING), player.getMainHandItem());
    }

    private static void awardNearby(ServerLevel level, Entity entity, String advancementId) {
        var advancement = level.getServer().getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric", advancementId));
        if (advancement == null) {
            return;
        }
        for (Player player : level.players()) {
            if (player.getBoundingBox().intersects(entity.getBoundingBox().inflate(16.0D))
                    && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.getAdvancements().award(advancement, advancementId);
            }
        }
    }

    /** Registers the loot table callback. */
    public static void register() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) ->
                onLootTableModify(key, builder));
    }
}

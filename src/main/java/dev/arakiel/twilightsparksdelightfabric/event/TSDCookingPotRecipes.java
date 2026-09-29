package dev.arakiel.twilightsparksdelightfabric.event;

import java.util.ArrayList;
import java.util.List;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.data.TSDPersistentData;
import dev.arakiel.twilightsparksdelightfabric.common.experiment.Experiment250Logic;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.mixin.CookingPotAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dynamic cooking pot operations of Experiment 250.
 *
 * <p>Inputs and outputs depend on stack components, activity and the loaded
 * configuration, so they are kept outside Farmer's Delight's data recipes.</p>
 */
public final class TSDCookingPotRecipes {
    private static final int INPUT_SLOTS = 6;
    private static final int MEAL_SLOT = 6;
    private static final int OUTPUT_SLOT = 8;
    private static final int PROCESS_TIME = 100;
    private static final String MODE = "twilightsparksdelightfabric.cooking_pot_mode";
    private static final String PROGRESS = "twilightsparksdelightfabric.cooking_pot_progress";
    private static final String KEY = "twilightsparksdelightfabric.cooking_pot_key";
    private static final String LAST_TICK = "twilightsparksdelightfabric.cooking_pot_last_tick";

    private TSDCookingPotRecipes() {
    }

    public static void tickPot(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot) {
        if (pot.getLevel() == null || pot.getLevel().isClientSide) {
            return;
        }
        var data = TSDPersistentData.of(pot);
        List<ItemStack> inputs = inputs(pot);
        Operation operation = findOperation(pot, inputs);
        if (operation == null || !pot.isHeated() || !canStore(pot)) {
            reset(pot, data);
            return;
        }
        long time = pot.getLevel().getGameTime();
        if (data.contains(LAST_TICK) && data.getLong(LAST_TICK) == time) {
            return;
        }
        data.putLong(LAST_TICK, time);

        String key = operation.key(pot);
        if (!key.equals(data.getString(KEY)) || !operation.mode().equals(data.getString(MODE))) {
            data.putString(KEY, key);
            data.putString(MODE, operation.mode());
            data.putInt(PROGRESS, 0);
        }

        int progress = data.getInt(PROGRESS) + 1;
        data.putInt(PROGRESS, progress);
        var cookingData = ((CookingPotAccess) pot).tsd$getCookingData();
        cookingData.set(0, progress);
        cookingData.set(1, PROCESS_TIME);
        if (progress < PROCESS_TIME) {
            pot.setChanged();
            return;
        }

        if ("upgrade".equals(operation.mode())) {
            finishUpgrade(pot, operation.upgrade());
        } else {
            finishReplication(pot, operation.replication());
        }
        reset(pot, data);
    }

    private static Operation findOperation(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot,
                                          List<ItemStack> inputs) {
        if (inputs.size() == 2 || inputs.size() == 4) {
            Experiment250Logic.UpgradeResult upgrade = Experiment250Logic.upgrade(inputs);
            if (upgrade.valid()) {
                return new Operation("upgrade", upgrade, null);
            }
        }
        if (inputs.size() == 2
                && TSDConfig.EXPERIMENT_WORKSTATION.get() == TSDConfig.ExperimentWorkstation.COOKING_POT) {
            int experimentSlot = -1;
            int sourceSlot = -1;
            for (int slot = 0; slot < INPUT_SLOTS; slot++) {
                ItemStack stack = pot.getInventory().getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() instanceof TSDItems.Experiment250 && experimentSlot < 0) {
                    experimentSlot = slot;
                } else if (sourceSlot < 0) {
                    sourceSlot = slot;
                }
            }
            if (experimentSlot >= 0 && sourceSlot >= 0) {
                ItemStack experiment = pot.getInventory().getStackInSlot(experimentSlot);
                ResourceLocation target = Experiment250Logic.resolveTarget(experiment,
                        pot.getInventory().getStackInSlot(sourceSlot));
                Experiment250Logic.ReplicationResult replication =
                        Experiment250Logic.calculate(experiment, target);
                if (replication.valid()) {
                    return new Operation("replication", null,
                            new Replication(experimentSlot, sourceSlot, target, replication));
                }
            }
        }
        return null;
    }

    private static boolean canStore(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot) {
        return pot.getInventory().getStackInSlot(MEAL_SLOT).isEmpty()
                && pot.getInventory().getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    private static void finishUpgrade(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot,
                                      Experiment250Logic.UpgradeResult upgrade) {
        ((CookingPotAccess) pot).tsd$setMealContainer(
                new ItemStack(twilightforest.init.TFItems.TRANSFORMATION_POWDER.get()));
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            pot.getInventory().setStackInSlot(slot, ItemStack.EMPTY);
        }
        List<ItemStack> outputs = upgrade.outputs();
        // The meal slot stores servings, exactly like unstackable soups.
        pot.getInventory().setStackInSlot(MEAL_SLOT, outputs.getFirst().copyWithCount(outputs.size()));
        pot.setChanged();
    }

    private static void finishReplication(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot,
                                          Replication replication) {
        Item item = BuiltInRegistries.ITEM.get(replication.outputId());
        if (item == Items.AIR) {
            return;
        }
        ((CookingPotAccess) pot).tsd$setMealContainer(ItemStack.EMPTY);
        ItemStack experiment = pot.getInventory().getStackInSlot(replication.experimentSlot()).copyWithCount(1);
        if (!Experiment250Logic.consume(experiment, replication.result())) {
            return;
        }
        ItemStack source = pot.getInventory().getStackInSlot(replication.sourceSlot()).copy();
        source.shrink(1);
        pot.getInventory().setStackInSlot(replication.sourceSlot(),
                source.isEmpty() ? ItemStack.EMPTY : source);
        pot.getInventory().setStackInSlot(replication.experimentSlot(), experiment);
        // This path never uses a serving container.
        pot.getInventory().setStackInSlot(OUTPUT_SLOT,
                new ItemStack(item, replication.result().outputCount()));
        pot.setChanged();
    }

    private static List<ItemStack> inputs(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot) {
        List<ItemStack> inputs = new ArrayList<>(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = pot.getInventory().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                inputs.add(stack);
            }
        }
        return inputs;
    }

    private static void reset(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot,
                              net.minecraft.nbt.CompoundTag data) {
        if (data.contains(PROGRESS) || data.contains(KEY) || data.contains(MODE)) {
            data.remove(PROGRESS);
            data.remove(KEY);
            data.remove(MODE);
            ((CookingPotAccess) pot).tsd$getCookingData().set(0, 0);
            pot.setChanged();
        }
    }

    private record Operation(String mode, Experiment250Logic.UpgradeResult upgrade,
                             Replication replication) {
        private String key(vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity pot) {
            StringBuilder result = new StringBuilder(mode).append('|');
            for (int slot = 0; slot < INPUT_SLOTS; slot++) {
                result.append(pot.getInventory().getStackInSlot(slot)
                        .saveOptional(pot.getLevel().registryAccess())).append('|');
            }
            return result.toString();
        }
    }

    private record Replication(int experimentSlot, int sourceSlot, ResourceLocation outputId,
                               Experiment250Logic.ReplicationResult result) {
    }
}

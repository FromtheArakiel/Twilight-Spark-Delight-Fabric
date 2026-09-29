package dev.arakiel.twilightsparksdelightfabric.client.integration.jei;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.math.Axis;

import dev.arakiel.twilightsparksdelightfabric.TSDConfig;
import dev.arakiel.twilightsparksdelightfabric.common.TSDUtil;
import dev.arakiel.twilightsparksdelightfabric.common.item.TSDItems;
import dev.arakiel.twilightsparksdelightfabric.registry.TSDRegistry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;

/** Every JEI category of the mod together with its ingredient renderers. */
public final class TSDJeiCategories {
    private TSDJeiCategories() {
    }

    /** Shared scaffolding of the categories. */
    public abstract static class Category<T> implements IRecipeCategory<T> {
        private final RecipeType<T> type;
        private final Component title;
        private final IDrawable icon;
        private final int width;
        private final int height;

        protected Category(IGuiHelper helper, RecipeType<T> type, String title, ItemStack icon,
                           int width, int height) {
            this.type = type;
            this.title = Component.translatable(title);
            this.icon = helper.createDrawableItemStack(icon);
            this.width = width;
            this.height = height;
        }

        @Override
        public RecipeType<T> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        protected void centeredText(GuiGraphics graphics, Component text, int y, int color) {
            var font = Minecraft.getInstance().font;
            // Long translations wrap instead of overflowing the recipe border.
            for (var line : font.split(text, width)) {
                graphics.drawString(font, line, (width - font.width(line)) / 2, y, color, false);
                y += font.lineHeight + 1;
            }
        }
    }

    // ------------------------------------------------------------------
    // Experiment 250 cultivation
    // ------------------------------------------------------------------

    public static final class UpgradeCategory extends Category<TSDJeiRecipes.Upgrade> {
        public static final ResourceLocation POT =
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "textures/gui/jei/cooking_pot.png");
        public static final ResourceLocation GUI =
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "textures/gui/cooking_pot.png");

        private final IDrawable background;
        private final IDrawable heat;
        private final IDrawable arrow;

        public UpgradeCategory(IGuiHelper helper) {
            super(helper, TSDJeiPlugin.CULTIVATION,
                    "twilightsparksdelightfabric.jei.experiment_250.upgrade.title",
                    new ItemStack(TSDRegistry.Items.EXPERIMENT_250), 116, 84);
            background = helper.createDrawable(POT, 0, 0, 116, 56);
            heat = helper.createDrawable(GUI, 176, 0, 17, 15);
            arrow = helper.createAnimatedRecipeArrow(100);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder layout, TSDJeiRecipes.Upgrade recipe,
                              IFocusGroup focuses) {
            for (int slot = 0; slot < recipe.inputCount(); slot++) {
                layout.addInputSlot(1 + slot % 3 * 18, 1 + slot / 3 * 18).addItemStack(recipe.input());
            }
            layout.addInputSlot(63, 39)
                    .addItemStack(new ItemStack(twilightforest.init.TFItems.TRANSFORMATION_POWDER.get()));
            layout.addOutputSlot(95, 10).addItemStack(recipe.output());
            layout.addOutputSlot(95, 39).addItemStack(recipe.output());
        }

        @Override
        public void draw(TSDJeiRecipes.Upgrade recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                         double mouseX, double mouseY) {
            background.draw(graphics, 0, 0);
            arrow.draw(graphics, 60, 9);
            heat.draw(graphics, 18, 39);
            centeredText(graphics, Component.translatable(
                    "twilightsparksdelightfabric.jei.experiment_250.activity_inherited"), 62, 0x555555);
        }
    }

    /** Replication page: the level table of every differentiable raw meat. */
    public static final class DifferentiationCategory extends Category<TSDJeiRecipes.Differentiation> {
        private static final DecimalFormat NUMBER = new DecimalFormat("0.##");
        private static final int WIDTH = 268;
        private static final int HEIGHT = 98;
        private static final int STATION_X = (WIDTH - 174) / 2;
        private static final int TABLE_TOP = 59;
        private static final int ROW_HEIGHT = 10;
        private static final int LABEL_WIDTH = 48;
        private static final int COLUMN_WIDTH = 36;
        private static final String[] TABLE_KEYS = {"level", "max", "each", "batch"};

        private final IDrawable pot;
        private final IDrawable board;
        private final IDrawable slot;
        private final IDrawable arrow;
        private final IDrawable heat;
        private final TSDConfig.ExperimentWorkstation mode;

        public DifferentiationCategory(IGuiHelper helper) {
            super(helper, TSDJeiPlugin.DIFFERENTIATION,
                    "twilightsparksdelightfabric.jei.experiment_250.replication.title",
                    new ItemStack(TSDRegistry.Items.EXPERIMENT_250), WIDTH, HEIGHT);
            mode = TSDConfig.EXPERIMENT_WORKSTATION.get();
            pot = helper.createDrawable(UpgradeCategory.POT, 0, 0, 116, 56);
            board = helper.createDrawable(ResourceLocation.fromNamespaceAndPath(
                    "farmersdelight", "textures/gui/jei/cutting_board.png"), 0, 0, 117, 57);
            slot = helper.getSlotDrawable();
            arrow = helper.createAnimatedRecipeArrow(100);
            heat = helper.createDrawable(UpgradeCategory.GUI, 176, 0, 17, 15);
        }

        @Override
        public void onDisplayedIngredientsUpdate(TSDJeiRecipes.Differentiation recipe,
                                                 List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
            int level = slots.stream()
                    .filter(slot -> slot.getSlotName().filter("experiment"::equals).isPresent())
                    .findFirst()
                    .flatMap(IRecipeSlotDrawable::getDisplayedItemStack)
                    .map(TSDItems.Experiment250::getLevel)
                    .orElse(1);
            ItemStack output = recipe.rows().get(level - 1).output();
            for (var slot : slots) {
                if (slot.getRole() == RecipeIngredientRole.OUTPUT) {
                    // JEI treats the same meat at different counts as one ingredient.
                    slot.createDisplayOverrides().addItemStack(output);
                }
            }
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder layout, TSDJeiRecipes.Differentiation recipe,
                              IFocusGroup focuses) {
            boolean cutting = mode == TSDConfig.ExperimentWorkstation.CUTTING_BOARD;
            boolean cooking = mode == TSDConfig.ExperimentWorkstation.COOKING_POT;
            var experiment = layout.addInputSlot(STATION_X + (cutting ? 45 : 30), cutting ? 9 : 1)
                    .addItemStacks(recipe.rows().stream().map(TSDJeiRecipes.Row::experiment).toList())
                    .setSlotName("experiment");
            var input = layout.addInputSlot(STATION_X + (cutting ? 45 : 48), cutting ? 28 : 1);
            if (TSDConfig.EXPERIMENT_BINDING_MODE_ENABLED.get()) {
                input.addIngredients(Ingredient.of(TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("c", "foods/dough"))));
                input.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "twilightsparksdelightfabric.jei.experiment_250.binding_output")));
            } else {
                input.addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(recipe.item())));
            }
            List<ItemStack> outputs = recipe.rows().stream().map(TSDJeiRecipes.Row::output).toList();
            var output = layout.addOutputSlot(STATION_X + (cutting ? 115 : 124),
                    cutting ? 21 : cooking ? 10 : 19).addItemStacks(outputs).setSlotName("result");
            if (cutting) {
                output.setStandardSlotBackground();
            }
            List<IIngredientAcceptor<?>> linked = new ArrayList<>(List.of(experiment, output));
            if (cooking) {
                linked.add(layout.addOutputSlot(STATION_X + 124, 39).addItemStacks(outputs));
            }
            // Linked slots share one cycle index, so a level can never display
            // another level's output quantity.
            layout.createFocusLink(linked.toArray(IIngredientAcceptor<?>[]::new));
            experiment.addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(Component.translatable("twilightsparksdelightfabric.jei.not_consumed"));
                view.getDisplayedItemStack().ifPresent(stack -> {
                    var row = recipe.rows().get(TSDItems.Experiment250.getLevel(stack) - 1);
                    tooltip.add(Component.translatable(
                            "twilightsparksdelightfabric.jei.experiment_250.cost_per_copy",
                            NUMBER.format(row.cost())));
                });
            });
        }

        @Override
        public void draw(TSDJeiRecipes.Differentiation recipe, IRecipeSlotsView slots,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.pose().pushPose();
            graphics.pose().translate(STATION_X, 0, 0);
            switch (mode) {
                case COOKING_POT -> {
                    pot.draw(graphics, 29, 0);
                    arrow.draw(graphics, 89, 9);
                    heat.draw(graphics, 47, 39);
                }
                case CUTTING_BOARD -> board.draw(graphics, 29, 0);
                case CRAFTING_TABLE -> {
                    for (int cell = 0; cell < 9; cell++) {
                        slot.draw(graphics, 29 + cell % 3 * 18, cell / 3 * 18);
                    }
                    arrow.draw(graphics, 90, 18);
                    slot.draw(graphics, 123, 18);
                }
            }
            graphics.pose().popPose();
            for (int row = 0; row < TABLE_KEYS.length; row++) {
                drawCell(graphics, label(row).getString(), 4, TABLE_TOP + row * ROW_HEIGHT,
                        LABEL_WIDTH - 8, 0x555555, false);
            }
            int displayed = slots.findSlotByName("experiment")
                    .flatMap(IRecipeSlotView::getDisplayedItemStack)
                    .map(TSDItems.Experiment250::getLevel).orElse(1);
            for (var level : recipe.rows()) {
                int x = LABEL_WIDTH + (level.level() - 1) * COLUMN_WIDTH;
                int color = level.level() == displayed ? 0x227733 : 0x555555;
                for (int row = 0; row < TABLE_KEYS.length; row++) {
                    drawCell(graphics, value(level, row), x + 2, TABLE_TOP + row * ROW_HEIGHT,
                            COLUMN_WIDTH - 4, color, true);
                }
            }
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, TSDJeiRecipes.Differentiation recipe,
                               IRecipeSlotsView slots, double mouseX, double mouseY) {
            int row = (int) ((mouseY - TABLE_TOP) / ROW_HEIGHT);
            if (mouseY < TABLE_TOP || row >= TABLE_KEYS.length || mouseX < 4 || mouseX >= WIDTH - 4) {
                return;
            }
            if (mouseX < LABEL_WIDTH) {
                tooltip.add(label(row));
                return;
            }
            int column = (int) ((mouseX - LABEL_WIDTH) / COLUMN_WIDTH);
            if (column >= recipe.rows().size()) {
                return;
            }
            var level = recipe.rows().get(column);
            tooltip.add(Component.translatable(
                    "twilightsparksdelightfabric.jei.experiment_250.level", level.level()));
            tooltip.add(label(row).append(": ").append(value(level, row)));
        }

        private static net.minecraft.network.chat.MutableComponent label(int row) {
            return Component.translatable(
                    "twilightsparksdelightfabric.jei.experiment_250.table." + TABLE_KEYS[row]);
        }

        private static String value(TSDJeiRecipes.Row level, int row) {
            return switch (row) {
                case 0 -> Integer.toString(level.level());
                case 1 -> Integer.toString(level.output().getCount());
                case 2 -> NUMBER.format(level.cost());
                default -> NUMBER.format(level.batchCost());
            };
        }

        private static void drawCell(GuiGraphics graphics, String text, int x, int y,
                                     int width, int color, boolean centered) {
            var font = Minecraft.getInstance().font;
            if (font.width(text) > width) {
                text = font.plainSubstrByWidth(text, width - font.width("...")) + "...";
            }
            graphics.drawString(font, text, centered ? x + (width - font.width(text)) / 2 : x, y,
                    color, false);
        }
    }

    // ------------------------------------------------------------------
    // Pickling
    // ------------------------------------------------------------------

    public static final class PicklingCategory extends Category<TSDJeiRecipes.Pickling> {
        private final IDrawable background;
        private final IDrawable catalystSlot;

        public PicklingCategory(IGuiHelper helper) {
            super(helper, TSDJeiPlugin.PICKLING,
                    "twilightsparksdelightfabric.jei.pickling.title",
                    TSDRegistry.Blocks.PICKLED_BRACKEN_JAR.asItem().getDefaultInstance(), 118, 80);
            var texture = ResourceLocation.fromNamespaceAndPath("twilightsparksdelightfabric",
                    "textures/gui/jei/pickling.png");
            background = helper.createDrawable(texture, 0, 0, 118, 80);
            catalystSlot = helper.createDrawable(texture, 119, 0, 18, 18);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder layout, TSDJeiRecipes.Pickling recipe,
                              IFocusGroup focuses) {
            layout.addInputSlot(9, 26).addItemStack(recipe.unripe());
            layout.addOutputSlot(93, 26).addItemStack(recipe.finished());
            layout.addSlot(RecipeIngredientRole.CATALYST, 64, 54)
                    .setBackground(catalystSlot, -1, -1)
                    .addItemStacks(recipe.catalysts())
                    .setSlotName("catalyst")
                    .addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(stack -> {
                        if (stack.is(TSDRegistry.Blocks.UNRIPE_PICKLED_BRACKEN_JAR.asItem())
                                || stack.is(TSDRegistry.Blocks.PICKLED_BRACKEN_JAR.asItem())) {
                            tooltip.add(Component.translatable(
                                    "twilightsparksdelightfabric.jei.pickling.clean_area"));
                        } else if (stack.is(twilightforest.init.TFItems.PEACOCK_FEATHER_FAN.get())) {
                            tooltip.add(Component.translatable(
                                    "twilightsparksdelightfabric.jei.pickling.peacock_fan"));
                        }
                    }));
        }

        @Override
        public void draw(TSDJeiRecipes.Pickling recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                         double mouseX, double mouseY) {
            background.draw(graphics);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, TSDJeiRecipes.Pickling recipe,
                               IRecipeSlotsView slots, double mouseX, double mouseY) {
            if (mouseY < 38 || mouseY >= 49) {
                return;
            }
            if (mouseX >= 40 && mouseX < 51) {
                tooltip.add(Component.translatable("twilightsparksdelightfabric.jei.pickling.shade"));
            } else if (mouseX >= 53 && mouseX < 64) {
                for (String key : List.of("structures", "structure_hills", "structure_mushroom_tower",
                        "structure_labyrinth", "structure_knight_stronghold", "structure_quest_grove")) {
                    tooltip.add(Component.translatable(
                            "twilightsparksdelightfabric.jei.pickling." + key));
                }
            } else if (mouseX >= 67 && mouseX < 78) {
                tooltip.add(Component.translatable(
                        "twilightsparksdelightfabric.jei.pickling.accelerators"));
            }
        }
    }

    // ------------------------------------------------------------------
    // Hunting
    // ------------------------------------------------------------------

    public static final class HuntingCategory extends Category<TSDJeiRecipes.Drop> {
        private final IDrawable arrow;
        private final Map<TSDJeiRecipes.Drop, LivingEntity> previews = new HashMap<>();
        private net.minecraft.client.multiplayer.ClientLevel previewLevel;

        public HuntingCategory(IGuiHelper helper) {
            super(helper, TSDJeiPlugin.HUNTING,
                    "twilightsparksdelightfabric.jei.hunting.title",
                    new ItemStack(vectorwing.farmersdelight.common.registry.ModItems.IRON_KNIFE.get()),
                    162, 76);
            arrow = helper.createDrawable(ResourceLocation.fromNamespaceAndPath("farmersdelight",
                    "textures/gui/jei/cutting_board.png"), 47, 20, 24, 18);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder layout, TSDJeiRecipes.Drop recipe, IFocusGroup focuses) {
            layout.addOutputSlot(112, 23).setStandardSlotBackground().addItemStack(recipe.output())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                            recipe.knifeOnly() ? "twilightsparksdelightfabric.jei.hunting_tool_extra"
                                    : "twilightsparksdelightfabric.jei.any_kill")));
            if (recipe.extraLegSlot()) {
                layout.addOutputSlot(135, 23).setStandardSlotBackground().addItemStack(recipe.output())
                        .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                                "twilightsparksdelightfabric.jei.hunting_tool_extra")));
            }
            if (recipe.knifeOnly() || recipe.extraLegSlot()) {
                layout.addInputSlot(74, 43).setSlotName("knife")
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, new SwingingKnifeRenderer())
                        .addItemStacks(knives());
            }
        }

        @Override
        public void draw(TSDJeiRecipes.Drop recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                         double mouseX, double mouseY) {
            arrow.draw(graphics, 77, 24);
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != previewLevel) {
                previews.clear();
                previewLevel = minecraft.level;
            }
            if (previewLevel != null) {
                LivingEntity entity = previews.computeIfAbsent(recipe, this::createPreview);
                if (entity != null) {
                    entity.setRemainingFireTicks(recipe.burning() ? 200 : 0);
                    int scale = (int) Math.min(36 / Math.max(0.5, entity.getBbHeight()),
                            46 / Math.max(0.5, entity.getBbWidth()));
                    renderPreview(graphics, entity, scale);
                }
            }
            if (recipe.burning()) {
                centeredText(graphics, Component.translatable(
                        "twilightsparksdelightfabric.jei.hunting.burning"), 64, 0x555555);
            }
        }

        private static void renderPreview(GuiGraphics graphics, LivingEntity entity, int scale) {
            var matrix = graphics.pose().last().pose();
            var min = matrix.transformPosition(4, 0, 0, new org.joml.Vector3f());
            var max = matrix.transformPosition(66, 59, 0, new org.joml.Vector3f());
            graphics.enableScissor((int) min.x, (int) min.y, (int) max.x, (int) max.y);
            float oldBody = entity.yBodyRot;
            float oldYaw = entity.getYRot();
            float oldHead = entity.yHeadRot;
            float oldHeadPrevious = entity.yHeadRotO;
            try {
                entity.yBodyRot = 150;
                entity.setYRot(150);
                entity.yHeadRot = 150;
                entity.yHeadRotO = 150;
                var tilt = new org.joml.Quaternionf().rotationX(0.12F);
                var pose = new org.joml.Quaternionf().rotationZ((float) Math.PI).mul(tilt);
                InventoryScreen.renderEntityInInventory(graphics, 35, 30, scale / entity.getScale(),
                        new org.joml.Vector3f(0, entity.getBbHeight() / 2 + 0.06F * entity.getScale(), 0),
                        pose, tilt, entity);
            } finally {
                entity.yBodyRot = oldBody;
                entity.setYRot(oldYaw);
                entity.yHeadRot = oldHead;
                entity.yHeadRotO = oldHeadPrevious;
                graphics.disableScissor();
            }
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, TSDJeiRecipes.Drop recipe, IRecipeSlotsView slots,
                               double mouseX, double mouseY) {
            if (mouseX < 4 || mouseX >= 66 || mouseY < 0 || mouseY >= 59) {
                return;
            }
            LivingEntity entity = previews.get(recipe);
            if (entity == null) {
                return;
            }
            tooltip.add(recipe.transformedRabbit()
                    ? Component.translatable("entity.minecraft.killer_bunny") : entity.getName());
            if (recipe.transformedRabbit()) {
                tooltip.add(Component.translatable(
                        "twilightsparksdelightfabric.jei.transformation_powder_chance"));
            }
        }

        private LivingEntity createPreview(TSDJeiRecipes.Drop recipe) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(recipe.entity()).orElse(null);
            if (type == null || !(type.create(previewLevel) instanceof LivingEntity entity)) {
                return null;
            }
            if (entity instanceof Rabbit rabbit && recipe.transformedRabbit()) {
                rabbit.setVariant(Rabbit.Variant.EVIL);
                rabbit.setCustomName(null);
            }
            return entity;
        }
    }

    /** Harvesting is a world action, not a cutting board recipe. */
    public static final class HarvestCategory extends Category<TSDJeiRecipes.Harvest> {
        private final IDrawable arrow;

        public HarvestCategory(IGuiHelper helper) {
            super(helper, TSDJeiPlugin.HARVESTING,
                    "twilightsparksdelightfabric.jei.harvesting.title",
                    new ItemStack(TSDRegistry.Items.BRACKEN), 140, 62);
            arrow = helper.createDrawable(ResourceLocation.fromNamespaceAndPath("farmersdelight",
                    "textures/gui/jei/cutting_board.png"), 47, 20, 24, 18);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder layout, TSDJeiRecipes.Harvest recipe, IFocusGroup focuses) {
            layout.addInputSlot(2, 2).setSlotName("plant").addItemStack(recipe.plant());
            layout.addInputSlot(59, 37).setSlotName("knife")
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, new SwingingKnifeRenderer())
                    .addItemStacks(knives());
            layout.addOutputSlot(108, 21).setStandardSlotBackground().addItemStack(recipe.result())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                            "twilightsparksdelightfabric.jei.extra_by_looting")));
        }

        @Override
        public void draw(TSDJeiRecipes.Harvest recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                         double mouseX, double mouseY) {
            arrow.draw(graphics, 59, 20);
        }
    }

    // ------------------------------------------------------------------
    // Glory crucible
    // ------------------------------------------------------------------

    public static final class CrucibleCategory extends Category<TSDJeiRecipes.Crucible> {
        private final boolean brewing;

        public CrucibleCategory(IGuiHelper helper, boolean brewing) {
            super(helper, brewing ? TSDJeiPlugin.CRUCIBLE_BREWING : TSDJeiPlugin.CRUCIBLE_HEATING,
                    brewing ? "twilightsparksdelightfabric.jei.glory_crucible.brewing.title"
                            : "twilightsparksdelightfabric.jei.glory_crucible.heating.title",
                    TSDRegistry.Blocks.GLORY_CRUCIBLE.asItem().getDefaultInstance(), 125, 48);
            this.brewing = brewing;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder layout, TSDJeiRecipes.Crucible recipe, IFocusGroup focuses) {
            if (brewing) {
                layout.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(recipe.reagents());
                layout.addSlot(RecipeIngredientRole.INPUT, 50, 1).setStandardSlotBackground()
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, new PotionFluidRenderer())
                        .setSlotName("potion_input").addItemStack(recipe.inputs().getFirst());
                layout.addOutputSlot(108, 1).setStandardSlotBackground()
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, new PotionFluidRenderer())
                        .setSlotName("potion_output").addItemStack(recipe.outputs().getFirst());
            } else {
                layout.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(recipe.inputs());
                layout.addOutputSlot(108, 1).setStandardSlotBackground().addItemStacks(recipe.outputs());
                layout.addInputSlot(50, 1).setStandardSlotBackground().addItemStacks(recipe.fluids());
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, TSDJeiRecipes.Crucible recipe,
                                       IFocusGroup focuses) {
            builder.addRecipePlusSign(27, 3);
            builder.addRecipeArrow(76, 1);
        }

        @Override
        public void draw(TSDJeiRecipes.Crucible recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                         double mouseX, double mouseY) {
            if (brewing) {
                centeredText(graphics, Component.translatable(
                        "twilightsparksdelightfabric.jei.glory_crucible.heat_source_required"), 27, 0xAA0000);
            } else {
                centeredText(graphics, Component.translatable(
                        "twilightsparksdelightfabric.jei.glory_crucible.fuel_not_consumed"), 27, 0xAA0000);
            }
        }
    }

    // ------------------------------------------------------------------
    // Renderers
    // ------------------------------------------------------------------

    /** Every knife of the game, animated so both JEI and its bridges keep the motion. */
    public static final class SwingingKnifeRenderer implements IIngredientRenderer<ItemStack> {
        @Override
        public void render(GuiGraphics graphics, ItemStack stack) {
            float phase = (Util.getMillis() % 1400L) / 1400F;
            float angle = -20 + 65 * (float) Math.sin(phase * Math.PI * 2);
            graphics.pose().pushPose();
            graphics.pose().translate(12, 12, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            graphics.renderItem(stack, -8, -8);
            graphics.pose().popPose();
        }

        @Override
        public int getWidth() {
            return 24;
        }

        @Override
        public int getHeight() {
            return 24;
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, ItemStack stack, TooltipFlag flag) {
            var minecraft = Minecraft.getInstance();
            tooltip.addAll(stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, flag));
        }

        /**
         * The abstract method of this JEI version is deprecated for removal but
         * still has to be implemented; the modern overload above is used by JEI
         * whenever it is available.
         */
        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(ItemStack stack, TooltipFlag flag) {
            var minecraft = Minecraft.getInstance();
            return stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, flag);
        }
    }

    /** Keeps the liquid look while showing the real potion tooltip. */
    public static final class PotionFluidRenderer implements IIngredientRenderer<ItemStack> {
        @Override
        public void render(GuiGraphics graphics, ItemStack potion) {
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.withDefaultNamespace("block/water_still"));
            int color = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            graphics.blit(0, 0, 0, 16, 16, sprite,
                    (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, ItemStack potion, TooltipFlag flag) {
            var minecraft = Minecraft.getInstance();
            tooltip.addAll(potion.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, flag));
        }

        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(ItemStack potion, TooltipFlag flag) {
            var minecraft = Minecraft.getInstance();
            return potion.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, flag);
        }
    }

    /** All registered knives, identical to the runtime knife test. */
    public static List<ItemStack> knives() {
        List<ItemStack> knives = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            ItemStack stack = item.getDefaultInstance();
            if (TSDUtil.isKnife(stack)) {
                knives.add(stack);
            }
        });
        return knives;
    }

}

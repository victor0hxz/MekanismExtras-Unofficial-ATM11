package com.jerry.mekextras.common.tile.factory;

import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.common.Mekanism;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.lookup.cache.type.ItemInputCache;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import fr.iglee42.evolvedmekanism.interfaces.EMInputRecipeCache.IFindRecipes;
import fr.iglee42.evolvedmekanism.interfaces.EMInputRecipeCache.TripleItem;
import fr.iglee42.evolvedmekanism.interfaces.IGetEnergySlot;
import com.jerry.mekextras.common.integration.evolved.recipe.ExtraAlloyerCachedRecipe;
import fr.iglee42.evolvedmekanism.interfaces.TripleItemRecipeLookupHandler;
import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import fr.iglee42.evolvedmekanism.registries.EMRecipeType;
import mekanism.common.inventory.slot.InputInventorySlot;
import com.jerry.mekextras.common.inventory.slot.StackableInputInventorySlot;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import com.jerry.mekextras.common.upgrade.ExtraAlloyerUpgradeData;
import fr.iglee42.evolvedmekanism.tiles.upgrade.AlloyerUpgradeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class TileEntityExtraAlloyingFactory extends TileEntityExtraItemToItemFactory<AlloyerRecipe> {

    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT);

    InputInventorySlot extraSlot;
    InputInventorySlot secondExtraSlot;

    private final IInputHandler<net.minecraft.world.item.Item, @NotNull ItemStack> extraInputHandler;
    private final IInputHandler<net.minecraft.world.item.Item, @NotNull ItemStack> secondExtraInputHandler;

    public TileEntityExtraAlloyingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);

        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, List.of(extraSlot, secondExtraSlot)));
        }

        extraInputHandler = InputHelper.getInputHandler(extraSlot, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
        secondExtraInputHandler = InputHelper.getInputHandler(secondExtraSlot, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<AlloyerRecipe> cached, @NotNull ItemResource stack) {
        if (cached != null) {
            AlloyerRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getMainInput().testType(stack) && (extraSlot.isEmpty() || cachedRecipe.getExtraInput().testType(extraSlot.resource()) && (secondExtraSlot.isEmpty() || cachedRecipe.getTertiaryExtraInput().testType(secondExtraSlot.resource())));
        }
        return false;
    }

    @Override
    protected @Nullable AlloyerRecipe findRecipe(@NotNull ItemResource fallbackInput, @NotNull IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
        ItemStack extra = extraSlot.resource().toStack(extraSlot.amountAsInt());
        ItemStack secondExtra = secondExtraSlot.resource().toStack(secondExtraSlot.amountAsInt());
        ItemStack output = outputSlot.resource().toStack(outputSlot.amountAsInt());
        return getRecipeType().findFirst(level, recipe -> recipe.getMainInput().testType(fallbackInput)
                && (extra.isEmpty() || recipe.getExtraInput().testType(ItemResource.of(extra)))
                && (secondExtra.isEmpty() || recipe.getTertiaryExtraInput().testType(ItemResource.of(secondExtra)))
                && (output.isEmpty() || ItemStack.isSameItemSameComponents(recipe.getOutput(fallbackInput.toStack(), extra, secondExtra), output)));
    }

    @Override
    protected int getNeededInput(AlloyerRecipe recipe, ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getMainInput().getNeededAmount(inputStack));
    }

    @Override
    public boolean isItemValidForSlot(@NotNull ItemResource stack) {
        return getRecipeType().contains(level, recipe -> recipe.getMainInput().testType(stack));
    }

    @Override
    public boolean isValidInputItem(@NotNull ItemResource stack) {
        return getRecipeType().contains(level, recipe -> recipe.getMainInput().testType(stack));
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<?, AlloyerRecipe, TripleItem<AlloyerRecipe>> getRecipeType() {
        return EMRecipeType.ALLOYING;
    }

    @Override
    public @Nullable AlloyerRecipe getRecipe(int cacheIndex) {
        return getRecipeType().findFirst(level, recipe -> recipe.test(inputHandlers[cacheIndex].getInput(), extraInputHandler.getInput(), secondExtraInputHandler.getInput()));
    }

    @Override
    public @NotNull CachedRecipe<AlloyerRecipe> createNewCachedRecipe(@NotNull AlloyerRecipe recipe, int cacheIndex) {
        return ExtraAlloyerCachedRecipe.alloyer(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], extraInputHandler, secondExtraInputHandler, outputHandlers[cacheIndex])
                .setErrorsChanged(errors -> errorTracker.onErrorsChanged(errors, cacheIndex))
                .setCanHolderFunction(this::canFunction)
                .setActive(active -> setActiveState(active, cacheIndex))
                .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
                .setRequiredTicks(this::getTicksRequired)
                .setOnFinish(this::markForSave)
                .setBaselineMaxOperations(this::getOperationsPerTick)
                .setOperatingTicksChanged(operatingTicks -> progress[cacheIndex] = operatingTicks);
    }

    @Override
    public void parseUpgradeData(IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        super.parseUpgradeData(upgradeData, provider, transaction);
        if (upgradeData instanceof ExtraAlloyerUpgradeData data) {
            extraSlot.copyContents(data.extraSlot, transaction);
            secondExtraSlot.copyContents(data.secondExtraSlot, transaction);
        }
    }

    @Override
    public ExtraAlloyerUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new ExtraAlloyerUpgradeData(provider, redstone, getControlType(), energyContainer, progress,
                energySlot, extraSlot, secondExtraSlot, inputSlots, outputSlots, isSorting(), getComponents(), problemPath());
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener sortingListener) {
        super.addSlots(builder, listener, sortingListener);
        builder.addContainer(extraSlot = StackableInputInventorySlot.at(tier,
                stack -> getRecipeType().contains(level, recipe -> recipe.getExtraInput().testType(ItemResource.of(stack))), markAllMonitorsChanged(listener), 7, 57));
        builder.addContainer(secondExtraSlot = StackableInputInventorySlot.at(tier,
                stack -> getRecipeType().contains(level, recipe -> recipe.getTertiaryExtraInput().testType(ItemResource.of(stack))), markAllMonitorsChanged(listener), 7, 37));
    }

    @Override
    protected InputInventorySlot getExtraSlot() { return extraSlot; }
}

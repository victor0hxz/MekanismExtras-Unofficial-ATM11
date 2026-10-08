package com.jerry.mekextras.common.tile.factory;

import java.util.List;
import java.util.Set;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.TwoInputCachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.DoubleItemRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.DoubleItem;
import mekanism.common.upgrade.CombinerUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraCombiningFactory extends TileEntityExtraItemToItemFactory<CombinerRecipe> implements DoubleItemRecipeLookupHandler<CombinerRecipe> {
   private static final CheckRecipeType<Item, ItemResource, Item, ItemResource, CombinerRecipe, ItemResource> CAN_OUTPUT_STACK = (recipe, input, extra, outputContents) -> outputContents.isEmpty()
      || outputContents.matches((ItemStackTemplate)recipe.getOutput(input, extra));
   private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
      RecipeError.NOT_ENOUGH_ENERGY,
      RecipeError.NOT_ENOUGH_INPUT,
      RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
      RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
      RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
   );
   private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(RecipeError.NOT_ENOUGH_ENERGY, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
   private final IInputHandler<Item, ItemStack> extraInputHandler;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getSecondaryInput", docPlaceholder = "secondary input slot")
   @UnknownNullability InputInventorySlot extraSlot;

   public TileEntityExtraCombiningFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
      this.extraInputHandler = InputHelper.getInputHandler(this.extraSlot, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
   }

   @Override
   protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
      super.addSlots(builder, listener, updateSortingListener);
      builder.addContainer(this.extraSlot = InputInventorySlot.at(this::containsRecipeB, this.markAllMonitorsChanged(listener), 7, 57));
      this.extraSlot.setSlotType(ContainerSlotType.EXTRA);
   }

   protected InputInventorySlot getExtraSlot() {
      return this.extraSlot;
   }

   @Override
   public boolean isItemValidForSlot(ItemResource itemType) {
      return this.containsRecipeAB(itemType, (TypedInstance)this.extraSlot.resource());
   }

   @Override
   public boolean isValidInputItem(ItemResource itemType) {
      return this.containsRecipeA(itemType);
   }

   protected int getNeededInput(CombinerRecipe recipe, ItemResource inputType) {
      return recipe.getMainInput().getNeededAmount(inputType);
   }

   @Override
   protected boolean isCachedRecipeValid(@Nullable CachedRecipe<CombinerRecipe> cached, ItemResource itemType) {
      if (cached == null) {
         return false;
      } else {
         CombinerRecipe cachedRecipe = (CombinerRecipe)cached.getRecipe();
         return cachedRecipe.getMainInput().testType(itemType)
            && (this.extraSlot.isEmpty() || cachedRecipe.getExtraInput().testType((TypedInstance)this.extraSlot.resource()));
      }
   }

   protected @Nullable CombinerRecipe findRecipe(ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
      return (CombinerRecipe)((DoubleItem)this.getRecipeType().getInputCache())
         .findTypeBasedRecipe(this.level, fallbackInput, (ItemResource)this.extraSlot.resource(), (ItemResource)outputSlot.resource(), CAN_OUTPUT_STACK);
   }

   public IMekanismRecipeTypeProvider<RecipeInput, CombinerRecipe, DoubleItem<CombinerRecipe>> getRecipeType() {
      return MekanismRecipeType.COMBINING;
   }

   public IRecipeViewerRecipeType<CombinerRecipe> recipeViewerType() {
      return RecipeViewerRecipeType.COMBINING;
   }

   public @Nullable CombinerRecipe getRecipe(int cacheIndex) {
      return (CombinerRecipe)this.findFirstRecipe(this.inputHandlers[cacheIndex], this.extraInputHandler);
   }

   public CachedRecipe<CombinerRecipe> createNewCachedRecipe(CombinerRecipe recipe, int cacheIndex) {
      return new TwoInputCachedRecipe<>(
            recipe, this.recheckAllRecipeErrors[cacheIndex], this.inputHandlers[cacheIndex], this.extraInputHandler, this.outputHandlers[cacheIndex]
         )
         .setErrorsChanged(errors -> this.errorTracker.onErrorsChanged(errors, cacheIndex))
         .setCanHolderFunction(this::canFunction)
         .setActive(active -> this.setActiveState(active, cacheIndex))
         .setEnergyRequirements(this.energyContainer::getEnergyPerTick, this.energyContainer)
         .setRequiredTicks(this::getTicksRequired)
         .setOnFinish(this::markForSave)
         .setOperatingTicksChanged(operatingTicks -> this.progress[cacheIndex] = operatingTicks)
         .setBaselineMaxOperations(this::getOperationsPerTick);
   }

   @Override
   public void parseUpgradeData(IUpgradeData upgradeData, Provider provider, TransactionContext transaction) {
      if (upgradeData instanceof CombinerUpgradeData data) {
         super.parseUpgradeData(upgradeData, provider, transaction);
         this.extraSlot.copyContents(data.extraSlot, transaction);
      } else {
         Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
      }
   }

   public CombinerUpgradeData getUpgradeData(Provider provider) {
      return new CombinerUpgradeData(
         provider,
         this.redstone,
         this.getControlType(),
         this.energyContainer,
         this.progress,
         this.energySlot,
         this.extraSlot,
         this.inputSlots,
         this.outputSlots,
         this.isSorting(),
         this.getComponents(),
         this.problemPath()
      );
   }
}

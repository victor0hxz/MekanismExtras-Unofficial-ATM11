package com.jerry.mekextras.common.tile.factory;

import java.util.List;
import java.util.Set;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.OneInputCachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryInputInventorySlot;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryOutputInventorySlot;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.ISingleRecipeLookupHandler.ItemRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.recipe.lookup.cache.SingleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.SawmillUpgradeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraSawingFactory extends TileEntityExtraFactory<SawmillRecipe> implements ItemRecipeLookupHandler<SawmillRecipe> {
   private static final CheckRecipeType<Item, ItemResource, SawmillRecipe, ItemResource, ItemResource> CAN_OUTPUTS_STACK = (recipe, input, outputContents, secondaryOutputContents) -> {
      ChanceOutput chanceOutput = (ChanceOutput)recipe.getOutput(input);
      if (!outputContents.isEmpty() && !outputContents.matches(chanceOutput.getMainOutput())) {
         return false;
      } else if (secondaryOutputContents.isEmpty()) {
         return true;
      } else {
         ItemStackTemplate secondaryOutput = chanceOutput.getMaxSecondaryOutput();
         return secondaryOutput == null || secondaryOutputContents.matches(secondaryOutput);
      }
   };
   private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
      RecipeError.NOT_ENOUGH_ENERGY,
      RecipeError.NOT_ENOUGH_INPUT,
      RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
      TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR,
      RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
   );
   private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(RecipeError.NOT_ENOUGH_ENERGY);
   protected final IInputHandler<Item, ItemStack>[] inputHandlers;
   protected final IOutputHandler<ChanceOutput>[] outputHandlers;

   public TileEntityExtraSawingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      ExtraFactoryTier tier = (ExtraFactoryTier)com.jerry.mekextras.common.block.attribute.ExtraAttribute.getAdvancedTier(blockProvider, ExtraFactoryTier.class);
      this.inputHandlers = new IInputHandler[tier.processes];
      this.outputHandlers = new IOutputHandler[tier.processes];
      super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
   }

   @Override
   protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
      int baseX = 27;
      int baseXMult = 19;

      for (int i = 0; i < this.tier.processes; i++) {
         int xPos = baseX + i * baseXMult;
         FactoryRecipeCacheLookupMonitor<SawmillRecipe> lookupMonitor = this.recipeCacheLookupMonitors[i];
         IContentsListener updateSortingAndUnpause = () -> {
            updateSortingListener.onContentsChanged();
            lookupMonitor.unpause();
         };
         ExtraFactoryOutputInventorySlot outputSlot = ExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 57);
         ExtraFactoryOutputInventorySlot secondaryOutputSlot = ExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 77);
         ExtraFactoryInputInventorySlot inputSlot = ExtraFactoryInputInventorySlot.create(this, i, outputSlot, secondaryOutputSlot, lookupMonitor, xPos, 13);
         int index = i;
         ((ExtraFactoryInputInventorySlot)builder.addContainer(inputSlot))
            .tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, this.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT, index)));
         ((ExtraFactoryOutputInventorySlot)builder.addContainer(outputSlot))
            .tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, this.getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE, index)));
         ((ExtraFactoryOutputInventorySlot)builder.addContainer(secondaryOutputSlot))
            .tracksWarnings(
               slot -> slot.warning(
                  WarningType.NO_SPACE_IN_OUTPUT, this.getWarningCheck(TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR, index)
               )
            );
         this.inputHandlers[i] = InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT);
         this.outputHandlers[i] = OutputHelper.getOutputHandler(
            outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE, secondaryOutputSlot, TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR
         );
         this.processInfoSlots[i] = new TileEntityExtraFactory.ProcessInfo(i, inputSlot, outputSlot, secondaryOutputSlot);
      }
   }

   @Override
   public boolean isItemValidForSlot(ItemResource itemType) {
      return true;
   }

   @Override
   public boolean isValidInputItem(ItemResource itemType) {
      return this.containsRecipe(itemType);
   }

   protected int getNeededInput(SawmillRecipe recipe, ItemResource inputType) {
      return ((ItemStackIngredient)recipe.getInput()).getNeededAmount(inputType);
   }

   @Override
   protected boolean isCachedRecipeValid(@Nullable CachedRecipe<SawmillRecipe> cached, ItemResource itemType) {
      return cached != null && ((ItemStackIngredient)((SawmillRecipe)cached.getRecipe()).getInput()).testType(itemType);
   }

   protected @Nullable SawmillRecipe findRecipe(ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
      ItemResource extra = secondaryOutputSlot == null ? ItemResource.EMPTY : (ItemResource)secondaryOutputSlot.resource();
      return (SawmillRecipe)((SingleItem)this.getRecipeType().getInputCache())
         .findTypeBasedRecipe(this.level, fallbackInput, (ItemResource)outputSlot.resource(), extra, CAN_OUTPUTS_STACK);
   }

   public IMekanismRecipeTypeProvider<SingleRecipeInput, SawmillRecipe, SingleItem<SawmillRecipe>> getRecipeType() {
      return MekanismRecipeType.SAWING;
   }

   public IRecipeViewerRecipeType<SawmillRecipe> recipeViewerType() {
      return RecipeViewerRecipeType.SAWING;
   }

   public @Nullable SawmillRecipe getRecipe(int cacheIndex) {
      return (SawmillRecipe)this.findFirstRecipe(this.inputHandlers[cacheIndex]);
   }

   public CachedRecipe<SawmillRecipe> createNewCachedRecipe(SawmillRecipe recipe, int cacheIndex) {
      return new OneInputCachedRecipe<>(recipe, this.recheckAllRecipeErrors[cacheIndex], this.inputHandlers[cacheIndex], this.outputHandlers[cacheIndex])
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
      if (upgradeData instanceof SawmillUpgradeData) {
         super.parseUpgradeData(upgradeData, provider, transaction);
      } else {
         Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
      }
   }

   public SawmillUpgradeData getUpgradeData(Provider provider) {
      return new SawmillUpgradeData(
         provider,
         this.redstone,
         this.getControlType(),
         this.energyContainer,
         this.progress,
         this.energySlot,
         this.inputSlots,
         this.outputSlots,
         this.isSorting(),
         this.getComponents(),
         this.problemPath()
      );
   }

   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getSecondaryOutput", docPlaceholder = "secondary output slot")
   IInventorySlot getSecondaryOutputSlot(int process) throws ComputerException {
      this.validateValidProcess(process);
      IInventorySlot secondaryOutputSlot = this.processInfoSlots[process].secondaryOutputSlot();
      if (secondaryOutputSlot == null) {
         throw new ComputerException("Process: '%d' has a null secondary output slot, this should not be possible for sawing factories", new Object[]{process});
      } else {
         return secondaryOutputSlot;
      }
   }
}

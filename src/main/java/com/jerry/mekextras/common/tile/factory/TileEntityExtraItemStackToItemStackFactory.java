package com.jerry.mekextras.common.tile.factory;

import java.util.List;
import java.util.Set;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.OneInputCachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.ISingleRecipeLookupHandler.ItemRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.upgrade.MachineUpgradeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriPredicate;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraItemStackToItemStackFactory
   extends TileEntityExtraItemToItemFactory<ItemStackToItemStackRecipe>
   implements ItemRecipeLookupHandler<ItemStackToItemStackRecipe> {
   private static final TriPredicate<ItemStackToItemStackRecipe, ItemResource, ItemResource> CAN_OUTPUT_STACK = (recipe, input, outputContents) -> outputContents.isEmpty()
      || outputContents.matches((ItemStackTemplate)recipe.getOutput(input));
   private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
      RecipeError.NOT_ENOUGH_ENERGY, RecipeError.NOT_ENOUGH_INPUT, RecipeError.NOT_ENOUGH_OUTPUT_SPACE, RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
   );
   private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(RecipeError.NOT_ENOUGH_ENERGY);

   public TileEntityExtraItemStackToItemStackFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
   }

   @Override
   public boolean isItemValidForSlot(ItemResource itemType) {
      return true;
   }

   @Override
   public boolean isValidInputItem(ItemResource itemType) {
      return this.containsRecipe(itemType);
   }

   protected int getNeededInput(ItemStackToItemStackRecipe recipe, ItemResource inputType) {
      return ((ItemStackIngredient)recipe.getInput()).getNeededAmount(inputType);
   }

   @Override
   protected boolean isCachedRecipeValid(@Nullable CachedRecipe<ItemStackToItemStackRecipe> cached, ItemResource itemType) {
      return cached != null && ((ItemStackIngredient)((ItemStackToItemStackRecipe)cached.getRecipe()).getInput()).testType(itemType);
   }

   protected @Nullable ItemStackToItemStackRecipe findRecipe(
      ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot
   ) {
      return (ItemStackToItemStackRecipe)((SingleItem)this.getRecipeType().getInputCache())
         .findTypeBasedRecipe(this.level, fallbackInput, (ItemResource)outputSlot.resource(), CAN_OUTPUT_STACK);
   }

   public IMekanismRecipeTypeProvider<SingleRecipeInput, ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> getRecipeType() {
      return switch (this.type) {
         case ENRICHING -> MekanismRecipeType.ENRICHING;
         case CRUSHING -> MekanismRecipeType.CRUSHING;
         default -> MekanismRecipeType.SMELTING;
      };
   }

   public IRecipeViewerRecipeType<ItemStackToItemStackRecipe> recipeViewerType() {
      return switch (this.type) {
         case ENRICHING -> RecipeViewerRecipeType.ENRICHING;
         case CRUSHING -> RecipeViewerRecipeType.CRUSHING;
         default -> RecipeViewerRecipeType.SMELTING;
      };
   }

   public @Nullable ItemStackToItemStackRecipe getRecipe(int cacheIndex) {
      return (ItemStackToItemStackRecipe)this.findFirstRecipe(this.inputHandlers[cacheIndex]);
   }

   public CachedRecipe<ItemStackToItemStackRecipe> createNewCachedRecipe(ItemStackToItemStackRecipe recipe, int cacheIndex) {
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

   public MachineUpgradeData getUpgradeData(Provider provider) {
      return new MachineUpgradeData(
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
}

package com.jerry.mekextras.common.tile.factory;

import java.util.List;
import java.util.Set;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryInputInventorySlot;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryOutputInventorySlot;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TileEntityExtraItemToItemFactory<RECIPE extends MekanismRecipe<?>> extends TileEntityExtraFactory<RECIPE> {
   protected final IInputHandler<Item, ItemStack>[] inputHandlers;
   protected final IOutputHandler<ItemStackTemplate>[] outputHandlers;

   protected TileEntityExtraItemToItemFactory(
      Holder<Block> blockProvider, BlockPos pos, BlockState state, List<RecipeError> errorTypes, Set<RecipeError> globalErrorTypes
   ) {
      ExtraFactoryTier tier = (ExtraFactoryTier)com.jerry.mekextras.common.block.attribute.ExtraAttribute.getAdvancedTier(blockProvider, ExtraFactoryTier.class);
      this.inputHandlers = new IInputHandler[tier.processes];
      this.outputHandlers = new IOutputHandler[tier.processes];
      super(blockProvider, pos, state, errorTypes, globalErrorTypes);
   }

   @Override
   protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
      int baseX = 27;
      int baseXMult = 19;

      for (int i = 0; i < this.tier.processes; i++) {
         int xPos = baseX + i * baseXMult;
         FactoryRecipeCacheLookupMonitor<RECIPE> lookupMonitor = this.recipeCacheLookupMonitors[i];
         IContentsListener updateSortingAndUnpause = () -> {
            updateSortingListener.onContentsChanged();
            lookupMonitor.unpause();
         };
         ExtraFactoryOutputInventorySlot outputSlot = ExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 57);
         ExtraFactoryInputInventorySlot inputSlot = ExtraFactoryInputInventorySlot.create(this, i, outputSlot, this.recipeCacheLookupMonitors[i], xPos, 13);
         int index = i;
         ((ExtraFactoryInputInventorySlot)builder.addContainer(inputSlot))
            .tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, this.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT, index)));
         ((ExtraFactoryOutputInventorySlot)builder.addContainer(outputSlot))
            .tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, this.getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE, index)));
         this.inputHandlers[i] = InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT);
         this.outputHandlers[i] = OutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE);
         this.processInfoSlots[i] = new TileEntityExtraFactory.ProcessInfo(i, inputSlot, outputSlot, null);
      }
   }
}

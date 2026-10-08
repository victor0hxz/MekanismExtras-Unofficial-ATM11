package com.jerry.mekextras.common.tile.factory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToObjectCachedRecipe;
import mekanism.api.recipes.cache.TwoInputCachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToObjectCachedRecipe.ChemicalUsageMultiplier;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.IRecipeLookupHandler.ConstantUsageRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.upgrade.AdvancedMachineUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraItemStackChemicalToItemStackFactory
   extends TileEntityExtraItemToItemFactory<ItemStackChemicalToItemStackRecipe>
   implements IHasDumpButton,
   ItemChemicalRecipeLookupHandler<ItemStackChemicalToItemStackRecipe>,
   ConstantUsageRecipeLookupHandler {
   private static final CheckRecipeType<Item, ItemResource, Chemical, ChemicalResource, ItemStackChemicalToItemStackRecipe, ItemResource> CAN_OUTPUT_STACK = (recipe, input, extra, outputContents) -> outputContents.isEmpty()
      || outputContents.matches((ItemStackTemplate)recipe.getOutput(input, extra));
   private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
      RecipeError.NOT_ENOUGH_ENERGY,
      RecipeError.NOT_ENOUGH_INPUT,
      RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
      RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
      RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
   );
   private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(RecipeError.NOT_ENOUGH_ENERGY, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
   private final IInputHandler<Chemical, ChemicalStack> chemicalInputHandler;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getChemicalItem", docPlaceholder = "chemical item (extra) slot")
   @UnknownNullability ChemicalInventorySlot extraSlot;
   @WrappingComputerMethod(
      wrapper = ComputerChemicalTankWrapper.class,
      methodNames = {"getChemical", "getChemicalCapacity", "getChemicalNeeded", "getChemicalFilledPercentage"},
      docPlaceholder = "chemical tank"
   )
   @UnknownNullability IChemicalTank chemicalTank;
   private final ChemicalUsageMultiplier chemicalUsageMultiplier;
   private final int[] usedSoFar;
   private double chemicalPerTickMeanMultiplier = 1.0;
   private int baseTotalUsage;

   public TileEntityExtraItemStackChemicalToItemStackFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
      this.chemicalInputHandler = InputHelper.getConstantInputHandler(this.chemicalTank);
      if (this.allowExtractingChemical()) {
         this.configComponent.setupIOConfig(TransmissionType.CHEMICAL, this.chemicalTank).setCanEject(false);
      } else {
         this.configComponent.setupInputConfig(TransmissionType.CHEMICAL, this.chemicalTank);
      }

      this.baseTotalUsage = 200;
      this.usedSoFar = new int[this.tier.processes];
      if (this.useStatisticalMechanics()) {
         this.chemicalUsageMultiplier = (var1, var2) -> StatUtils.inversePoisson(this.chemicalPerTickMeanMultiplier);
      } else {
         this.chemicalUsageMultiplier = ChemicalUsageMultiplier.constantUse(() -> this.baseTotalUsage, this::getChemicalTicksRequired);
      }
   }

   public IContainerHolder<IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
      MekContainerHelper<IChemicalTank> builder = MekContainerHelper.forSideWithChemicalConfig(this);
      long capacity = ((AttributeFactoryType)Attribute.getOrThrow(this.getBlockHolder(), AttributeFactoryType.class)).getFactoryType() == FactoryType.INFUSING
         ? 1000L
         : 210L;
      if (this.allowExtractingChemical()) {
         this.chemicalTank = BasicChemicalTank.create(capacity * this.tier.processes * this.tier.processes, this::containsRecipeB, this.markAllMonitorsChanged(listener));
      } else {
         this.chemicalTank = BasicChemicalTank.input(capacity * this.tier.processes * this.tier.processes, this::containsRecipeB, this.markAllMonitorsChanged(listener));
      }

      builder.addContainer(this.chemicalTank);
      return builder.build();
   }

   @Override
   protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
      super.addSlots(builder, listener, updateSortingListener);
      builder.addContainer(this.extraSlot = com.jerry.mekextras.common.inventory.slot.chemical.ExtraFactoryChemicalInventorySlot.fillOrConverts(this, this.chemicalTank, this::getLevel, listener, 7, 57));
   }

   public IChemicalTank getChemicalTank() {
      return this.chemicalTank;
   }

   protected ChemicalInventorySlot getExtraSlot() {
      return this.extraSlot;
   }

   @Override
   public boolean isItemValidForSlot(ItemResource itemType) {
      return this.containsRecipeAB(itemType, (TypedInstance)this.chemicalTank.resource());
   }

   @Override
   public boolean isValidInputItem(ItemResource itemType) {
      return this.containsRecipeA(itemType);
   }

   protected int getNeededInput(ItemStackChemicalToItemStackRecipe recipe, ItemResource inputType) {
      return recipe.getItemInput().getNeededAmount(inputType);
   }

   @Override
   protected boolean isCachedRecipeValid(@Nullable CachedRecipe<ItemStackChemicalToItemStackRecipe> cached, ItemResource itemType) {
      if (cached == null) {
         return false;
      } else {
         ItemStackChemicalToItemStackRecipe cachedRecipe = (ItemStackChemicalToItemStackRecipe)cached.getRecipe();
         return cachedRecipe.getItemInput().testType(itemType)
            && (this.chemicalTank.isEmpty() || cachedRecipe.getChemicalInput().testType((TypedInstance)this.chemicalTank.resource()));
      }
   }

   protected @Nullable ItemStackChemicalToItemStackRecipe findRecipe(
      ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot
   ) {
      return (ItemStackChemicalToItemStackRecipe)((ItemChemical)this.getRecipeType().getInputCache())
         .findTypeBasedRecipe(this.level, fallbackInput, (ChemicalResource)this.chemicalTank.resource(), (ItemResource)outputSlot.resource(), CAN_OUTPUT_STACK);
   }

   @Override
   protected void handleSecondaryFuel() {
      this.extraSlot.fillTankOrConvert(null);
   }

   public IMekanismRecipeTypeProvider<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> getRecipeType() {
      return switch (this.type) {
         case COMPRESSING -> MekanismRecipeType.COMPRESSING;
         case INFUSING -> MekanismRecipeType.METALLURGIC_INFUSING;
         case INJECTING -> MekanismRecipeType.INJECTING;
         case PURIFYING -> MekanismRecipeType.PURIFYING;
         default -> throw new IllegalStateException("Unhandled factory type");
      };
   }

   public IRecipeViewerRecipeType<ItemStackChemicalToItemStackRecipe> recipeViewerType() {
      return switch (this.type) {
         case COMPRESSING -> RecipeViewerRecipeType.COMPRESSING;
         case INFUSING -> RecipeViewerRecipeType.METALLURGIC_INFUSING;
         case INJECTING -> RecipeViewerRecipeType.INJECTING;
         case PURIFYING -> RecipeViewerRecipeType.PURIFYING;
         default -> throw new IllegalStateException("Unhandled factory type");
      };
   }

   private boolean allowExtractingChemical() {
      FactoryType factoryType = ((AttributeFactoryType)Attribute.getOrThrow(this.getBlockHolder(), AttributeFactoryType.class)).getFactoryType();
      return factoryType == FactoryType.COMPRESSING || factoryType == FactoryType.INFUSING;
   }

   private boolean useStatisticalMechanics() {
      return MekanismConfig.usage.randomizedConsumption.get() && (this.type == FactoryType.INJECTING || this.type == FactoryType.PURIFYING);
   }

   public @Nullable ItemStackChemicalToItemStackRecipe getRecipe(int cacheIndex) {
      return (ItemStackChemicalToItemStackRecipe)this.findFirstRecipe(this.inputHandlers[cacheIndex], this.chemicalInputHandler);
   }

   public CachedRecipe<ItemStackChemicalToItemStackRecipe> createNewCachedRecipe(ItemStackChemicalToItemStackRecipe recipe, int cacheIndex) {
      CachedRecipe<ItemStackChemicalToItemStackRecipe> cachedRecipe;
      if (recipe.perTickUsage()) {
         cachedRecipe = ItemStackConstantChemicalToObjectCachedRecipe.create(
            recipe,
            this.recheckAllRecipeErrors[cacheIndex],
            this.inputHandlers[cacheIndex],
            this.chemicalInputHandler,
            this.chemicalUsageMultiplier,
            used -> this.usedSoFar[cacheIndex] = used,
            this.outputHandlers[cacheIndex]
         );
      } else {
         cachedRecipe = new TwoInputCachedRecipe<>(
            recipe, this.recheckAllRecipeErrors[cacheIndex], this.inputHandlers[cacheIndex], this.chemicalInputHandler, this.outputHandlers[cacheIndex]
         );
      }

      return cachedRecipe.setErrorsChanged(errors -> this.errorTracker.onErrorsChanged(errors, cacheIndex))
         .setCanHolderFunction(this::canFunction)
         .setActive(active -> this.setActiveState(active, cacheIndex))
         .setEnergyRequirements(this.energyContainer::getEnergyPerTick, this.energyContainer)
         .setRequiredTicks(this::getChemicalTicksRequired)
         .setOnFinish(this::markForSave)
         .setOperatingTicksChanged(operatingTicks -> this.progress[cacheIndex] = operatingTicks)
         .setBaselineMaxOperations(this::getOperationsPerTick);
   }

   @Override
   public boolean hasSecondaryResourceBar() {
      return true;
   }

   @Override
   public void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      Optional<int[]> savedUsage = input.getIntArray("used_so_far");
      if (savedUsage.isPresent()) {
         int[] savedUsed = savedUsage.get();
         if (this.tier.processes > savedUsed.length) {
            Arrays.fill(this.usedSoFar, 0);
         }

         for (int i = 0; i < this.tier.processes && i < savedUsed.length; i++) {
            this.usedSoFar[i] = savedUsed[i];
         }
      } else {
         Arrays.fill(this.usedSoFar, 0);
      }
   }

   @Override
   public void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putIntArray("used_so_far", this.usedSoFar);
   }

   public int getSavedUsedSoFar(int cacheIndex) {
      return this.usedSoFar[cacheIndex];
   }

   @Override
   public void recalculateUpgrades(Upgrade upgrade) {
      super.recalculateUpgrades(upgrade);
      if (upgrade == Upgrade.SPEED || upgrade == Upgrade.CHEMICAL && this.supportsUpgrade(Upgrade.CHEMICAL)) {
         if (this.useStatisticalMechanics()) {
            this.chemicalPerTickMeanMultiplier = MekanismUtils.getGasPerTickMeanMultiplier(this);
         } else {
            this.baseTotalUsage = MekanismUtils.getBaseUsage(this, 200);
         }
      }
   }

   @Override
   public void parseUpgradeData(IUpgradeData upgradeData, Provider provider, TransactionContext transaction) {
      if (upgradeData instanceof AdvancedMachineUpgradeData data) {
         super.parseUpgradeData(upgradeData, provider, transaction);
         this.chemicalTank.copyContents(data.stored, transaction);
         this.extraSlot.copyContents(data.chemicalSlot, transaction);
         System.arraycopy(data.usedSoFar, 0, this.usedSoFar, 0, data.usedSoFar.length);
      } else {
         Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
      }
   }

   public AdvancedMachineUpgradeData getUpgradeData(Provider provider) {
      return new AdvancedMachineUpgradeData(
         provider,
         this.redstone,
         this.getControlType(),
         this.energyContainer,
         this.progress,
         this.usedSoFar,
         this.chemicalTank,
         this.extraSlot,
         this.energySlot,
         this.inputSlots,
         this.outputSlots,
         this.isSorting(),
         this.getComponents(),
         this.problemPath()
      );
   }

   public void dump() {
      ContainerType.CHEMICAL.dumpOrClearContents(this.level, this.worldPosition, this.chemicalTank, null);
   }

   @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Empty the contents of the chemical tank into the environment")
   void dumpChemical() throws ComputerException {
      this.validateSecurityIsPublic();
      this.dump();
   }
}

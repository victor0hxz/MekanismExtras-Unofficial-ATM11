package com.jerry.mekextras.common.tile.factory;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.ToIntBiFunction;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.resource.IResourceContainer;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.Attribute;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.common.util.ExtraUpgradeUtils;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.capabilities.holder.single.SingleConfigHolder;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryInputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.registries.MekanismDataComponents;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.tile.prefab.TileEntityRecipeMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.MachineUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public abstract class TileEntityExtraFactory<RECIPE extends MekanismRecipe<?>> extends TileEntityConfigurableMachine implements IRecipeLookupHandler<RECIPE> {
   protected static final int BASE_TICKS_REQUIRED = 200;
   protected final FactoryRecipeCacheLookupMonitor<RECIPE>[] recipeCacheLookupMonitors;
   protected BooleanSupplier[] recheckAllRecipeErrors;
   protected final TileEntityExtraFactory.ErrorTracker errorTracker;
   private final boolean[] activeStates;
   protected final TileEntityExtraFactory.ProcessInfo[] processInfoSlots;
   public final ExtraFactoryTier tier;
   public final int[] progress;
   private int ticksRequired;
   protected int upgradeMaxOperations = 1;
   private int operationsPerTick;
   private boolean sorting;
   private boolean sortingNeeded;
   private long lastUsage;
   protected final FactoryType type;
   protected @UnknownNullability MachineEnergyContainer<TileEntityExtraFactory<?>> energyContainer;
   protected final List<IInventorySlot> inputSlots;
   protected final List<IInventorySlot> outputSlots;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
   @UnknownNullability EnergyInventorySlot energySlot;

   protected TileEntityExtraFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state, List<RecipeError> errorTypes, Set<RecipeError> globalErrorTypes) {
      ExtraFactoryTier tier = (ExtraFactoryTier)ExtraAttribute.getAdvancedTier(blockProvider, ExtraFactoryTier.class);
      this.tier = tier;
      this.recipeCacheLookupMonitors = new FactoryRecipeCacheLookupMonitor[tier.processes];
      this.processInfoSlots = new TileEntityExtraFactory.ProcessInfo[tier.processes];
      super(blockProvider, pos, state);
      this.ticksRequired = 200;
      this.operationsPerTick = 1;
      this.sortingNeeded = true;
      this.lastUsage = 0L;
      this.type = ((AttributeFactoryType)Attribute.getOrThrow(blockProvider, AttributeFactoryType.class)).getFactoryType();
      this.inputSlots = new ArrayList<>();
      this.outputSlots = new ArrayList<>();

      for (TileEntityExtraFactory.ProcessInfo info : this.processInfoSlots) {
         this.inputSlots.add(info.inputSlot());
         this.outputSlots.add(info.outputSlot());
         if (info.secondaryOutputSlot() != null) {
            this.outputSlots.add(info.secondaryOutputSlot());
         }
      }

      this.configComponent.setupItemIOConfig(this.inputSlots, this.outputSlots, this.energySlot, false);
      IInventorySlot extraSlot = this.getExtraSlot();
      if (extraSlot != null) {
         ConfigInfo itemConfig = this.configComponent.getConfig(TransmissionType.ITEM);
         if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, new IInventorySlot[]{extraSlot}));
         }
      }

      this.configComponent.setupInputConfig(TransmissionType.ENERGY, this.energyContainer);
      this.ejectorComponent.setOutputData(this.configComponent, new TransmissionType[]{TransmissionType.ITEM});
      this.progress = new int[tier.processes];
      this.activeStates = new boolean[tier.processes];
      this.recheckAllRecipeErrors = new BooleanSupplier[tier.processes];

      for (int i = 0; i < this.recheckAllRecipeErrors.length; i++) {
         this.recheckAllRecipeErrors[i] = TileEntityRecipeMachine.shouldRecheckAllErrors(this);
      }

      this.errorTracker = new TileEntityExtraFactory.ErrorTracker(errorTypes, globalErrorTypes, tier.processes);
   }

   protected IContentsListener markAllMonitorsChanged(IContentsListener listener) {
      return () -> {
         listener.onContentsChanged();

         for (FactoryRecipeCacheLookupMonitor<RECIPE> cacheLookupMonitor : this.recipeCacheLookupMonitors) {
            cacheLookupMonitor.onChange();
         }
      };
   }

   protected void presetVariables() {
      super.presetVariables();
      Runnable setSortingNeeded = () -> this.sortingNeeded = true;

      for (int i = 0; i < this.recipeCacheLookupMonitors.length; i++) {
         this.recipeCacheLookupMonitors[i] = new FactoryRecipeCacheLookupMonitor(this, i, setSortingNeeded);
      }
   }

   protected ISingleContainerHolder<IEnergyContainer> getInitialEnergyContainer(IContentsListener listener) {
      this.energyContainer = MachineEnergyContainer.input(this, () -> {
         listener.onContentsChanged();

         for (FactoryRecipeCacheLookupMonitor<RECIPE> cacheLookupMonitor : this.recipeCacheLookupMonitors) {
            cacheLookupMonitor.unpause();
         }
      });
      return SingleConfigHolder.energy(this.energyContainer, this);
   }

   protected IContainerHolder<IInventorySlot> getInitialInventory(IContentsListener listener) {
      MekContainerHelper<IInventorySlot> builder = MekContainerHelper.forSideWithItemConfig(this);
      this.addSlots(builder, listener, () -> {
         listener.onContentsChanged();
         this.sortingNeeded = true;
      });
      builder.addContainer(this.energySlot = EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 7, 13));
      return builder.build();
   }

   protected abstract void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener);

   protected @Nullable IInventorySlot getExtraSlot() {
      return null;
   }

   public FactoryType getFactoryType() {
      return this.type;
   }

   protected boolean onUpdateServer(ServerLevel level) {
      boolean sendUpdatePacket = super.onUpdateServer(level);
      this.energySlot.fillContainerOrConvert(null);
      this.handleSecondaryFuel();
      if (this.sortingNeeded && this.isSorting()) {
         this.sortingNeeded = false;
         this.sortInventory();
      } else if (!this.sortingNeeded && CommonWorldTickHandler.flushTagAndRecipeCaches) {
         this.sortingNeeded = true;
      }

      long prev = this.energyContainer.getAmountAsLong();

      for (int i = 0; i < this.recipeCacheLookupMonitors.length; i++) {
         if (!this.recipeCacheLookupMonitors[i].updateAndProcess()) {
            this.activeStates[i] = false;
         }
      }

      boolean isActive = false;

      for (boolean state : this.activeStates) {
         if (state) {
            isActive = true;
            break;
         }
      }

      this.setActive(isActive);
      this.lastUsage = isActive ? prev - this.energyContainer.getAmountAsLong() : 0L;
      return sendUpdatePacket;
   }

   public boolean inputProducesOutput(
      int process, ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot, boolean updateCache
   ) {
      return outputSlot.isEmpty() || this.getRecipeForInput(process, fallbackInput, outputSlot, secondaryOutputSlot, false, updateCache) != null;
   }

   @Contract("null, _ -> false")
   protected abstract boolean isCachedRecipeValid(@Nullable CachedRecipe<RECIPE> cached, ItemResource itemType);

   private @Nullable RECIPE getRecipeForInput(
      int process,
      ItemResource fallbackInput,
      IInventorySlot outputSlot,
      @Nullable IInventorySlot secondaryOutputSlot,
      boolean skipCacheLookup,
      boolean updateCache
   ) {
      if (!skipCacheLookup && !CommonWorldTickHandler.flushTagAndRecipeCaches) {
         CachedRecipe<RECIPE> cached = this.getCachedRecipe(process);
         if (this.isCachedRecipeValid(cached, fallbackInput)) {
            return (RECIPE)cached.getRecipe();
         }
      }

      RECIPE foundRecipe = this.findRecipe(fallbackInput, outputSlot, secondaryOutputSlot);
      if (foundRecipe == null) {
         return null;
      } else {
         if (updateCache) {
            this.recipeCacheLookupMonitors[process].updateCachedRecipe(foundRecipe);
         }

         return foundRecipe;
      }
   }

   protected abstract @Nullable RECIPE findRecipe(ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot);

   protected abstract int getNeededInput(RECIPE recipe, ItemResource inputType);

   private @Nullable CachedRecipe<RECIPE> getCachedRecipe(int cacheIndex) {
      return this.recipeCacheLookupMonitors[cacheIndex].getCachedRecipe(cacheIndex);
   }

   public BooleanSupplier getWarningCheck(RecipeError error, int processIndex) {
      return this.errorTracker.getWarningCheck(error, processIndex);
   }

   public void clearRecipeErrors(int cacheIndex) {
      Arrays.fill(this.errorTracker.trackedErrors[cacheIndex], false);
   }

   protected void setActiveState(boolean state, int cacheIndex) {
      this.activeStates[cacheIndex] = state;
   }

   protected void handleSecondaryFuel() {
   }

   public abstract boolean isItemValidForSlot(ItemResource itemType);

   public abstract boolean isValidInputItem(ItemResource itemType);

   public int getProgress(int cacheIndex) {
      return this.progress[cacheIndex];
   }

   public int getSavedOperatingTicks(int cacheIndex) {
      return this.getProgress(cacheIndex);
   }

   public double getScaledProgress(int i, int process) {
      return (double)this.getProgress(process) * i / this.ticksRequired;
   }

   public void toggleSorting() {
      this.sorting = !this.isSorting();
      this.markForSave();
   }

   @ComputerMethod(nameOverride = "isAutoSortEnabled")
   public boolean isSorting() {
      return this.sorting;
   }

   @ComputerMethod(nameOverride = "getEnergyUsage", methodDescription = "Get the energy used in the last tick by the machine")
   public long getLastUsage() {
      return this.lastUsage;
   }

   @ComputerMethod(methodDescription = "Total number of ticks it takes currently for the recipe to complete")
   public int getTicksRequired() {
      return getComponent().getUpgrades(ExtraUpgrade.CREATIVE) > 0 ? 0 : this.ticksRequired;
   }

   public int getChemicalTicksRequired() { return ticksRequired; }

   public int getOperationsPerTick() {
      return this.operationsPerTick;
   }

   public void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      Optional<int[]> optionalProgress = input.getIntArray("progress");
      if (optionalProgress.isPresent()) {
         int[] savedProgress = optionalProgress.get();
         if (this.tier.processes != savedProgress.length) {
            Arrays.fill(this.progress, 0);
         }

         for (int i = 0; i < this.tier.processes && i < savedProgress.length; i++) {
            this.progress[i] = savedProgress[i];
         }
      }
   }

   public void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putIntArray("progress", Arrays.copyOf(this.progress, this.progress.length));
   }

   public void writeSustainedData(ValueOutput output) {
      super.writeSustainedData(output);
      output.putBoolean("sorting", this.isSorting());
   }

   public void readSustainedData(ValueInput input) {
      super.readSustainedData(input);
      this.sorting = input.getBooleanOr("sorting", this.sorting);
   }

   protected void collectImplicitComponents(Builder builder) {
      super.collectImplicitComponents(builder);
      builder.set(MekanismDataComponents.SORTING, this.isSorting());
   }

   protected void applyImplicitComponents(DataComponentGetter input) {
      super.applyImplicitComponents(input);
      this.sorting = (Boolean)input.getOrDefault(MekanismDataComponents.SORTING, this.sorting);
   }

   public void recalculateUpgrades(Upgrade upgrade) {
      super.recalculateUpgrades(upgrade);
      if (upgrade == Upgrade.SPEED || upgrade == ExtraUpgrade.STACK) {
         upgradeMaxOperations = 1 << Math.min(30, getComponent().getUpgrades(ExtraUpgrade.STACK));
         this.ticksRequired = MekanismUtils.getTicks(this, 200);
         this.operationsPerTick = MekanismUtils.getOperationsPerTick(this, 200, upgradeMaxOperations);
      }
   }

   public List<Component> getInfo(Upgrade upgrade) {
      return ExtraUpgradeUtils.getExpScaledInfo(UpgradeUtils.getMultScaledInfo(this, upgrade), this, upgrade);
   }

   public boolean isConfigurationDataCompatible(Block blockType) {
      return super.isConfigurationDataCompatible(blockType) || MekanismUtils.isSameTypeFactory(this.getBlockHolder(), blockType);
   }

   public boolean hasSecondaryResourceBar() {
      return false;
   }

   public MachineEnergyContainer<TileEntityExtraFactory<?>> energyContainer() {
      return this.energyContainer;
   }

   public void addContainerTrackers(MekanismContainer container) {
      super.addContainerTrackers(container);
      container.trackArray(this.progress);
      this.errorTracker.track(container);
      container.track(SyncableLong.create(this::getLastUsage, value -> this.lastUsage = value));
      container.track(SyncableBoolean.create(this::isSorting, value -> this.sorting = value));
      container.track(SyncableInt.create(this::getTicksRequired, value -> this.ticksRequired = value));
   }

   public void parseUpgradeData(IUpgradeData upgradeData, Provider provider, TransactionContext transaction) {
      if (upgradeData instanceof MachineUpgradeData data) {
         this.redstone = data.redstone;
         this.setControlType(data.controlType);
         this.energyContainer.copyContents(data.energyContainer, transaction);
         this.sorting = data.sorting;
         this.energySlot.copyContents(data.energySlot, transaction);
         System.arraycopy(data.progress, 0, this.progress, 0, data.progress.length);

         for (int i = 0; i < data.inputSlots.size(); i++) {
            this.inputSlots.get(i).copyContents((IResourceContainer)data.inputSlots.get(i), transaction);
         }

         for (int i = 0; i < data.outputSlots.size(); i++) {
            this.outputSlots.get(i).copyContents((IResourceContainer)data.outputSlots.get(i), transaction);
         }

         ScopedCollector reporter = new ScopedCollector(this.problemPath(), Mekanism.logger);

         try {
            ValueInput input = TagValueInput.create(reporter, provider, data.components);

            for (ITileComponent component : this.getComponents()) {
               component.read(input);
            }
         } catch (Throwable var10) {
            try {
               reporter.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         reporter.close();
      } else {
         super.parseUpgradeData(upgradeData, provider, transaction);
      }
   }

   protected void validateValidProcess(int process) throws ComputerException {
      if (process < 0 || process >= this.progress.length) {
         throw new ComputerException(
            "Process: '%d' is out of bounds, as this factory only has '%d' processes (zero indexed).", new Object[]{process, this.progress.length}
         );
      }
   }

   @ComputerMethod(requiresPublicSecurity = true)
   void setAutoSort(boolean enabled) throws ComputerException {
      this.validateSecurityIsPublic();
      if (this.sorting != enabled) {
         this.sorting = enabled;
         this.markForSave();
      }
   }

   @ComputerMethod
   int getRecipeProgress(int process) throws ComputerException {
      this.validateValidProcess(process);
      return this.getProgress(process);
   }

   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getInput", docPlaceholder = "input slot")
   IInventorySlot getInputSlot(int process) throws ComputerException {
      this.validateValidProcess(process);
      return this.processInfoSlots[process].inputSlot();
   }

   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getOutput", docPlaceholder = "output slot")
   IInventorySlot getOutputSlot(int process) throws ComputerException {
      this.validateValidProcess(process);
      return this.processInfoSlots[process].outputSlot();
   }

   private void sortInventory() {
      Map<ItemResource, TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE>> processes = new HashMap<>();
      List<TileEntityExtraFactory.ProcessInfo> emptyProcesses = new ArrayList<>();

      for (TileEntityExtraFactory.ProcessInfo processInfo : this.processInfoSlots) {
         IInventorySlot inputSlot = processInfo.inputSlot();
         if (inputSlot.isEmpty()) {
            emptyProcesses.add(processInfo);
         } else {
            ItemResource inputType = (ItemResource)inputSlot.resource();
            TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE> recipeProcessInfo = processes.computeIfAbsent(
               inputType, TileEntityExtraFactory.RecipeProcessInfo::new
            );
            recipeProcessInfo.processes.add(processInfo);
            recipeProcessInfo.totalCount = recipeProcessInfo.totalCount + inputSlot.amountAsLong();
            if (recipeProcessInfo.lazyMinPerSlot == null && !CommonWorldTickHandler.flushTagAndRecipeCaches) {
               CachedRecipe<RECIPE> cachedRecipe = this.getCachedRecipe(processInfo.process());
               if (this.isCachedRecipeValid(cachedRecipe, inputType)) {
                  recipeProcessInfo.recipe = (RECIPE)cachedRecipe.getRecipe();
                  recipeProcessInfo.lazyMinPerSlot = (info, factory) -> info.recipe == null ? 1 : factory.getNeededInput(info.recipe, info.item);
               }
            }
         }
      }

      if (!processes.isEmpty()) {
         Collection<TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE>> processInfos = processes.values();

         for (TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE> recipeProcessInfo : processInfos) {
            if (recipeProcessInfo.lazyMinPerSlot == null) {
               recipeProcessInfo.lazyMinPerSlot = (info, factory) -> {
                  TileEntityExtraFactory.ProcessInfo processInfox = info.processes.getFirst();
                  info.recipe = factory.getRecipeForInput(
                     processInfox.process(), info.item, processInfox.outputSlot(), processInfox.secondaryOutputSlot(), true, true
                  );
                  return info.recipe == null ? 1 : factory.getNeededInput(info.recipe, info.item);
               };
            }
         }

         if (!emptyProcesses.isEmpty()) {
            this.addEmptySlotsAsTargets(processInfos, emptyProcesses);
         }

         this.distributeItems(processInfos);
      }
   }

   private void addEmptySlotsAsTargets(
      Collection<TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE>> processes, List<TileEntityExtraFactory.ProcessInfo> emptyProcesses
   ) {
      for (TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE> recipeProcessInfo : processes) {
         long minPerSlot = recipeProcessInfo.getMinPerSlot(this);
         long maxSlots = recipeProcessInfo.totalCount / minPerSlot;
         if (maxSlots > 1L) {
            int processCount = recipeProcessInfo.processes.size();
            if (maxSlots > processCount) {
               long emptyToAdd = maxSlots - processCount;
               int added = 0;
               Iterator<TileEntityExtraFactory.ProcessInfo> iter = emptyProcesses.iterator();

               while (true) {
                  if (iter.hasNext()) {
                     TileEntityExtraFactory.ProcessInfo emptyProcess = iter.next();
                     if (!this.inputProducesOutput(
                        emptyProcess.process(), recipeProcessInfo.item, emptyProcess.outputSlot(), emptyProcess.secondaryOutputSlot(), true
                     )) {
                        continue;
                     }

                     recipeProcessInfo.processes.add(emptyProcess);
                     iter.remove();
                     if (++added < emptyToAdd) {
                        continue;
                     }
                  }

                  if (emptyProcesses.isEmpty()) {
                     return;
                  }
                  break;
               }
            }
         }
      }
   }

   private void distributeItems(Collection<TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE>> processes) {
      for (TileEntityExtraFactory.RecipeProcessInfo<ItemResource, RECIPE> recipeProcessInfo : processes) {
         int processCount = recipeProcessInfo.processes.size();
         if (processCount != 1) {
            int maxStackSize = recipeProcessInfo.item.getMaxStackSize();
            long numberPerSlot = recipeProcessInfo.totalCount / processCount;
            if (numberPerSlot != maxStackSize) {
               long remainder = recipeProcessInfo.totalCount % processCount;
               long minPerSlot = recipeProcessInfo.getMinPerSlot(this);
               if (minPerSlot > 1L) {
                  long perSlotRemainder = numberPerSlot % minPerSlot;
                  if (perSlotRemainder > 0L) {
                     numberPerSlot -= perSlotRemainder;
                     remainder += perSlotRemainder * processCount;
                  }

                  if (numberPerSlot + minPerSlot > maxStackSize) {
                     minPerSlot = maxStackSize - numberPerSlot;
                  }
               }

               for (TileEntityExtraFactory.ProcessInfo processInfo : recipeProcessInfo.processes) {
                  IInventorySlot inputSlot = processInfo.inputSlot();
                  long sizeForSlot = numberPerSlot;
                  if (remainder > 0L) {
                     if (remainder > minPerSlot) {
                        sizeForSlot = numberPerSlot + minPerSlot;
                        remainder -= minPerSlot;
                     } else {
                        sizeForSlot = numberPerSlot + remainder;
                        remainder = 0L;
                     }
                  }

                  inputSlot.setContents(recipeProcessInfo.item, sizeForSlot, null);
               }
            }
         }
      }
   }

   protected static class ErrorTracker {
      private final List<RecipeError> errorTypes;
      private final IntSet globalTypes;
      private final boolean[][] trackedErrors;
      private final int processes;

      public ErrorTracker(List<RecipeError> errorTypes, Set<RecipeError> globalErrorTypes, int processes) {
         this.errorTypes = List.copyOf(errorTypes);
         this.globalTypes = new IntArraySet(globalErrorTypes.size());

         for (int i = 0; i < this.errorTypes.size(); i++) {
            RecipeError error = this.errorTypes.get(i);
            if (globalErrorTypes.contains(error)) {
               this.globalTypes.add(i);
            }
         }

         this.processes = processes;
         this.trackedErrors = new boolean[this.processes][];
         int errors = this.errorTypes.size();

         for (int ix = 0; ix < this.trackedErrors.length; ix++) {
            this.trackedErrors[ix] = new boolean[errors];
         }
      }

      private void track(MekanismContainer container) {
         container.trackArray(this.trackedErrors);
      }

      public void onErrorsChanged(Set<RecipeError> errors, int processIndex) {
         boolean[] processTrackedErrors = this.trackedErrors[processIndex];

         for (int i = 0; i < processTrackedErrors.length; i++) {
            processTrackedErrors[i] = errors.contains(this.errorTypes.get(i));
         }
      }

      private BooleanSupplier getWarningCheck(RecipeError error, int processIndex) {
         if (processIndex >= 0 && processIndex < this.processes) {
            int errorIndex = this.errorTypes.indexOf(error);
            if (errorIndex >= 0) {
               if (this.globalTypes.contains(errorIndex)) {
                  return () -> {
                     for (boolean[] tracked : this.trackedErrors) {
                        if (tracked[errorIndex]) {
                           return true;
                        }
                     }

                     return false;
                  };
               }

               return () -> this.trackedErrors[processIndex][errorIndex];
            }
         }

         return () -> false;
      }
   }

   public record ProcessInfo(int process, ExtraFactoryInputInventorySlot inputSlot, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
   }

   private static class RecipeProcessInfo<ITEM, RECIPE extends MekanismRecipe<?>> {
      private final List<TileEntityExtraFactory.ProcessInfo> processes = new ArrayList<>();
      private final ITEM item;
      private @Nullable ToIntBiFunction<TileEntityExtraFactory.RecipeProcessInfo<ITEM, RECIPE>, TileEntityExtraFactory<RECIPE>> lazyMinPerSlot;
      private @Nullable RECIPE recipe;
      private long minPerSlot = 1L;
      private long totalCount;

      public RecipeProcessInfo(ITEM item) {
         this.item = item;
      }

      public long getMinPerSlot(TileEntityExtraFactory<RECIPE> factory) {
         if (this.lazyMinPerSlot != null) {
            this.minPerSlot = Math.max(1, this.lazyMinPerSlot.applyAsInt(this, factory));
            this.lazyMinPerSlot = null;
         }

         return this.minPerSlot;
      }
   }
}

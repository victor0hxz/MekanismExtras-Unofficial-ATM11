package com.jerry.mekextras.common.content.matrix;

import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import mekanism.api.AutomationType;
import mekanism.api.MekanismPreconditions;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transaction.SimpleLongJournal;
import com.jerry.mekextras.common.tier.IPTier;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionCell;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionProvider;
import mekanism.common.util.EnergyUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

public class ReinforcedMatrixEnergyContainer implements IEnergyContainer {
   private final Map<BlockPos, IPTier> providers = new Object2ObjectOpenHashMap();
   private final Map<BlockPos, IEnergyContainer> cells = new Object2ObjectOpenHashMap();
   private final Set<BlockPos> invalidPositions = new ObjectOpenHashSet();
   private final SimpleLongJournal queuedInput = new SimpleLongJournal();
   private final SimpleLongJournal queuedOutput = new SimpleLongJournal();
   private long lastInput = 0L;
   private long lastOutput = 0L;
   private long cachedTotal = 0L;
   private long transferCap = 0L;
   private long storageCap = 0L;
   private final ReinforcedMatrixMultiblockData multiblock;

   public ReinforcedMatrixEnergyContainer(ReinforcedMatrixMultiblockData multiblock) {
      this.multiblock = multiblock;
   }

   public void addCell(BlockPos pos, TileEntityExtraInductionCell cell) {
      MachineEnergyContainer<TileEntityExtraInductionCell> energyContainer = cell.energyContainer();
      this.cells.put(pos, energyContainer);
      this.storageCap = MathUtils.addClamped(this.storageCap, energyContainer.getCapacityAsLong());
      this.cachedTotal = MathUtils.addClamped(this.cachedTotal, energyContainer.getAmountAsLong());
   }

   public void addProvider(BlockPos pos, TileEntityExtraInductionProvider provider) {
      this.providers.put(pos, provider.tier);
      this.transferCap = MathUtils.addClamped(this.transferCap, provider.tier.getOutput());
   }

   public void removeInternal(BlockPos pos) {
      if (this.invalidPositions.add(pos)) {
         if (this.providers.containsKey(pos)) {
            this.transferCap = this.transferCap - this.providers.get(pos).getOutput();
         } else if (this.cells.containsKey(pos)) {
            IEnergyContainer cellContainer = this.cells.get(pos);
            this.storageCap = this.storageCap - cellContainer.getCapacityAsLong();
            this.cachedTotal = this.cachedTotal - cellContainer.getAmountAsLong();
         }
      }
   }

   public void invalidate() {
      this.tick(Collections.emptyList(), null, null, null);
      this.cells.clear();
      this.providers.clear();
      this.queuedOutput.value = 0L;
      this.queuedInput.value = 0L;
      this.lastOutput = 0L;
      this.lastInput = 0L;
      this.cachedTotal = 0L;
      this.transferCap = 0L;
      this.storageCap = 0L;
   }

   public boolean tick(
      Collection<BlockCapabilityCache<EnergyHandler, @Nullable Direction>> targets,
      @Nullable EnergyInventorySlot energyInputSlot,
      @Nullable EnergyInventorySlot energyOutputSlot,
      @Nullable TransactionContext transaction
   ) {
      if (!this.invalidPositions.isEmpty()) {
         for (BlockPos invalidPosition : this.invalidPositions) {
            this.cells.remove(invalidPosition);
            this.providers.remove(invalidPosition);
         }

         this.invalidPositions.clear();
      }

      if (this.queuedInput.value != this.queuedOutput.value || !targets.isEmpty()) {
         Transaction subTransaction = Transaction.open(transaction);

         try {
            if (energyInputSlot != null) {
               energyInputSlot.drainContainerIntoSlot(subTransaction);
            }

            if (energyOutputSlot != null) {
               energyOutputSlot.fillContainerOrConvert(subTransaction);
            }

            if (!targets.isEmpty()) {
               // The output limit is a rate, not stored energy. Never emit more than
               // the cells contain (including transfers queued in this transaction).
               long available = Math.max(0L, Math.min(this.getRemainingOutput(), this.getAmountAsLong()));
               long sent = EnergyUtils.emit(targets, available, subTransaction);
               if (sent > 0L) {
                  this.queuedOutput.updateSnapshots(subTransaction);
                  this.queuedOutput.value += sent;
               }
            }

            if (this.queuedInput.value < this.queuedOutput.value) {
               this.removeEnergy(-this.getQueuedChange(), subTransaction);
            } else if (this.queuedInput.value > this.queuedOutput.value) {
               this.addEnergy(this.getQueuedChange(), subTransaction);
            }

            subTransaction.commit();
         } catch (Throwable var9) {
            if (subTransaction != null) {
               try {
                  subTransaction.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (subTransaction != null) {
            subTransaction.close();
         }
      }

      this.lastInput = this.queuedInput.value;
      this.lastOutput = this.queuedOutput.value;
      this.queuedInput.value = 0L;
      this.queuedOutput.value = 0L;
      return this.getLastInput() > 0L || this.getLastOutput() > 0L;
   }

   private void addEnergy(long energy, TransactionContext transaction) {
      this.cachedTotal += energy;

      for (IEnergyContainer container : this.cells.values()) {
         long stored = container.getAmountAsLong();
         long needed = container.getCapacityAsLong() - stored;
         if (needed > 0L) {
            if (energy <= needed) {
               container.setEnergy(stored + energy, transaction);
               break;
            }

            container.setEnergy(stored + needed, transaction);
            energy -= needed;
         }
      }
   }

   private void removeEnergy(long energy, TransactionContext transaction) {
      this.cachedTotal -= energy;

      for (IEnergyContainer container : this.cells.values()) {
         long stored = container.getAmountAsLong();
         if (stored > 0L) {
            if (energy <= stored) {
               container.setEnergy(stored - energy, transaction);
               break;
            }

            container.setEnergy(0L, transaction);
            energy -= stored;
         }
      }
   }

   private long getQueuedChange() {
      return this.queuedInput.value - this.queuedOutput.value;
   }

   public long getAmountAsLong() {
      return this.cachedTotal + this.getQueuedChange();
   }

   public void setEnergy(@Range(from = 0L, to = Long.MAX_VALUE) long energy, @Nullable TransactionContext transaction) {
      throw new RuntimeException("Unexpected call to setEnergy. The matrix energy container does not support directly setting the energy.");
   }

   public @Range(from = 0L, to = 2147483647L) int insert(
      @Range(from = 0L, to = 2147483647L) int amount, TransactionContext transaction, AutomationType automationType
   ) {
      MekanismPreconditions.checkNonNegative(amount);
      if (amount != 0 && this.multiblock.isFormed() && this.isValidForInsertion(automationType)) {
         int toAdd = Ints.saturatedCast(Math.min(Math.min((long)amount, this.getRemainingInput()), this.getNeededAsLong()));
         if (toAdd > 0) {
            this.queuedInput.updateSnapshots(transaction);
            this.queuedInput.value += toAdd;
         }

         return toAdd;
      } else {
         return 0;
      }
   }

   public @Range(from = 0L, to = 2147483647L) int extract(
      @Range(from = 0L, to = 2147483647L) int amount, TransactionContext transaction, AutomationType automationType
   ) {
      MekanismPreconditions.checkNonNegative(amount);
      if (!this.isEmpty() && amount != 0 && this.multiblock.isFormed() && this.isValidForExtraction(automationType)) {
         int toRemove = Ints.saturatedCast(Math.min(Math.min((long)amount, this.getRemainingOutput()), this.getAmountAsLong()));
         if (toRemove > 0) {
            this.queuedOutput.updateSnapshots(transaction);
            this.queuedOutput.value += toRemove;
         }

         return toRemove;
      } else {
         return 0;
      }
   }

   public boolean isValidForExtraction(AutomationType automationType) {
      return !automationType.isExternal();
   }

   public @Range(from = 0L, to = Long.MAX_VALUE) long getCapacityAsLong() {
      return this.storageCap;
   }

   public void serialize(ValueOutput output) {
   }

   public void deserialize(ValueInput input) {
   }

   public void copyContents(IEnergyContainer other, @Nullable TransactionContext transaction) {
   }

   private long getRemainingInput() {
      return this.transferCap - this.queuedInput.value;
   }

   private long getRemainingOutput() {
      return this.transferCap - this.queuedOutput.value;
   }

   public long getMaxTransfer() {
      return this.transferCap;
   }

   public long getLastInput() {
      return this.lastInput;
   }

   public long getLastOutput() {
      return this.lastOutput;
   }

   public int getCells() {
      return this.cells.size();
   }

   public int getProviders() {
      return this.providers.size();
   }
}

package com.jerry.mekextras.common.content.matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.multiblock.MultiblockCache;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.lib.multiblock.Structure;
import mekanism.common.lib.multiblock.IValveHandler.ValveData;
import mekanism.common.lib.multiblock.MultiblockCache.CacheSubstance;
import mekanism.common.lib.multiblock.MultiblockData.CapabilityOutputTarget;
import com.jerry.mekextras.common.tile.multiblock.TileEntityReinforcedInductionCasing;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionCell;
import com.jerry.mekextras.common.tile.multiblock.TileEntityReinforcedInductionPort;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionProvider;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public class ReinforcedMatrixMultiblockData extends MultiblockData {
   public static final String STATS_TAB = "stats";
   private final List<CapabilityOutputTarget<EnergyHandler>> energyOutputTargets = new ArrayList<>();
   private final ReinforcedMatrixEnergyContainer energyContainer;
   @ContainerSync(getter = "getLastOutput")
   private long clientLastOutput = 0L;
   @ContainerSync(getter = "getLastInput")
   private long clientLastInput = 0L;
   @ContainerSync(getter = "getEnergy")
   private long clientEnergy = 0L;
   @ContainerSync(tags = "stats", getter = "getTransferCap")
   private long clientMaxTransfer = 0L;
   @ContainerSync(getter = "getStorageCap")
   private long clientMaxEnergy = 0L;
   @ContainerSync(tags = "stats", getter = "getProviderCount")
   private int clientProviders;
   @ContainerSync(tags = "stats", getter = "getCellCount")
   private int clientCells;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getInputItem", docPlaceholder = "input slot")
   final EnergyInventorySlot energyInputSlot;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getOutputItem", docPlaceholder = "output slot")
   final EnergyInventorySlot energyOutputSlot;

   public ReinforcedMatrixMultiblockData(TileEntityReinforcedInductionCasing tile) {
      super(tile);
      this.energyContainer = new ReinforcedMatrixEnergyContainer(this);
      this.inventorySlots.add(this.energyInputSlot = EnergyInventorySlot.drain(this.energyContainer, this, 146, 21));
      this.inventorySlots.add(this.energyOutputSlot = EnergyInventorySlot.fillOrConvert(this.energyContainer, tile::getLevel, this, 146, 51));
      this.energyInputSlot.setSlotOverlay(SlotOverlay.PLUS);
      this.energyOutputSlot.setSlotOverlay(SlotOverlay.MINUS);
   }

   protected int getMultiblockRedstoneLevel() {
      return MekanismUtils.redstoneLevelFromContents(this.getEnergy(), this.getStorageCap());
   }

   protected boolean shouldCache(CacheSubstance<?> type) {
      return type != MultiblockCache.ENERGY;
   }

   public void addCell(TileEntityExtraInductionCell cell) {
      this.energyContainer.addCell(cell.getBlockPos(), cell);
   }

   public void addProvider(TileEntityExtraInductionProvider provider) {
      this.energyContainer.addProvider(provider.getBlockPos(), provider);
   }

   public ReinforcedMatrixEnergyContainer energyContainer() {
      return this.energyContainer;
   }

   public long getEnergy() {
      return this.isRemote() ? this.clientEnergy : this.energyContainer.getAmountAsLong();
   }

   public boolean tick(ServerLevel world) {
      boolean ret = super.tick(world);
      if (this.energyContainer.tick(this.getActiveOutputs(this.energyOutputTargets), this.energyInputSlot, this.energyOutputSlot, null)) {
         this.markDirtyComparator(world);
      }

      return ret;
   }

   public void remove(LevelReader world, Structure oldStructure) {
      this.energyContainer.invalidate();
      super.remove(world, oldStructure);
   }

   protected void updateEjectors(Level world) {
      this.energyOutputTargets.clear();

      for (Entry<BlockPos, ValveData> entry : this.valves.entrySet()) {
         TileEntityReinforcedInductionPort tile = (TileEntityReinforcedInductionPort)WorldUtils.getTileEntity(TileEntityReinforcedInductionPort.class, world, entry.getKey());
         if (tile != null) {
            tile.addEnergyTargetCapability(this.energyOutputTargets, entry.getValue().side);
         }
      }
   }

   public long getStorageCap() {
      return this.isRemote() ? this.clientMaxEnergy : this.energyContainer.getCapacityAsLong();
   }

   @ComputerMethod
   public long getTransferCap() {
      return this.isRemote() ? this.clientMaxTransfer : this.energyContainer.getMaxTransfer();
   }

   @ComputerMethod
   public long getLastInput() {
      return this.isRemote() ? this.clientLastInput : this.energyContainer.getLastInput();
   }

   @ComputerMethod
   public long getLastOutput() {
      return this.isRemote() ? this.clientLastOutput : this.energyContainer.getLastOutput();
   }

   @ComputerMethod(nameOverride = "getInstalledCells")
   public int getCellCount() {
      return this.isRemote() ? this.clientCells : this.energyContainer.getCells();
   }

   @ComputerMethod(nameOverride = "getInstalledProviders")
   public int getProviderCount() {
      return this.isRemote() ? this.clientProviders : this.energyContainer.getProviders();
   }
}

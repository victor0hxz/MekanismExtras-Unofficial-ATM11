package com.jerry.mekextras.common.tile;

import java.util.Objects;
import java.util.function.Supplier;
import mekanism.api.AutomationType;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.fluid.IFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.resource.ResourceContainerWrapper;
import mekanism.common.Mekanism;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import mekanism.common.capabilities.Capabilities;
import com.jerry.mekextras.common.capabilities.fluid.ExtraFluidTankFluidTank;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.capabilities.proxy.BelowContainerCache;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerFluidTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.registries.MekanismDataComponents;
import com.jerry.mekextras.common.tier.FTTier;

import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.interfaces.IFluidContainerManager;
import mekanism.common.tile.interfaces.IFluidContainerManager.ContainerEditMode;
import mekanism.common.upgrade.FluidTankUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.ResourceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraFluidTank extends TileEntityMekanism implements IConfigurable, IFluidContainerManager {
   @WrappingComputerMethod(
      wrapper = ComputerFluidTankWrapper.class,
      methodNames = {"getStored", "getCapacity", "getNeeded", "getFilledPercentage"},
      docPlaceholder = "tank"
   )
   public @UnknownNullability ExtraFluidTankFluidTank fluidTank;
   private @Nullable BelowContainerCache<FluidResource, IFluidTank> belowTankCache;
   private ContainerEditMode editMode;
   public final FTTier tier;
   private final TileEntityExtraFluidTank.ValveJournal valveJournal;
   public float prevScale;
   private boolean needsPacket;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getInputItem", docPlaceholder = "input slot")
   @UnknownNullability FluidInventorySlot inputSlot;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getOutputItem", docPlaceholder = "output slot")
   @UnknownNullability OutputInventorySlot outputSlot;
   private int lastLightLevel;
   private int lightUpdateDelay;

   public TileEntityExtraFluidTank(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      this.tier = (FTTier)ExtraAttribute.getAdvancedTier(blockProvider, FTTier.class);
      super(blockProvider, pos, state);
      this.editMode = ContainerEditMode.BOTH;
      this.delaySupplier = NO_DELAY;
      this.valveJournal = new TileEntityExtraFluidTank.ValveJournal();
   }

   private TileEntityExtraFluidTank.ValveJournal getValveJournal() {
      return this.valveJournal;
   }

   protected IContainerHolder<IFluidTank> getInitialFluidTanks(IContentsListener listener) {
      MekContainerHelper<IFluidTank> builder = MekContainerHelper.forSideWithOverrides(this.facingSupplier);
      this.fluidTank = (ExtraFluidTankFluidTank)builder.addContainer(
         ExtraFluidTankFluidTank.create(this, listener),
         (tank, side) -> (IFluidTank)(side == RelativeSide.TOP ? new TileEntityExtraFluidTank.ValveFluidTankWrapper(tank, this::getValveJournal) : tank)
      );
      return builder.build();
   }

   protected IContainerHolder<IInventorySlot> getInitialInventory(IContentsListener listener) {
      MekContainerHelper<IInventorySlot> builder = MekContainerHelper.forSide(this.facingSupplier);
      builder.addContainer(this.inputSlot = FluidInventorySlot.input(this.fluidTank, listener, 146, 19));
      builder.addContainer(this.outputSlot = OutputInventorySlot.at(listener, 146, 51));
      this.inputSlot.setSlotOverlay(SlotOverlay.INPUT);
      this.outputSlot.setSlotOverlay(SlotOverlay.OUTPUT);
      return builder.build();
   }

   protected void onUpdateClient(Level level) {
      super.onUpdateClient(level);
      this.checkLight(level);
   }

   private void checkLight(Level level) {
      if (this.lightUpdateDelay > 0) {
         this.lightUpdateDelay--;
         if (this.lightUpdateDelay == 0) {
            int lightLevel = this.getBlockState().getLightEmission(level, this.worldPosition);
            if (lightLevel != this.lastLightLevel) {
               this.lastLightLevel = lightLevel;
               level.getLightEngine().checkBlock(this.worldPosition);
            }
         }
      }
   }

   protected boolean onUpdateServer(ServerLevel level) {
      boolean sendUpdatePacket = super.onUpdateServer(level);
      if (this.valveJournal.tick()) {
         sendUpdatePacket = true;
      }

      this.checkLight(level);
      float scale = MekanismUtils.getScale(this.prevScale, this.fluidTank);
      if (MekanismUtils.scaleChanged(scale, this.prevScale)) {
         if ((this.prevScale == 0.0F || scale == 0.0F) && this.lightUpdateDelay == 0) {
            this.lightUpdateDelay = this.prevScale == 0.0F ? 1 : MekanismConfig.general.blockDeactivationDelay.get();
         }

         this.prevScale = scale;
         sendUpdatePacket = true;
      }

      this.inputSlot.handleTank(this.outputSlot, this.editMode, null);
      if (this.getActive()) {
         if (this.belowTankCache == null) {
            this.belowTankCache = new BelowContainerCache<>(Capabilities.FLUID, level, this.worldPosition);
         }

         int toEmit = this.tier.getOutput();
         IFluidTank below = (IFluidTank)this.belowTankCache.getContainer(ExtraFluidTankFluidTank.class);
         if (below != null) {
            toEmit = Math.min(below.getNeededAsInt(FluidResource.EMPTY), toEmit);
         }

         ResourceUtils.emit(this.belowTankCache.getHandler(), this.fluidTank, toEmit, null);
      }

      if (this.needsPacket) {
         sendUpdatePacket = true;
         this.needsPacket = false;
      }

      return sendUpdatePacket;
   }

   public void setLevel(Level world) {
      super.setLevel(world);
      this.belowTankCache = null;
   }

   public void writeSustainedData(ValueOutput output) {
      super.writeSustainedData(output);
      NBTUtils.writeEnum(output, "edit_mode", this.editMode);
   }

   public void readSustainedData(ValueInput input) {
      super.readSustainedData(input);
      NBTUtils.setEnumIfPresent(input, "edit_mode", ContainerEditMode.BY_ID, mode -> this.editMode = mode);
   }

   protected void collectImplicitComponents(Builder builder) {
      super.collectImplicitComponents(builder);
      builder.set(MekanismDataComponents.EDIT_MODE, this.editMode);
   }

   protected void applyImplicitComponents(DataComponentGetter input) {
      super.applyImplicitComponents(input);
      this.editMode = (ContainerEditMode)input.getOrDefault(MekanismDataComponents.EDIT_MODE, this.editMode);
   }

   public int getRedstoneLevel() {
      return ContainerType.FLUID.getRedstoneSignalFromContainer(this.fluidTank);
   }

   protected boolean makesComparatorDirty(IContainerType<?, ?> type) {
      return type == ContainerType.FLUID;
   }

   public InteractionResult onSneakRightClick(Level level, Player player) {
      if (!level.isClientSide()) {
         this.setActive(!this.getActive());
         level.playSound(
            null,
            this.getBlockPos().getX(),
            this.getBlockPos().getY(),
            this.getBlockPos().getZ(),
            (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(),
            SoundSource.BLOCKS,
            0.3F,
            1.0F
         );
      }

      return InteractionResult.SUCCESS;
   }

   public InteractionResult onRightClick(Level level, Player player) {
      return InteractionResult.PASS;
   }

   @ComputerMethod
   public ContainerEditMode getContainerEditMode() {
      return this.editMode;
   }

   public void nextMode() {
      this.editMode = (ContainerEditMode)this.editMode.getNext();
      this.editModeChanged();
   }

   public void previousMode() {
      this.editMode = (ContainerEditMode)this.editMode.getPrevious();
      this.editModeChanged();
   }

   private void editModeChanged() {
      this.inputSlot.resetLastTransferDirection(null);
      this.markForSave();
   }

   public void parseUpgradeData(IUpgradeData upgradeData, Provider provider, TransactionContext transaction) {
      if (upgradeData instanceof FluidTankUpgradeData data) {
         this.redstone = data.redstone;
         this.inputSlot.copyContents(data.inputSlot, transaction);
         this.outputSlot.copyContents(data.outputSlot, transaction);
         this.editMode = data.editMode;
         this.fluidTank.copyContents(data.fluidTank, transaction);
         try (ScopedCollector reporter = new ScopedCollector(problemPath(), Mekanism.logger)) {
            ValueInput input = TagValueInput.create(reporter, provider, data.components);
            for (ITileComponent component : getComponents()) {
                component.read(input);
            }
         }
      } else {
         super.parseUpgradeData(upgradeData, provider, transaction);
      }
   }

   public FluidTankUpgradeData getUpgradeData(Provider provider) {
      return new FluidTankUpgradeData(
         provider, this.redstone, this.inputSlot, this.outputSlot, this.editMode, this.fluidTank, this.getComponents(), this.problemPath()
      );
   }

   public void addContainerTrackers(MekanismContainer container) {
      super.addContainerTrackers(container);
      container.track(SyncableEnum.create(ContainerEditMode.BY_ID, ContainerEditMode.BOTH, () -> this.editMode, value -> this.editMode = value));
   }

   public void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.lightUpdateDelay = input.getIntOr("delay", this.lightUpdateDelay);
   }

   public void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("delay", this.lightUpdateDelay);
   }

   public void writeReducedUpdatedTag(ValueOutput output) {
      super.writeReducedUpdatedTag(output);
      output.putFloat("scale", this.prevScale);
      NBTUtils.storeNonEmpty(output, "fluid", this.fluidTank);
      if (!this.valveJournal.fluid.isEmpty()) {
         output.store("valve", FluidResource.CODEC, this.valveJournal.fluid);
      }
   }

   public void handleUpdateTag(ValueInput input) {
      super.handleUpdateTag(input);
      float scale = input.getFloatOr("scale", this.prevScale);
      if (this.lightUpdateDelay == 0 && MekanismUtils.scaleChanged(this.prevScale, scale) && (this.prevScale == 0.0F || scale == 0.0F)) {
         this.lightUpdateDelay = this.prevScale == 0.0F ? 1 : MekanismConfig.general.blockDeactivationDelay.get();
      }

      this.prevScale = scale;
      NBTUtils.readOrEmpty(input, "fluid", this.fluidTank);
      this.valveJournal.fluid = input.read("valve", FluidResource.CODEC).orElse(FluidResource.EMPTY);
   }

   public FluidResource getValveFluid() {
      return this.valveJournal.fluid;
   }

   @ComputerMethod(requiresPublicSecurity = true)
   void setContainerEditMode(ContainerEditMode mode) throws ComputerException {
      this.validateSecurityIsPublic();
      if (this.editMode != mode) {
         this.editMode = mode;
         this.editModeChanged();
      }
   }

   @ComputerMethod(requiresPublicSecurity = true)
   void incrementContainerEditMode() throws ComputerException {
      this.validateSecurityIsPublic();
      this.nextMode();
   }

   @ComputerMethod(requiresPublicSecurity = true)
   void decrementContainerEditMode() throws ComputerException {
      this.validateSecurityIsPublic();
      this.previousMode();
   }

   private static class ValveFluidTankWrapper extends ResourceContainerWrapper<FluidResource, IFluidTank> implements IFluidTank {
      private final Supplier<TileEntityExtraFluidTank.ValveJournal> valveJournal;

      public ValveFluidTankWrapper(IFluidTank internal, Supplier<TileEntityExtraFluidTank.ValveJournal> valveJournal) {
         super(internal);
         this.valveJournal = valveJournal;
      }

      public @Range(from = 0L, to = 2147483647L) int insert(
         FluidResource resource, @Range(from = 0L, to = 2147483647L) int amount, TransactionContext transaction, AutomationType automationType
      ) {
         int inserted = super.insert(resource, amount, transaction, automationType);
         if (inserted > 0) {
            this.valveJournal.get().onTransfer(resource, transaction);
         }

         return inserted;
      }
   }

   private class ValveJournal extends SnapshotJournal<ValveJournal.ValveData> {
      private record ValveData(FluidResource valveFluid, int valve) {}
      private FluidResource fluid;
      private int valve;

      private ValveJournal() {
         super();
         this.fluid = FluidResource.EMPTY;
      }

      public void onTransfer(FluidResource resource, TransactionContext transaction) {
         if (!TileEntityExtraFluidTank.this.isRemote()) {
            this.updateSnapshots(transaction);
            this.valve = 20;
            this.fluid = resource;
         }
      }

      private boolean tick() {
         if (this.valve > 0 && --this.valve == 0) {
            this.fluid = FluidResource.EMPTY;
            return true;
         } else {
            return false;
         }
      }

      protected ValveData createSnapshot() {
         return new ValveData(this.fluid, this.valve);
      }

      protected void revertToSnapshot(ValveData snapshot) {
         this.fluid = snapshot.valveFluid;
         this.valve = snapshot.valve;
      }

      protected void onRootCommit(ValveData originalState) {
         super.onRootCommit(originalState);
         if (originalState.valve == 0 || !originalState.valveFluid().equals(this.fluid)) {
            TileEntityExtraFluidTank.this.needsPacket = true;
         }
      }
   }
}

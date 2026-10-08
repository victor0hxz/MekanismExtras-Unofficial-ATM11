package com.jerry.mekextras.common.tile;

import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.Mekanism;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.capabilities.energy.ExtraEnergyCubeEnergyContainer;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.capabilities.holder.single.SingleConfigHolder;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import com.jerry.mekextras.common.tier.ECTier;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ISlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.upgrade.EnergyCubeUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.UnknownNullability;

public class TileEntityExtraEnergyCube extends TileEntityConfigurableMachine {
   public static final ModelProperty<mekanism.common.tile.TileEntityEnergyCube.CubeSideState[]> SIDE_STATE_PROPERTY = mekanism.common.tile.TileEntityEnergyCube.SIDE_STATE_PROPERTY;
   private final ECTier tier;
   private float prevScale;
   private @UnknownNullability ExtraEnergyCubeEnergyContainer energyContainer;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
   @UnknownNullability EnergyInventorySlot chargeSlot;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
   @UnknownNullability EnergyInventorySlot dischargeSlot;

   public TileEntityExtraEnergyCube(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      ECTier tier = (ECTier)ExtraAttribute.getAdvancedTier(blockProvider, ECTier.class);
      this.tier = tier;
      super(blockProvider, pos, state, tile -> TileComponentEjector.energy(tile, () -> mekanism.api.math.MathUtils.clampToInt(tier.getOutput())));
      this.configComponent.setupIOConfig(TransmissionType.ITEM, this.chargeSlot, this.dischargeSlot, true).setCanEject(false);
      this.configComponent.setupIOConfig(TransmissionType.ENERGY, this.energyContainer);
      this.ejectorComponent.setOutputData(this.configComponent, new TransmissionType[]{TransmissionType.ENERGY}).setCanEject(var1 -> this.canFunction());
   }

   protected ISingleContainerHolder<IEnergyContainer> getInitialEnergyContainer(IContentsListener listener) {
      this.energyContainer = ExtraEnergyCubeEnergyContainer.create(this, listener);
      return SingleConfigHolder.energy(this.energyContainer, this);
   }

   protected IContainerHolder<IInventorySlot> getInitialInventory(IContentsListener listener) {
      MekContainerHelper<IInventorySlot> builder = MekContainerHelper.forSideWithItemConfig(this);
      builder.addContainer(this.dischargeSlot = EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 17, 35));
      builder.addContainer(this.chargeSlot = EnergyInventorySlot.drain(this.energyContainer, listener, 143, 35));
      this.dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
      this.chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
      return builder.build();
   }

   public ECTier getAdvanceTier() {
      return this.tier;
   }

   protected boolean onUpdateServer(ServerLevel level) {
      boolean sendUpdatePacket = super.onUpdateServer(level);
      this.chargeSlot.drainContainerIntoSlot(null);
      this.dischargeSlot.fillContainerOrConvert(null);
      float newScale = MekanismUtils.getScale(this.prevScale, this.energyContainer);
      if (MekanismUtils.scaleChanged(newScale, this.prevScale)) {
         this.prevScale = newScale;
         sendUpdatePacket = true;
      }

      return sendUpdatePacket;
   }

   public int getRedstoneLevel() {
      return MekanismUtils.redstoneLevelFromContents(this.energyContainer.getAmountAsLong(), this.energyContainer.getCapacityAsLong());
   }

   protected boolean makesComparatorDirty(IContainerType<?, ?> type) {
      return type == ContainerType.ENERGY;
   }

   public void parseUpgradeData(IUpgradeData upgradeData, Provider provider, TransactionContext transaction) {
      if (upgradeData instanceof EnergyCubeUpgradeData data) {
         this.redstone = data.redstone;
         this.setControlType(data.controlType);
         this.energyContainer.copyContents(data.energyContainer, transaction);
         this.chargeSlot.copyContents(data.chargeSlot, transaction);
         this.dischargeSlot.copyContents(data.dischargeSlot, transaction);
         try (ScopedCollector reporter = new ScopedCollector(problemPath(), Mekanism.logger)) {
            ValueInput input = TagValueInput.create(reporter, provider, data.components);
            for (ITileComponent component : getComponents()) component.read(input);
         }
      } else {
         super.parseUpgradeData(upgradeData, provider, transaction);
      }
   }

   public ExtraEnergyCubeEnergyContainer getEnergyContainerTyped() {
      return this.energyContainer;
   }

   public EnergyCubeUpgradeData getUpgradeData(Provider provider) {
      return new EnergyCubeUpgradeData(
         provider, this.redstone, this.getControlType(), this.energyContainer, this.chargeSlot, this.dischargeSlot, this.getComponents(), this.problemPath()
      );
   }

   public float getEnergyScale() {
      return this.prevScale;
   }

   public void writeReducedUpdatedTag(ValueOutput output) {
      super.writeReducedUpdatedTag(output);
      output.putFloat("scale", this.prevScale);
   }

   public void handleUpdateTag(ValueInput input) {
      ConfigInfo config = this.getConfig().getConfig(TransmissionType.ENERGY);
      DataType[] currentConfig = new DataType[EnumUtils.SIDES.length];
      if (config != null) {
         for (RelativeSide side : EnumUtils.SIDES) {
            currentConfig[side.ordinal()] = config.getDataType(side);
         }
      }

      super.handleUpdateTag(input);
      this.prevScale = input.getFloatOr("scale", this.prevScale);
      if (config != null) {
         for (RelativeSide side : EnumUtils.SIDES) {
            if (currentConfig[side.ordinal()] != config.getDataType(side)) {
               this.updateModelData();
               break;
            }
         }
      }
   }

   public ModelData getModelData() {
      ConfigInfo config = this.getConfig().getConfig(TransmissionType.ENERGY);
      if (config == null) {
         return super.getModelData();
      } else {
         mekanism.common.tile.TileEntityEnergyCube.CubeSideState[] sideStates = new mekanism.common.tile.TileEntityEnergyCube.CubeSideState[EnumUtils.SIDES.length];

         for (RelativeSide side : EnumUtils.SIDES) {
            mekanism.common.tile.TileEntityEnergyCube.CubeSideState state = mekanism.common.tile.TileEntityEnergyCube.CubeSideState.INACTIVE;
            ISlotInfo slotInfo = config.getSlotInfo(side);
            if (slotInfo != null) {
               if (slotInfo.canOutput()) {
                  state = mekanism.common.tile.TileEntityEnergyCube.CubeSideState.ACTIVE_LIT;
               } else if (slotInfo.canInput()) {
                  state = mekanism.common.tile.TileEntityEnergyCube.CubeSideState.ACTIVE_UNLIT;
               }
            }

            sideStates[side.ordinal()] = state;
         }

         return ModelData.of(SIDE_STATE_PROPERTY, sideStates);
      }
   }
}

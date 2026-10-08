package com.jerry.mekextras.common.content.network.transmitter;
import mekanism.common.content.network.transmitter.ThermodynamicConductor;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import com.jerry.mekextras.common.tier.transmitter.TCTier;

import java.util.Collection;
import java.util.UUID;
import mekanism.api.IContentsListener;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.heat.CachedAmbientTemperature;
import mekanism.common.capabilities.heat.ITileHeatHandler;
import mekanism.common.capabilities.heat.VariableHeatCapacitor;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.content.network.HeatNetwork;
import mekanism.common.lib.Color;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.lib.transmitter.acceptor.AcceptorCache;
import mekanism.common.tier.ConductorTier;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.upgrade.transmitter.ThermodynamicConductorUpgradeData;
import mekanism.common.upgrade.transmitter.TransmitterUpgradeData;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ExtraThermodynamicConductor
   extends ThermodynamicConductor
   implements ITileHeatHandler,
   IContentsListener,
   IExtraUpgradeableTransmitter<ThermodynamicConductorUpgradeData> {
   private final CachedAmbientTemperature ambientTemperature;
   public final ConductorTier tier;
   private double clientTemperature;
   public final VariableHeatCapacitor buffer;

   public ExtraThermodynamicConductor(Holder<Block> blockProvider, TileEntityTransmitter tile) {
      this.tier = (ConductorTier)Attribute.getTierNN(blockProvider, ConductorTier.class);
      super(blockProvider, tile);
      this.ambientTemperature = new CachedAmbientTemperature(this::getLevel, this::getBlockPos);
      this.clientTemperature = -1.0;
      this.buffer = VariableHeatCapacitor.create(
         TCTier.getHeatCapacity(tier), () -> TCTier.getConduction(tier), () -> TCTier.getConductionInsulation(tier), this.ambientTemperature, this
      );
   }

   @Override
   protected AcceptorCache<IHeatHandler> createAcceptorCache() {
      return new AcceptorCache(this.getTransmitterTile(), Capabilities.HEAT);
   }

   public ConductorTier getTier() {
      return this.tier;
   }

   public HeatNetwork createEmptyNetworkWithID(UUID networkID) {
      return new HeatNetwork(networkID);
   }

   public HeatNetwork createNetworkByMerging(Collection<HeatNetwork> networks) {
      return new HeatNetwork(networks);
   }

   @Override
   public void takeShare(@Nullable TransactionContext transaction) {
   }

   @Override
   protected boolean isValidAcceptor(@Nullable BlockEntity tile, Direction side) {
      return this.getAcceptorCache().getConnectedAcceptor(side) != null;
   }

   public ThermodynamicConductorUpgradeData getUpgradeData() {
      return new ThermodynamicConductorUpgradeData(this.redstoneReactive, this.getConnectionTypesRaw(), this.buffer.getHeat());
   }

   @Override
   public boolean dataTypeMatches(TransmitterUpgradeData data) {
      return data instanceof ThermodynamicConductorUpgradeData;
   }

   @Override public void parseUpgradeData(ThermodynamicConductorUpgradeData data) { parseUpgradeData(data, null); }

   public void parseUpgradeData(ThermodynamicConductorUpgradeData data, TransactionContext transaction) {
      this.redstoneReactive = data.redstoneReactive;
      this.setConnectionTypesRaw(data.connectionTypes);
      this.buffer.setHeat(data.heat, transaction);
   }

   @Override
   public void write(ValueOutput output) {
      super.write(output);
      // The Version Locked 2.1 CapacitorState codec writes heat into both fields.
      // Write our conductor's capacity explicitly so a reload preserves temperature.
      ValueOutput heatState = output.child("heat_capacitor").child("state");
      heatState.putDouble("heat", this.buffer.getHeat());
      heatState.putDouble("heat_capacity", this.buffer.getHeatCapacity());
   }

   @Override
   public void read(ValueInput input) {
      super.read(input);
      ContainerType.HEAT.readFrom(input, this.buffer);
      // Repair capacities written by the old codec, using the conductor's configured tier.
      if (this.buffer.getHeatCapacity() == this.buffer.getHeat()) {
         this.buffer.setHeatAndCapacity(this.buffer.getHeat(), TCTier.getHeatCapacity(this.tier), null);
      }
   }

   @Override
   public void writeReducedUpdatedTag(ValueOutput output) {
      super.writeReducedUpdatedTag(output);
      output.putDouble("heat", this.buffer.getHeat());
   }

   @Override
   public boolean handleUpdateTag(ValueInput input) {
      boolean refreshModelData = super.handleUpdateTag(input);
      this.buffer.setHeat(input.getDoubleOr("heat", this.buffer.getHeat()), null);
      return refreshModelData;
   }

   public Color getBaseColor() {
      return this.tier.getBaseColor();
   }

   public IHeatCapacitor getHeatCapacitor(@Nullable Direction side) {
      return this.buffer;
   }

   public double getTemperature() {
      return this.buffer.getTemperature();
   }

   public void onContentsChanged() {
      // Loading saved heat commits before the block entity is attached to its level.
      if (this.getLevel() == null) {
         return;
      }
      if (!this.isRemote()) {
         if (this.clientTemperature == -1.0) {
            this.clientTemperature = this.ambientTemperature.getAsDouble();
         }

         if (Math.abs(this.buffer.getTemperature() - this.clientTemperature) > this.buffer.getTemperature() / 20.0) {
            this.clientTemperature = this.buffer.getTemperature();
            this.getTransmitterTile().sendUpdatePacket();
         }
      }

      this.getTransmitterTile().setChanged();
   }

   public double getAmbientTemperature(Direction side) {
      return this.ambientTemperature.getTemperature(side);
   }

   public @Nullable IHeatHandler getAdjacent(Direction side) {
      return connectionMapContainsSide(this.getAllCurrentConnections(), side) ? (IHeatHandler)this.getAcceptorCache().getConnectedAcceptor(side) : null;
   }

   public boolean countsAsAdjacent(Direction side) {
      return !this.hasTransmitterNetwork() || this.getTransmitterNetworkNN().getTransmitter(this.getBlockPos().relative(side)) != null;
   }
}

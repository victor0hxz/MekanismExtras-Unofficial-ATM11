package com.jerry.mekextras.common.tile.transmitter;

import java.util.Objects;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.MathUtils;
import com.jerry.mekextras.api.tier.AdvancedTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.capabilities.resolver.manager.EnergyHandlerManager;
import mekanism.common.content.network.EnergyNetwork;
import com.jerry.mekextras.common.content.network.transmitter.ExtraUniversalCable;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.lib.transmitter.ConnectionType;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraUniversalCable extends TileEntityExtraTransmitter implements IComputerTile {
   public TileEntityExtraUniversalCable(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state);
      this.addCapabilityResolver(
         new EnergyHandlerManager(
            new ISingleContainerHolder<IEnergyContainer>() {
               {
                  Objects.requireNonNull(TileEntityExtraUniversalCable.this);
               }

               public @Nullable IEnergyContainer getContainer(@Nullable Direction direction) {
                  ExtraUniversalCable cable = TileEntityExtraUniversalCable.this.getTransmitter();
                  return (direction == null || cable.getConnectionTypeRaw(direction) != ConnectionType.NONE) && !cable.isRedstoneActivated()
                     ? cable.getContainer()
                     : null;
               }

               public boolean canInsert(@Nullable Direction direction) {
                  return TileEntityExtraUniversalCable.this.canInsert(direction);
               }

               public boolean canExtract(@Nullable Direction direction) {
                  return TileEntityExtraUniversalCable.this.canExtract(direction);
               }
            },
            this::getGameTime
         )
      );
   }

   protected ExtraUniversalCable createTransmitter(Holder<Block> blockProvider) {
      return new ExtraUniversalCable(blockProvider, this);
   }

   public ExtraUniversalCable getTransmitter() {
      return (ExtraUniversalCable)super.getTransmitter();
   }

   @Override
   protected void onUpdateServer(ServerLevel level) {
      this.getTransmitter().pullFromAcceptors(level);
      super.onUpdateServer(level);
   }

   @Override
   public TransmitterType getTransmitterType() {
      return TransmitterType.UNIVERSAL_CABLE;
   }

   @Override
   protected BlockState upgradeResult(BlockState current, AdvancedTier tier) {
      return BlockStateHelper.copyStateData(current, switch (tier) {
         case ABSOLUTE -> ExtraBlocks.ABSOLUTE_UNIVERSAL_CABLE;
         case SUPREME -> ExtraBlocks.SUPREME_UNIVERSAL_CABLE;
         case COSMIC -> ExtraBlocks.COSMIC_UNIVERSAL_CABLE;
         case INFINITE -> ExtraBlocks.INFINITE_UNIVERSAL_CABLE;
         default -> null;
      });
   }

   protected void writeUpdatedTag(ValueOutput output) {
      super.writeUpdatedTag(output);
      if (this.getTransmitter().hasTransmitterNetwork()) {
         EnergyNetwork network = this.getTransmitter().getTransmitterNetworkNN();
         output.putLong("energy", network.energyContainer.getAmountAsLong());
         output.putFloat("scale", network.currentScale);
      }
   }

   @Override
   public void sideChanged(Direction side, ConnectionType old, ConnectionType type) {
      super.sideChanged(side, old, type);
      if (type == ConnectionType.NONE) {
         this.invalidateCapability(Capabilities.ENERGY.block(), side);
      } else if (old == ConnectionType.NONE) {
         this.invalidateCapabilities();
      }
   }

   @Override
   public void redstoneChanged(boolean powered) {
      super.redstoneChanged(powered);
      if (powered) {
         this.invalidateCapabilityAll(Capabilities.ENERGY.block());
      } else {
         this.invalidateCapabilities();
      }
   }

   public String getComputerName() {
      return this.getTransmitter().getTier().getBaseTier().getLowerName() + "ExtraUniversalCable";
   }

   @ComputerMethod
   long getBuffer() {
      return this.getTransmitter().getBufferWithFallback();
   }

   @ComputerMethod
   long getCapacity() {
      ExtraUniversalCable cable = this.getTransmitter();
      return cable.hasTransmitterNetwork() ? cable.getTransmitterNetworkNN().getCapacity() : cable.getCapacity();
   }

   @ComputerMethod
   long getNeeded() {
      return this.getCapacity() - this.getBuffer();
   }

   @ComputerMethod
   double getFilledPercentage() {
      return MathUtils.divideToLevel(this.getBuffer(), this.getCapacity());
   }
}

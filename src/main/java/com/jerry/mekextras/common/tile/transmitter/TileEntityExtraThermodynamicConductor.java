package com.jerry.mekextras.common.tile.transmitter;

import com.jerry.mekextras.api.tier.AdvancedTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.resolver.manager.HeatHandlerManager;
import com.jerry.mekextras.common.content.network.transmitter.ExtraThermodynamicConductor;
import mekanism.common.lib.transmitter.ConnectionType;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityExtraThermodynamicConductor extends TileEntityExtraTransmitter {
   public TileEntityExtraThermodynamicConductor(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state);
      this.addCapabilityResolver(
         new HeatHandlerManager(
            direction -> {
               ExtraThermodynamicConductor conductor = this.getTransmitter();
               return (direction == null || conductor.getConnectionTypeRaw(direction) != ConnectionType.NONE) && !conductor.isRedstoneActivated()
                  ? conductor.getHeatCapacitor(direction)
                  : null;
            }
         )
      );
   }

   protected ExtraThermodynamicConductor createTransmitter(Holder<Block> blockProvider) {
      return new ExtraThermodynamicConductor(blockProvider, this);
   }

   public ExtraThermodynamicConductor getTransmitter() {
      return (ExtraThermodynamicConductor)super.getTransmitter();
   }

   @Override
   public TransmitterType getTransmitterType() {
      return TransmitterType.THERMODYNAMIC_CONDUCTOR;
   }

   @Override
   protected BlockState upgradeResult(BlockState current, AdvancedTier tier) {
      return BlockStateHelper.copyStateData(current, switch (tier) {
         case ABSOLUTE -> ExtraBlocks.ABSOLUTE_THERMODYNAMIC_CONDUCTOR;
         case SUPREME -> ExtraBlocks.SUPREME_THERMODYNAMIC_CONDUCTOR;
         case COSMIC -> ExtraBlocks.COSMIC_THERMODYNAMIC_CONDUCTOR;
         case INFINITE -> ExtraBlocks.INFINITE_THERMODYNAMIC_CONDUCTOR;
         default -> null;
      });
   }

   @Override
   public void sideChanged(Direction side, ConnectionType old, ConnectionType type) {
      super.sideChanged(side, old, type);
      if (type == ConnectionType.NONE) {
         this.invalidateCapability(Capabilities.HEAT, side);
      } else if (old == ConnectionType.NONE) {
         this.invalidateCapabilities();
      }
   }

   @Override
   public void redstoneChanged(boolean powered) {
      super.redstoneChanged(powered);
      if (powered) {
         this.invalidateCapabilityAll(Capabilities.HEAT);
      } else {
         this.invalidateCapabilities();
      }
   }
}

package com.jerry.mekextras.common.tile.transmitter;

import com.mojang.serialization.Codec;
import mekanism.api.fluid.IFluidTank;
import com.jerry.mekextras.api.tier.AdvancedTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.network.FluidNetwork;
import mekanism.common.content.network.transmitter.MechanicalPipe;
import com.jerry.mekextras.common.content.network.transmitter.ExtraMechanicalPipe;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class TileEntityExtraMechanicalPipe extends TileEntityExtraResourceTransmitter<FluidResource, IFluidTank, FluidNetwork, MechanicalPipe> {
   public TileEntityExtraMechanicalPipe(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state, Capabilities.FLUID);
   }

   protected MechanicalPipe createTransmitter(Holder<Block> blockProvider) {
      return new ExtraMechanicalPipe(blockProvider, this);
   }

   @Override
   public TransmitterType getTransmitterType() {
      return TransmitterType.MECHANICAL_PIPE;
   }

   @Override
   protected Codec<FluidResource> resourceCodec() {
      return FluidResource.CODEC;
   }

   @Override
   protected BlockState upgradeResult(BlockState current, AdvancedTier tier) {
      return BlockStateHelper.copyStateData(current, switch (tier) {
         case ABSOLUTE -> ExtraBlocks.ABSOLUTE_MECHANICAL_PIPE;
         case SUPREME -> ExtraBlocks.SUPREME_MECHANICAL_PIPE;
         case COSMIC -> ExtraBlocks.COSMIC_MECHANICAL_PIPE;
         case INFINITE -> ExtraBlocks.INFINITE_MECHANICAL_PIPE;
         default -> null;
      });
   }

   @Override public ExtraMechanicalPipe getTransmitter() { return (ExtraMechanicalPipe)super.getTransmitter(); }

   public String getComputerName() {
      return this.getTransmitter().getTier().getBaseTier().getLowerName() + "MechanicalPipe";
   }
}

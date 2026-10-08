package com.jerry.mekextras.common.tile.transmitter;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mekanism.api.resource.IResourceContainer;
import mekanism.api.resource.LargeResourceStack;
import mekanism.common.capabilities.MultiTypeCapability;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.resolver.manager.ResourceHandlerManager;
import mekanism.common.content.network.transmitter.BufferedResourceTransmitter;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.lib.transmitter.DynamicBufferedResourceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import org.jspecify.annotations.Nullable;

public abstract class TileEntityExtraResourceTransmitter<RESOURCE extends Resource, CONTAINER extends IResourceContainer<RESOURCE>, NETWORK extends DynamicBufferedResourceNetwork<RESOURCE, CONTAINER, NETWORK, TRANSMITTER>, TRANSMITTER extends BufferedResourceTransmitter<RESOURCE, CONTAINER, NETWORK, TRANSMITTER>>
   extends TileEntityExtraTransmitter
   implements IComputerTile {
   private final BlockCapability<ResourceHandler<RESOURCE>, @Nullable Direction> capability;

   public TileEntityExtraResourceTransmitter(Holder<Block> blockProvider, BlockPos pos, BlockState state, MultiTypeCapability<ResourceHandler<RESOURCE>> capability) {
      super(blockProvider, pos, state);
      this.capability = capability.block();
      this.addCapabilityResolver(
         new ResourceHandlerManager(
            capability,
            new IContainerHolder<CONTAINER>() {
               {
                  Objects.requireNonNull(TileEntityExtraResourceTransmitter.this);
               }

               public List<CONTAINER> getContainers(@Nullable Direction direction) {
                  TRANSMITTER transmitter = (TRANSMITTER)TileEntityExtraResourceTransmitter.this.getTransmitter();
                  return (direction == null || transmitter.getConnectionTypeRaw(direction) != ConnectionType.NONE) && !transmitter.isRedstoneActivated()
                     ? transmitter.getContainers()
                     : Collections.emptyList();
               }

               public boolean canInsert(@Nullable Direction direction) {
                  return TileEntityExtraResourceTransmitter.this.canInsert(direction);
               }

               public boolean canExtract(@Nullable Direction direction) {
                  return TileEntityExtraResourceTransmitter.this.canExtract(direction);
               }
            }
         )
      );
   }

   protected abstract TRANSMITTER createTransmitter(Holder<Block> blockProvider);

   public TRANSMITTER getTransmitter() {
      return (TRANSMITTER)super.getTransmitter();
   }

   protected abstract Codec<RESOURCE> resourceCodec();

   @Override
   protected void onUpdateServer(ServerLevel level) {
      this.getTransmitter().pullFromAcceptors(level);
      super.onUpdateServer(level);
   }

   @Override
   public void sideChanged(Direction side, ConnectionType old, ConnectionType type) {
      super.sideChanged(side, old, type);
      if (type == ConnectionType.NONE) {
         this.invalidateCapability(this.capability, side);
      } else if (old == ConnectionType.NONE) {
         this.invalidateCapabilities();
      }
   }

   @Override
   public void redstoneChanged(boolean powered) {
      super.redstoneChanged(powered);
      if (powered) {
         this.invalidateCapabilityAll(this.capability);
      } else {
         this.invalidateCapabilities();
      }
   }

   protected void writeUpdatedTag(ValueOutput output) {
      super.writeUpdatedTag(output);
      if (this.getTransmitter().hasTransmitterNetwork()) {
         NETWORK network = this.getTransmitter().getTransmitterNetworkNN();
         if (!network.getLastType().isEmpty()) {
            output.store("stored", this.resourceCodec(), network.getLastType());
         }

         output.putFloat("scale", network.currentScale);
      }
   }

   @ComputerMethod
   public LargeResourceStack<RESOURCE> getBuffer() {
      return this.getTransmitter().getBufferWithFallback();
   }

   @ComputerMethod
   public long getCapacity() {
      BufferedResourceTransmitter<RESOURCE, CONTAINER, ?, ?> transmitter = this.getTransmitter();
      return transmitter.hasTransmitterNetwork() ? transmitter.getTransmitterNetworkNN().getCapacity() : transmitter.getCapacity();
   }

   @ComputerMethod
   public long getNeeded() {
      return this.getCapacity() - this.getBuffer().amount();
   }

   @ComputerMethod
   public double getFilledPercentage() {
      return (double)this.getBuffer().amount() / this.getCapacity();
   }
}

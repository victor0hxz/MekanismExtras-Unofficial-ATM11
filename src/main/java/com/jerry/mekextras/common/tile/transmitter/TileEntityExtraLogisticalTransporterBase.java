package com.jerry.mekextras.common.tile.transmitter;

import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.item.TransporterCapabilityResolver;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.lib.transaction.TransactionHelper;
import mekanism.common.lib.transmitter.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public abstract class TileEntityExtraLogisticalTransporterBase extends TileEntityExtraTransmitter {
   protected TileEntityExtraLogisticalTransporterBase(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      super(blockProvider, pos, state);
      this.addCapabilityResolver(new TransporterCapabilityResolver(this.getTransmitter()));
   }

   protected abstract LogisticalTransporterBase createTransmitter(Holder<Block> blockProvider);

   public LogisticalTransporterBase getTransmitter() {
      return (LogisticalTransporterBase)super.getTransmitter();
   }

   public static void tickClient(Level level, BlockPos pos, BlockState state, TileEntityExtraLogisticalTransporterBase transmitter) {
      transmitter.getTransmitter().onUpdateClient();
   }

   @Override
   public void onUpdateServer(ServerLevel level) {
      super.onUpdateServer(level);
      this.getTransmitter().onUpdateServer(level);
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      super.preRemoveSideEffects(pos, state);
      if (this.level != null && !this.level.isClientSide()) {
         LogisticalTransporterBase transporter = this.getTransmitter();
         if (!transporter.isUpgrading()) {
            Transaction transaction = TransactionHelper.openTransactionSafe();

            try {
               for (TransporterStack stack : transporter.getTransit()) {
                  transporter.drop(this.level, stack, transaction);
               }

               transaction.commit();
            } catch (Throwable var8) {
               if (transaction != null) {
                  try {
                     transaction.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (transaction != null) {
               transaction.close();
            }
         }
      }
   }

   @Override
   public void sideChanged(Direction side, ConnectionType old, ConnectionType type) {
      super.sideChanged(side, old, type);
      if ((type != ConnectionType.NONE || old == ConnectionType.PUSH) && (type != ConnectionType.PUSH || old == ConnectionType.NONE)) {
         if (old == ConnectionType.NONE && type != ConnectionType.PUSH || old == ConnectionType.PUSH && type != ConnectionType.NONE) {
            this.invalidateCapabilities();
         }
      } else {
         this.invalidateCapability(Capabilities.ITEM.block(), side);
      }
   }
}

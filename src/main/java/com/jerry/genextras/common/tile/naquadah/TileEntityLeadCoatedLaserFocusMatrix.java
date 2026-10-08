package com.jerry.genextras.common.tile.naquadah;

import mekanism.api.lasers.ILaserReceptor;
import mekanism.common.lib.transaction.TransactionHelper;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.registries.GenExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class TileEntityLeadCoatedLaserFocusMatrix extends TileEntityNaquadahReactorCasing implements ILaserReceptor {
   public TileEntityLeadCoatedLaserFocusMatrix(BlockPos pos, BlockState state) {
      super(GenExtraBlocks.LEAD_COATED_LASER_FOCUS_MATRIX, pos, state);
   }

   public int receiveLaserEnergy(int energy, TransactionContext transaction) {
      NaquadahReactorMultiblockData multiblock = (NaquadahReactorMultiblockData)this.getMultiblock();
      if (multiblock.isFormed()) {
         multiblock.addTemperatureFromEnergyInput(energy, transaction);
         return energy;
      } else {
         return 0;
      }
   }

   public InteractionResult onRightClick(Level level, Player player) {
      if (!level.isClientSide() && player.isCreative()) {
         NaquadahReactorMultiblockData multiblock = (NaquadahReactorMultiblockData)this.getMultiblock();
         if (multiblock.isFormed()) {
            Transaction transaction = TransactionHelper.openTransactionSafe();

            try {
               multiblock.setPlasmaTemp(1.0E9, transaction);
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

            return InteractionResult.SUCCESS_SERVER;
         }
      }

      return super.onRightClick(level, player);
   }

   public boolean canLasersDig() {
      return false;
   }
}

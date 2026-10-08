package com.jerry.mekextras.common.tile.multiblock;

import mekanism.api.IContentsListener;
import mekanism.api.energy.IEnergyContainer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import com.jerry.mekextras.common.tier.ICTier;
import mekanism.common.tile.prefab.TileEntityInternalMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.UnknownNullability;

public class TileEntityExtraInductionCell extends TileEntityInternalMultiblock {
   private @UnknownNullability MachineEnergyContainer<TileEntityExtraInductionCell> energyContainer;
   public final ICTier tier;

   public TileEntityExtraInductionCell(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      this.tier = (ICTier)ExtraAttribute.getAdvancedTier(blockProvider, ICTier.class);
      super(blockProvider, pos, state);
   }

   protected ISingleContainerHolder<IEnergyContainer> getInitialEnergyContainer(IContentsListener listener) {
      this.energyContainer = MachineEnergyContainer.internal(this, listener);
      return var1 -> this.energyContainer;
   }

   public MachineEnergyContainer<TileEntityExtraInductionCell> energyContainer() {
      return this.energyContainer;
   }
}

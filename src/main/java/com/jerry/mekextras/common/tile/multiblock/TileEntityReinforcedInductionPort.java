package com.jerry.mekextras.common.tile.multiblock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.IContentsListener;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.capabilities.holder.single.ProxiedSingleContainerHolder;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import com.jerry.mekextras.common.content.matrix.ReinforcedMatrixMultiblockData;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.lib.multiblock.MultiblockData.CapabilityOutputTarget;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import mekanism.common.util.text.BooleanStateDisplay.InputOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

public class TileEntityReinforcedInductionPort extends TileEntityReinforcedInductionCasing {
   private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> energyCapabilityCaches = new EnumMap<>(Direction.class);

   public TileEntityReinforcedInductionPort(BlockPos pos, BlockState state) {
      super(ExtraBlocks.REINFORCED_INDUCTION_PORT, pos, state);
      this.delaySupplier = NO_DELAY;
   }

   protected ISingleContainerHolder<IEnergyContainer> getInitialEnergyContainer(IContentsListener listener) {
      return ProxiedSingleContainerHolder.energy(
         var1 -> !this.getActive(), var1 -> this.getActive(), var1 -> ((ReinforcedMatrixMultiblockData)this.getMultiblock()).getEnergyContainer()
      );
   }

   public boolean persists(IContainerType<?, ?> type) {
      return type == ContainerType.ENERGY ? false : super.persists(type);
   }

   public void addEnergyTargetCapability(List<CapabilityOutputTarget<EnergyHandler>> outputTargets, Direction side) {
      BlockCapabilityCache<EnergyHandler, Direction> cache = this.energyCapabilityCaches.get(side);
      if (cache == null) {
         cache = Capabilities.ENERGY.createCache((ServerLevel)this.level, this.worldPosition.relative(side), side.getOpposite());
         this.energyCapabilityCaches.put(side, cache);
      }

      outputTargets.add(new CapabilityOutputTarget(cache, this::getActive));
   }

   public InteractionResult onSneakRightClick(Level level, Player player) {
      if (!level.isClientSide()) {
         boolean oldMode = this.getActive();
         this.setActive(!oldMode);
         player.sendOverlayMessage(MekanismLang.INDUCTION_PORT_MODE.translateColored(EnumColor.GRAY, new Object[]{InputOutput.of(oldMode, true)}));
      }

      return InteractionResult.SUCCESS;
   }

   public int getRedstoneLevel() {
      return ((ReinforcedMatrixMultiblockData)this.getMultiblock()).getCurrentRedstoneLevel();
   }

   @ComputerMethod(methodDescription = "true -> output, false -> input.")
   boolean getMode() {
      return this.getActive();
   }

   @ComputerMethod(methodDescription = "true -> output, false -> input")
   void setMode(boolean output) {
      this.setActive(output);
   }
}

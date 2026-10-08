package com.jerry.genextras.common.tile.naquadah;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IFluidTank;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.text.EnumColor;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.heat.CachedAmbientTemperature;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.lib.multiblock.MultiblockData.CapabilityOutputTarget;
import mekanism.common.util.text.BooleanStateDisplay.InputOutput;
import mekanism.generators.common.GeneratorsLang;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.registries.GenExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

public class TileEntityNaquadahReactorPort extends TileEntityNaquadahReactorCasing {
   private final Map<Direction, BlockCapabilityCache<ResourceHandler<ChemicalResource>, @Nullable Direction>> chemicalCapabilityCaches = new EnumMap<>(
      Direction.class
   );
   private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> energyCapabilityCaches = new EnumMap<>(Direction.class);

   public TileEntityNaquadahReactorPort(BlockPos pos, BlockState state) {
      super(GenExtraBlocks.NAQUADAH_REACTOR_PORT, pos, state);
      this.delaySupplier = NO_DELAY;
   }

   public IContainerHolder<IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
      return var1 -> ((NaquadahReactorMultiblockData)this.getMultiblock()).getChemicalTanks();
   }

   protected IContainerHolder<IFluidTank> getInitialFluidTanks(IContentsListener listener) {
      return var1 -> ((NaquadahReactorMultiblockData)this.getMultiblock()).getFluidTanks();
   }

   protected ISingleContainerHolder<IEnergyContainer> getInitialEnergyContainer(IContentsListener listener) {
      return var1 -> ((NaquadahReactorMultiblockData)this.getMultiblock()).getEnergyContainer();
   }

   protected ISingleContainerHolder<IHeatCapacitor> getInitialHeatCapacitor(IContentsListener listener, CachedAmbientTemperature ambientTemperature) {
      return var1 -> ((NaquadahReactorMultiblockData)this.getMultiblock()).getHeatCapacitor();
   }

   public boolean persists(IContainerType<?, ?> type) {
      return type != ContainerType.CHEMICAL && type != ContainerType.FLUID && type != ContainerType.ENERGY && type != ContainerType.HEAT
         ? super.persists(type)
         : false;
   }

   public void addChemicalTargetCapability(List<CapabilityOutputTarget<ResourceHandler<ChemicalResource>>> outputTargets, Direction side) {
      BlockCapabilityCache<ResourceHandler<ChemicalResource>, Direction> cache = this.chemicalCapabilityCaches.get(side);
      if (cache == null) {
         cache = Capabilities.CHEMICAL.createCache((ServerLevel)this.level, this.worldPosition.relative(side), side.getOpposite());
         this.chemicalCapabilityCaches.put(side, cache);
      }

      outputTargets.add(new CapabilityOutputTarget(cache, this::getActive));
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
         player.sendOverlayMessage(GeneratorsLang.REACTOR_PORT_EJECT.translateColored(EnumColor.GRAY, new Object[]{InputOutput.of(oldMode, true)}));
      }

      return InteractionResult.SUCCESS;
   }

   public int getRedstoneLevel() {
      return ((NaquadahReactorMultiblockData)this.getMultiblock()).getCurrentRedstoneLevel();
   }

   public boolean exposesMultiblockToComputer() {
      return false;
   }

   @ComputerMethod(methodDescription = "true -> output, false -> input")
   boolean getMode() {
      return this.getActive();
   }

   @ComputerMethod(methodDescription = "true -> output, false -> input")
   void setMode(boolean output) {
      this.setActive(output);
   }
}

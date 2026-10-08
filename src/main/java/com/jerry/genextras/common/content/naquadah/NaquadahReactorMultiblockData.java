package com.jerry.genextras.common.content.naquadah;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IFluidTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.HeatAPI.HeatTransfer;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.chemical.VariableCapacityChemicalTank;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.capabilities.fluid.VariableCapacityFluidTank;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import mekanism.common.capabilities.heat.ITileHeatHandler;
import mekanism.common.capabilities.heat.VariableHeatCapacitor;
import mekanism.common.capabilities.proxy.AutomatedResourceHandler;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerFluidTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.SyntheticComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.lib.multiblock.IValveHandler.ValveData;
import mekanism.common.lib.multiblock.MultiblockData.CapabilityOutputTarget;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.tile.prefab.TileEntityStructuralMultiblock;
import mekanism.common.util.EnergyUtils;
import mekanism.common.util.HeatUtils;
import mekanism.common.util.ResourceUtils;
import mekanism.common.util.WorldUtils;
import com.jerry.genextras.common.GeneratorExtraTags.Chemicals;
import com.jerry.genextras.common.config.GeneratorsExtraConfig;
import mekanism.generators.common.registries.GeneratorsChemicals;
import mekanism.generators.common.registries.GeneratorsDamageTypes;
import com.jerry.genextras.common.registries.GenExtraItems;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorCasing;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class NaquadahReactorMultiblockData extends MultiblockData {
   public static final String HEAT_TAB = "heat";
   public static final String FUEL_TAB = "fuel";
   public static final String STATS_TAB = "stats";
   public static final int MAX_INJECTION = 98;
   public static final double BURN_TEMPERATURE = 4.0E8;
   private static final double BURN_RATIO = 1.0;
   private static final long PLASMA_HEAT_CAPACITY = 100L;
   private static final double CASE_HEAT_CAPACITY = 1.0;
   private static final double INVERSE_INSULATION = 100000.0;
   private static final double PLASMA_CASE_CONDUCTIVITY = 0.2;
   private final List<CapabilityOutputTarget<EnergyHandler>> energyOutputTargets = new ArrayList<>();
   private final List<CapabilityOutputTarget<ResourceHandler<ChemicalResource>>> chemicalOutputTargets = new ArrayList<>();
   private final Set<ITileHeatHandler> heatHandlers = new ObjectOpenHashSet();
   @ContainerSync
   private boolean burning = false;
   @ContainerSync
   public final IEnergyContainer energyContainer;
   final BasicHeatCapacitor heatCapacitor;
   @ContainerSync(tags = "heat")
   @WrappingComputerMethod(
      wrapper = ComputerFluidTankWrapper.class,
      methodNames = {"getWater", "getWaterCapacity", "getWaterNeeded", "getWaterFilledPercentage"},
      docPlaceholder = "water tank"
   )
   public IFluidTank waterTank;
   @ContainerSync(tags = "heat")
   @WrappingComputerMethod(
      wrapper = ComputerChemicalTankWrapper.class,
      methodNames = {"getSteam", "getSteamCapacity", "getSteamNeeded", "getSteamFilledPercentage"},
      docPlaceholder = "steam tank"
   )
   public IChemicalTank steamTank;
   private double biomeAmbientTemp;
   @ContainerSync(tags = "heat")
   private double lastPlasmaTemperature;
   @ContainerSync
   private double lastCaseTemperature;
   @ContainerSync
   @SyntheticComputerMethod(getter = "getEnvironmentalLoss")
   public double lastEnvironmentLoss;
   @ContainerSync
   @SyntheticComputerMethod(getter = "getTransferLoss")
   public double lastTransferLoss;
   @ContainerSync(tags = "fuel")
   @WrappingComputerMethod(
      wrapper = ComputerChemicalTankWrapper.class,
      methodNames = {"getNaquadah", "getNaquadahCapacity", "getNaquadahNeeded", "getNaquadahFilledPercentage"},
      docPlaceholder = "naquadah tank"
   )
   public IChemicalTank naquadahTank;
   @ContainerSync(tags = "fuel")
   @WrappingComputerMethod(
      wrapper = ComputerChemicalTankWrapper.class,
      methodNames = {"getUranium", "getUraniumCapacity", "getUraniumNeeded", "getUraniumFilledPercentage"},
      docPlaceholder = "uranium tank"
   )
   public IChemicalTank uraniumTank;
   @ContainerSync(tags = "fuel")
   @WrappingComputerMethod(
      wrapper = ComputerChemicalTankWrapper.class,
      methodNames = {"getNqUFuel", "getNqUFuelCapacity", "getNqUFuelNeeded", "getNqUFuelFilledPercentage"},
      docPlaceholder = "fuel tank"
   )
   public IChemicalTank fuelTank;
   @ContainerSync(tags = {"fuel", "heat", "stats"}, getter = "getInjectionRate")
   private int injectionRate = 2;
   @ContainerSync(tags = {"fuel", "heat", "stats"})
   private int lastBurned;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getHohlraum", docPlaceholder = "Hohlraum slot")
   final BasicInventorySlot reactorSlot;
   private final NaquadahReactorMultiblockData.PlasmaJournal plasmaJournal = new NaquadahReactorMultiblockData.PlasmaJournal();
   private boolean clientBurning;
   private double clientTemp;
   private long maxWater;
   private long maxSteam;
   private @Nullable AABB deathZone;

   public NaquadahReactorMultiblockData(TileEntityNaquadahReactorCasing tile) {
      super(tile);
      this.biomeAmbientTemp = HeatAPI.getAmbientTemp(tile.getLevel(), tile.getBlockPos());
      this.lastPlasmaTemperature = this.biomeAmbientTemp;
      this.lastCaseTemperature = this.biomeAmbientTemp;
      this.plasmaJournal.temperature = this.biomeAmbientTemp;
      this.chemicalTanks
         .add(
            this.naquadahTank = VariableCapacityChemicalTank.input(
               this, GeneratorsExtraConfig.extraGenerators.reactorFuelCapacity, chemical -> chemical.is(Chemicals.RICH_NAQUADAH_FUEL), this
            )
         );
      this.chemicalTanks
         .add(
            this.uraniumTank = VariableCapacityChemicalTank.input(
               this, GeneratorsExtraConfig.extraGenerators.reactorFuelCapacity, chemical -> chemical.is(Chemicals.RICH_URANIUM_FUEL), this
            )
         );
      this.chemicalTanks
         .add(
            this.fuelTank = VariableCapacityChemicalTank.input(
               this, GeneratorsExtraConfig.extraGenerators.reactorFuelCapacity, chemical -> chemical.is(Chemicals.NAQUADAH_URANIUM_FUEL), this.createSaveAndComparator()
            )
         );
      this.chemicalTanks
         .add(this.steamTank = VariableCapacityChemicalTank.output(this, this::getMaxSteam, chemical -> chemical.is(com.jerry.genextras.common.registries.GenExtraChemicals.POLONIUM_CONTAINING_STEAM), this));
      this.fluidTanks.add(this.waterTank = VariableCapacityFluidTank.input(this, this::getMaxWater, fluid -> fluid.is(FluidTags.WATER), this));
      this.energyContainer = VariableCapacityEnergyContainer.output(GeneratorsExtraConfig.extraGenerators.reactorEnergyCapacity, this);
      this.heatCapacitor = VariableHeatCapacitor.create(
         1.0, NaquadahReactorMultiblockData::getInverseConductionCoefficient, () -> 100000.0, () -> this.biomeAmbientTemp, this
      );
      this.inventorySlots
         .add(
            this.reactorSlot = BasicInventorySlot.at(
               ConstantPredicates.notExternal(), ConstantPredicates.alwaysTrueBi(), GenExtraItems.NAQUADAH_HOHLRAUM::is, this, 85, 39
            )
         );
   }

   public IEnergyContainer energyContainer() {
      return this.energyContainer;
   }

   protected IHeatCapacitor heatCapacitor() {
      return this.heatCapacitor;
   }

   public void onCreated(Level world, TransactionContext transaction) {
      super.onCreated(world, transaction);
      this.biomeAmbientTemp = this.calculateAverageAmbientTemperature(world);
      this.deathZone = AABB.encapsulatingFullBlocks(this.getMinPos().offset(1, 1, 1), this.getMaxPos().offset(-1, -1, -1));
   }

   public boolean allowsStructuralGuiAccess(TileEntityStructuralMultiblock multiblock) {
      return false;
   }

   public void readUpdateTag(ValueInput input) {
      super.readUpdateTag(input);
      this.lastPlasmaTemperature = input.getDoubleOr("plasma_temp", this.getPlasmaTemp());
      this.setBurning(input.getBooleanOr("burning", this.isBurning()));
   }

   public void writeUpdateTag(ValueOutput output) {
      super.writeUpdateTag(output);
      output.putDouble("plasma_temp", this.getLastPlasmaTemp());
      output.putBoolean("burning", this.isBurning());
   }

   public void addTemperatureFromEnergyInput(long energyAdded, TransactionContext transaction) {
      if (energyAdded > 0L) {
         this.plasmaJournal.updateSnapshots(transaction);
         if (this.isBurning()) {
            this.plasmaJournal.temperature += energyAdded / 100.0;
         } else {
            this.plasmaJournal.temperature += energyAdded / 100.0 * 10.0;
         }
      }
   }

   public boolean tick(ServerLevel world) {
      boolean needsPacket = super.tick(world);
      int fuelBurned = 0;
      Transaction transaction = Transaction.openRoot();

      try {
         if (this.getPlasmaTemp() >= 4.0E8) {
            if (!this.isBurning()) {
               this.vaporiseHohlraum(transaction);
            }

            if (this.isBurning()) {
               this.injectFuel(transaction);
               fuelBurned = this.burnFuel(transaction);
            }

            if (fuelBurned == 0) {
               this.setBurning(false);
            }
         } else {
            this.setBurning(false);
         }

         if (this.lastBurned != fuelBurned) {
            this.lastBurned = fuelBurned;
         }

         this.transferHeat(transaction);
         if (!this.energyOutputTargets.isEmpty() && !this.energyContainer.isEmpty()) {
            EnergyUtils.emit(this.getActiveOutputs(this.energyOutputTargets), this.energyContainer, transaction);
         }

         if (!this.chemicalOutputTargets.isEmpty() && !this.steamTank.isEmpty()) {
            ResourceUtils.emit(this.getActiveOutputs(this.chemicalOutputTargets), this.steamTank, transaction);
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

      this.updateTemperatures();
      if (this.isBurning()) {
         this.kill(world);
      }

      if (this.isBurning() != this.clientBurning || Math.abs(this.getLastPlasmaTemp() - this.clientTemp) > 1000000.0) {
         this.clientBurning = this.isBurning();
         this.clientTemp = this.getLastPlasmaTemp();
         needsPacket = true;
      }

      return needsPacket;
   }

   protected void updateEjectors(Level world) {
      this.heatHandlers.clear();
      this.energyOutputTargets.clear();
      this.chemicalOutputTargets.clear();

      for (Entry<BlockPos, ValveData> entry : this.valves.entrySet()) {
         TileEntityNaquadahReactorPort tile = (TileEntityNaquadahReactorPort)WorldUtils.getTileEntity(TileEntityNaquadahReactorPort.class, world, entry.getKey());
         if (tile != null) {
            this.heatHandlers.add(tile);
            Direction side = entry.getValue().side;
            tile.addEnergyTargetCapability(this.energyOutputTargets, side);
            tile.addChemicalTargetCapability(this.chemicalOutputTargets, side);
         }
      }
   }

   public void updateTemperatures() {
      this.lastPlasmaTemperature = this.getPlasmaTemp();
      this.lastCaseTemperature = this.heatCapacitor.getTemperature();
   }

   private void kill(ServerLevel world) {
      if (this.deathZone != null && world.getRandom().nextInt() % 20 == 0) {
         DamageSource damageSource = GeneratorsDamageTypes.FUSION.source(world, this.deathZone.getCenter());

         for (Entity entity : world.getEntitiesOfClass(Entity.class, this.deathZone)) {
            entity.hurtServer(world, damageSource, 50000.0F);
         }
      }
   }

   private void vaporiseHohlraum(TransactionContext transaction) {
      if (GenExtraItems.NAQUADAH_HOHLRAUM.is((ItemResource)this.reactorSlot.resource())) {
         ResourceHandler<ChemicalResource> handler = AutomatedResourceHandler.manual(
            (ResourceHandler)Capabilities.CHEMICAL.getCapability(this.reactorSlot.asItemAccess())
         );
         if (handler != null && ResourceHandlerUtil.isFull(handler)) {
            Transaction subTransaction = Transaction.open(transaction);

            try {
               ChemicalResource fuelType = com.jerry.mekextras.common.registries.ExtraChemicals.NAQUADAH_URANIUM_FUEL.asResource();
               int needed = this.fuelTank.getNeededAsInt(ChemicalResource.EMPTY);
               int availableFuel = needed == 0 ? 0 : handler.extract(fuelType, needed, subTransaction);
               if (needed == 0 || availableFuel > 0 && this.fuelTank.insert(fuelType, availableFuel, subTransaction, AutomationType.INTERNAL) == availableFuel) {
                  ContainerType.ITEM.clearContents(this.reactorSlot, subTransaction);
                  this.setBurning(true);
                  subTransaction.commit();
               }
            } catch (Throwable var8) {
               if (subTransaction != null) {
                  try {
                     subTransaction.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (subTransaction != null) {
               subTransaction.close();
            }
         }
      }
   }

   private void injectFuel(TransactionContext transaction) {
      int amountNeeded = this.fuelTank.getNeededAsInt(ChemicalResource.EMPTY);
      int amountAvailable = 2 * Math.min(this.naquadahTank.amountAsInt(), this.uraniumTank.amountAsInt());
      int amountToInject = Math.min(amountNeeded, Math.min(amountAvailable, this.injectionRate));
      amountToInject -= amountToInject % 2;
      int injectingAmount = amountToInject / 2;
      if (injectingAmount > 0) {
         Transaction subTransaction = Transaction.open(transaction);

         try {
            if (this.naquadahTank.extract((ChemicalResource)this.naquadahTank.resource(), injectingAmount, subTransaction, AutomationType.MANUAL)
                  == injectingAmount
               && this.uraniumTank.extract((ChemicalResource)this.uraniumTank.resource(), injectingAmount, subTransaction, AutomationType.MANUAL)
                  == injectingAmount
               && this.fuelTank.insert(com.jerry.mekextras.common.registries.ExtraChemicals.NAQUADAH_URANIUM_FUEL.asResource(), amountToInject, subTransaction, AutomationType.MANUAL) == amountToInject) {
               subTransaction.commit();
            }
         } catch (Throwable var10) {
            if (subTransaction != null) {
               try {
                  subTransaction.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (subTransaction != null) {
            subTransaction.close();
         }
      }
   }

   private int burnFuel(TransactionContext transaction) {
      ChemicalResource fuel = (ChemicalResource)this.fuelTank.resource();
      if (fuel.isEmpty()) {
         return 0;
      } else {
         int fuelBurned = Math.clamp((long)MathUtils.clampToInt((this.getPlasmaTemp() - 4.0E8) * 1.0), 0, this.fuelTank.amountAsInt());
         int fuelUsed = this.fuelTank.extract(fuel, fuelBurned, transaction, AutomationType.INTERNAL);
         if (fuelUsed < fuelBurned) {
            return 0;
         } else {
            this.plasmaJournal.updateSnapshots(transaction);
            this.plasmaJournal.temperature = this.plasmaJournal.temperature
               + MathUtils.multiplyClamped(GeneratorsExtraConfig.extraGenerators.energyPerReactorFuel.get(), fuelBurned) / 100.0;
            return fuelBurned;
         }
      }
   }

   private void transferHeat(TransactionContext transaction) {
      double plasmaCaseHeat = 0.2 * (this.getPlasmaTemp() - this.heatCapacitor.getTemperature());
      if (Math.abs(plasmaCaseHeat) > 1.0E-6F) {
         this.plasmaJournal.updateSnapshots(transaction);
         this.plasmaJournal.temperature -= plasmaCaseHeat / 100.0;
         this.heatCapacitor.handleHeat(plasmaCaseHeat, transaction);
      }

      double caseWaterHeat = GeneratorsExtraConfig.extraGenerators.reactorWaterHeatingRatio.get() * (this.heatCapacitor.getTemperature() - this.biomeAmbientTemp);
      double lostToWater = 0.0;
      if (!this.waterTank.isEmpty() && Math.abs(caseWaterHeat) > 1.0E-6F) {
         Transaction subTransaction = Transaction.open(transaction);

         try {
            ChemicalResource steam = com.jerry.genextras.common.registries.GenExtraChemicals.POLONIUM_CONTAINING_STEAM.asResource();
            int waterToVaporize = (int)(HeatUtils.getSteamEnergyEfficiency() * caseWaterHeat / HeatUtils.getWaterThermalEnthalpy());
            int vaporized = this.waterTank
               .extract(
                  (FluidResource)this.waterTank.resource(),
                  Math.min(waterToVaporize, this.steamTank.getNeededAsInt(steam)),
                  subTransaction,
                  AutomationType.INTERNAL
               );
            if (vaporized > 0) {
               this.steamTank.insert(steam, vaporized, subTransaction, AutomationType.INTERNAL);
               lostToWater = vaporized * HeatUtils.getWaterThermalEnthalpy() / HeatUtils.getSteamEnergyEfficiency();
               this.heatCapacitor.handleHeat(-lostToWater, subTransaction);
               subTransaction.commit();
            }
         } catch (Throwable var13) {
            if (subTransaction != null) {
               try {
                  subTransaction.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (subTransaction != null) {
            subTransaction.close();
         }
      }

      this.lastTransferLoss = this.simulateAdjacent(transaction) + lostToWater;
      this.lastEnvironmentLoss = 0.0;
      double caseAirHeat = GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get()
         * (this.heatCapacitor.getTemperature() - this.biomeAmbientTemp);
      if (Math.abs(caseAirHeat) > 1.0E-6F) {
         this.heatCapacitor.handleHeat(-caseAirHeat, transaction);
         this.lastEnvironmentLoss = caseAirHeat;
         int powerGen = MathUtils.clampToInt(caseAirHeat * GeneratorsExtraConfig.extraGenerators.reactorThermocoupleEfficiency.get());
         if (powerGen > 0) {
            this.energyContainer.insert(powerGen, transaction, AutomationType.INTERNAL);
         }
      }
   }

   public HeatTransfer simulate(TransactionContext transaction) {
      throw new UnsupportedOperationException("I'm special");
   }

   public double simulateAdjacent(TransactionContext transaction) {
      double adjacentTransfer = 0.0;

      for (ITileHeatHandler source : this.heatHandlers) {
         adjacentTransfer += source.simulateAdjacent(transaction);
      }

      return adjacentTransfer;
   }

   @ComputerMethod(nameOverride = "getPlasmaTemperature")
   public double getLastPlasmaTemp() {
      return this.lastPlasmaTemperature;
   }

   @ComputerMethod(nameOverride = "getCaseTemperature")
   public double getLastCaseTemp() {
      return this.lastCaseTemperature;
   }

   public double getPlasmaTemp() {
      return this.plasmaJournal.temperature;
   }

   public void setPlasmaTemp(double temp, TransactionContext transaction) {
      this.plasmaJournal.updateSnapshots(transaction);
      this.plasmaJournal.temperature = temp;
   }

   @ComputerMethod
   public int getInjectionRate() {
      return this.injectionRate;
   }

   public void setInjectionRate(int rate) {
      this.setInjectionRate(rate, null);
   }

   public void setInjectionRate(int rate, @Nullable TransactionContext transaction) {
      if (this.injectionRate != rate) {
         this.injectionRate = rate;
         this.maxWater = this.injectionRate * GeneratorsExtraConfig.extraGenerators.reactorWaterPerInjection.get();
         this.maxSteam = this.injectionRate * GeneratorsExtraConfig.extraGenerators.reactorSteamPerInjection.get();
         if (!this.isRemote()) {
            ContainerType.FLUID.clampContents(this.waterTank, transaction);
            ContainerType.CHEMICAL.clampContents(this.steamTank, transaction);
         }

         this.markDirty();
      }
   }

   private long getMaxWater() {
      return this.maxWater;
   }

   private long getMaxSteam() {
      return this.maxSteam;
   }

   @ComputerMethod(nameOverride = "isIgnited", methodDescription = "Checks if a reaction is occurring.")
   public boolean isBurning() {
      return this.burning;
   }

   public void setBurning(boolean burn) {
      if (this.burning != burn) {
         this.burning = burn;
         this.markDirty();
      }
   }

   protected int getMultiblockRedstoneLevel() {
      return ContainerType.CHEMICAL.getRedstoneSignalFromContainer(this.fuelTank);
   }

   @ComputerMethod(methodDescription = "true -> water cooled, false -> air cooled")
   public int getMinInjectionRate(boolean active) {
      double k = active ? GeneratorsExtraConfig.extraGenerators.reactorWaterHeatingRatio.get() : 0.0;
      double caseAirConductivity = GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get();
      double aMin = 2.0E7
         * (k + caseAirConductivity)
         / (GeneratorsExtraConfig.extraGenerators.energyPerReactorFuel.get() * 1.0 * (0.2 + k + caseAirConductivity) - 0.2 * (k + caseAirConductivity));
      return 2 * Mth.ceil(aMin / 2.0);
   }

   @ComputerMethod(methodDescription = "true -> water cooled, false -> air cooled")
   public double getMaxPlasmaTemperature(boolean active) {
      double k = active ? GeneratorsExtraConfig.extraGenerators.reactorWaterHeatingRatio.get() : 0.0;
      double caseAirConductivity = GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get();
      int injectionRate = Math.max(this.injectionRate, this.lastBurned);
      return injectionRate * GeneratorsExtraConfig.extraGenerators.energyPerReactorFuel.get() / 0.2 * (0.2 + k + caseAirConductivity) / (k + caseAirConductivity);
   }

   @ComputerMethod(methodDescription = "true -> water cooled, false -> air cooled")
   public double getMaxCasingTemperature(boolean active) {
      double k = active ? GeneratorsExtraConfig.extraGenerators.reactorWaterHeatingRatio.get() : 0.0;
      int injectionRate = Math.max(this.injectionRate, this.lastBurned);
      return MathUtils.multiplyClamped(GeneratorsExtraConfig.extraGenerators.energyPerReactorFuel.get(), injectionRate)
         / (k + GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get());
   }

   @ComputerMethod(methodDescription = "true -> water cooled, false -> air cooled")
   public double getIgnitionTemperature(boolean active) {
      double k = active ? GeneratorsExtraConfig.extraGenerators.reactorWaterHeatingRatio.get() : 0.0;
      double caseAirConductivity = GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get();
      double energyPerFusionFuel = GeneratorsExtraConfig.extraGenerators.energyPerReactorFuel.get();
      return 4.0E8
         * energyPerFusionFuel
         * 1.0
         * (0.2 + k + caseAirConductivity)
         / (energyPerFusionFuel * 1.0 * (0.2 + k + caseAirConductivity) - 0.2 * (k + caseAirConductivity));
   }

   public long getPassiveGeneration(boolean active, boolean current) {
      double temperature = current ? this.getLastCaseTemp() : this.getMaxCasingTemperature(active);
      return MathUtils.clampToLong(
         GeneratorsExtraConfig.extraGenerators.reactorThermocoupleEfficiency.get()
            * GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get()
            * temperature
      );
   }

   public int getSteamPerTick(boolean current) {
      double temperature = current ? this.getLastCaseTemp() : this.getMaxCasingTemperature(true);
      return MathUtils.clampToInt(
         HeatUtils.getSteamEnergyEfficiency()
            * GeneratorsExtraConfig.extraGenerators.reactorWaterHeatingRatio.get()
            * temperature
            / HeatUtils.getWaterThermalEnthalpy()
      );
   }

   private static double getInverseConductionCoefficient() {
      return 1.0 / GeneratorsExtraConfig.extraGenerators.reactorCasingThermalConductivity.get();
   }

   @ComputerMethod(nameOverride = "setInjectionRate")
   void computerSetInjectionRate(int rate) throws ComputerException {
      if (rate < 0 || rate > 98) {
         throw new ComputerException("Injection Rate '%d' is out of range must be an even number between 0 and %d. (Inclusive)", new Object[]{rate, 98});
      } else if (rate % 2 != 0) {
         throw new ComputerException("Injection Rate '%d' must be an even number between 0 and %d. (Inclusive)", new Object[]{rate, 98});
      } else {
         this.setInjectionRate(rate);
      }
   }

   @ComputerMethod
   long getPassiveGeneration(boolean active) {
      return this.getPassiveGeneration(active, false);
   }

   @ComputerMethod
   long getProductionRate() {
      return this.getPassiveGeneration(false, true);
   }

   public void setPlasmaTemp(double temperature) { setPlasmaTemp(temperature, null); }


   private class PlasmaJournal extends SnapshotJournal<Double> {
      private double temperature;

      private PlasmaJournal() {
         Objects.requireNonNull(NaquadahReactorMultiblockData.this);
         super();
      }

      protected Double createSnapshot() {
         return this.temperature;
      }

      protected void revertToSnapshot(Double snapshot) {
         this.temperature = snapshot;
      }

      protected void onRootCommit(Double originalState) {
         super.onRootCommit(originalState);
         if (!Mth.equal(originalState, this.temperature)) {
            NaquadahReactorMultiblockData.this.markDirty();
         }
      }
   }
}

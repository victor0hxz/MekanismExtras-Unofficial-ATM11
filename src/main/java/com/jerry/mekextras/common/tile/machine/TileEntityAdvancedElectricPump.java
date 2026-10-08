package com.jerry.mekextras.common.tile.machine;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mekanism.api.AutomationType;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.capabilities.holder.single.BasicSingleHolder;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerFluidTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableResource;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import com.jerry.mekextras.common.registries.ExtraFluids;
import com.jerry.mekextras.common.config.ExtraConfig;
import com.jerry.mekextras.api.ExtraUpgrade;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.ResourceUtils;
import mekanism.common.util.UpgradeUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueOutput.TypedOutputList;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public class TileEntityAdvancedElectricPump extends TileEntityMekanism implements IConfigurable {
   private static final int BASE_TICKS_REQUIRED = 19;
   public static final long MAX_FLUID = 10000L;
   private static final int BASE_OUTPUT_RATE = 1024;
   @WrappingComputerMethod(
      wrapper = ComputerFluidTankWrapper.class,
      methodNames = {"getFluid", "getFluidCapacity", "getFluidNeeded", "getFluidFilledPercentage"},
      docPlaceholder = "buffer tank"
   )
   public @UnknownNullability BasicFluidTank fluidTank;
   private FluidResource activeType = FluidResource.EMPTY;
   public int ticksRequired = 19;
   public int operatingTicks;
   private boolean usedEnergy = false;
   private int outputRate = 256;
   private final Set<BlockPos> recurringNodes = new ObjectOpenHashSet();
   private @Nullable BlockCapabilityCache<ResourceHandler<FluidResource>, @Nullable Direction> fluidHandlerAbove;
   private @UnknownNullability MachineEnergyContainer<TileEntityAdvancedElectricPump> energyContainer;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getInputItem", docPlaceholder = "input slot")
   @UnknownNullability FluidInventorySlot inputSlot;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getOutputItem", docPlaceholder = "output slot")
   @UnknownNullability OutputInventorySlot outputSlot;
   @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
   @UnknownNullability EnergyInventorySlot energySlot;

   public TileEntityAdvancedElectricPump(BlockPos pos, BlockState state) {
      super(ExtraBlocks.ADVANCED_ELECTRIC_PUMP, pos, state);
   }

   protected IContainerHolder<IFluidTank> getInitialFluidTanks(IContentsListener listener) {
      MekContainerHelper<IFluidTank> builder = MekContainerHelper.forSide(this.facingSupplier);
      builder.addContainer(this.fluidTank = BasicFluidTank.output(10_000L * MAX_FLUID, listener), new RelativeSide[]{RelativeSide.TOP});
      return builder.build();
   }

   protected ISingleContainerHolder<IEnergyContainer> getInitialEnergyContainer(IContentsListener listener) {
      this.energyContainer = MachineEnergyContainer.input(this, listener);
      return new BasicSingleHolder(this.energyContainer, this.facingSupplier, BACK_ONLY);
   }

   protected IContainerHolder<IInventorySlot> getInitialInventory(IContentsListener listener) {
      MekContainerHelper<IInventorySlot> builder = MekContainerHelper.forSide(this.facingSupplier);
      builder.addContainer(this.inputSlot = FluidInventorySlot.drain(this.fluidTank, listener, 28, 20), new RelativeSide[]{RelativeSide.TOP});
      builder.addContainer(this.outputSlot = OutputInventorySlot.at(listener, 28, 51), new RelativeSide[]{RelativeSide.BOTTOM});
      builder.addContainer(
         this.energySlot = EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 143, 35), new RelativeSide[]{RelativeSide.BACK}
      );
      return builder.build();
   }

   protected boolean onUpdateServer(ServerLevel level) {
      boolean sendUpdatePacket = super.onUpdateServer(level);
      this.energySlot.fillContainerOrConvert(null);
      this.inputSlot.drainTankIntoSlot(this.outputSlot, null);
      int clientEnergyUsed = 0;
      if (this.canFunction() && (this.fluidTank.isEmpty() || this.estimateIncrementAmount() <= this.fluidTank.getNeededAsInt(FluidResource.EMPTY))) {
         int energyPerTick = this.energyContainer.getEnergyPerTick();
         Transaction transaction = Transaction.openRoot();

         try {
            if (this.energyContainer.extract(energyPerTick, transaction, AutomationType.INTERNAL) == energyPerTick) {
               if (!this.activeType.isEmpty()) {
                  clientEnergyUsed = energyPerTick;
               }

               this.operatingTicks++;
               if (this.operatingTicks >= this.ticksRequired) {
                  this.operatingTicks = 0;
                  if (this.suck(level, transaction)) {
                     clientEnergyUsed = energyPerTick;
                  } else {
                     this.reset();
                  }
               }

               if (clientEnergyUsed > 0) {
                  transaction.commit();
               }
            }
         } catch (Throwable var9) {
            if (transaction != null) {
               try {
                  transaction.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (transaction != null) {
            transaction.close();
         }
      }

      this.usedEnergy = clientEnergyUsed > 0;
      if (!this.fluidTank.isEmpty()) {
         if (this.fluidHandlerAbove == null) {
            this.fluidHandlerAbove = Capabilities.FLUID.createCache(level, this.worldPosition.above(), Direction.DOWN);
         }

         ResourceUtils.emit((ResourceHandler)this.fluidHandlerAbove.getCapability(), this.fluidTank, this.outputRate, null);
      }

      return sendUpdatePacket;
   }

   public void setLevel(Level world) {
      super.setLevel(world);
      this.fluidHandlerAbove = null;
   }

   public int estimateIncrementAmount() {
      return ((FluidResource)this.fluidTank.resource()).is(MekanismFluids.HEAVY_WATER) ? ExtraConfig.extraGeneralConfig.pumpHeavyWaterAmount.get() : 1000;
   }

   private boolean suck(ServerLevel level, TransactionContext transaction) {
      boolean hasFilter = this.getUpgrades(Upgrade.FILTER) > 0;
      if (this.suck(level, this.worldPosition.relative(Direction.DOWN), hasFilter, true, transaction)) {
         return true;
      } else {
         List<BlockPos> tempPumpList = new ArrayList<>(this.recurringNodes);
         Collections.shuffle(tempPumpList);
         MutableBlockPos mutable = new MutableBlockPos();

         for (BlockPos tempPumpPos : tempPumpList) {
            if (this.suck(level, tempPumpPos, hasFilter, false, transaction)) {
               return true;
            }

            for (Direction orientation : EnumUtils.DIRECTIONS) {
               mutable.setWithOffset(tempPumpPos, orientation);
               if (WorldUtils.distanceBetween(this.worldPosition, mutable) <= MekanismConfig.general.maxPumpRange.get()
                  && this.suck(level, mutable, hasFilter, true, transaction)) {
                  return true;
               }
            }

            this.recurringNodes.remove(tempPumpPos);
         }

         return false;
      }
   }

   private boolean suck(ServerLevel level, BlockPos pos, boolean hasFilter, boolean addRecurring, TransactionContext transaction) {
      Optional<BlockState> state = WorldUtils.getBlockState(level, pos);
      if (state.isEmpty()) {
         return false;
      } else {
         BlockState blockState = state.get();
         FluidState fluidState = blockState.getFluidState();
         if (!fluidState.isEmpty() && fluidState.isSource() && blockState.getBlock() instanceof BucketPickup bucketPickup) {
            boolean var24 = false;
            Fluid sourceFluid = fluidState.getType();
            Transaction subTransaction = Transaction.open(transaction);

            boolean var28;
            label151: {
               boolean var33;
               label152: {
                  label153: {
                     boolean var35;
                     label154: {
                        label163: {
                           try {
                              FluidStack fluidStack = this.getOutput(sourceFluid, hasFilter);
                              if (!this.activeType.isEmpty() && !this.activeType.matches(fluidStack)) {
                                 var28 = false;
                                 break label151;
                              }

                              FluidResource fluidType = FluidResource.of(fluidStack);
                              int amountProduced = fluidStack.amount();
                              int inserted = this.fluidTank.insert(fluidType, amountProduced, subTransaction, AutomationType.INTERNAL);
                              if (inserted < amountProduced) {
                                 var33 = false;
                                 break label152;
                              }

                              if (this.isInfiniteSource(level, sourceFluid)) {
                                 subTransaction.commit();
                                 this.suck(level, fluidType, pos, addRecurring);
                                 var33 = true;
                                 break label153;
                              }

                              ItemStack pickedUpStack = bucketPickup.pickupBlock(null, level, pos, blockState);
                              if (pickedUpStack.isEmpty()) {
                                 var35 = false;
                                 break label154;
                              }

                              if (pickedUpStack.getItem() instanceof BucketItem bucket) {
                                 if (sourceFluid == bucket.content) {
                                    subTransaction.commit();
                                    this.suck(level, fluidType, pos, addRecurring);
                                    var35 = true;
                                    break label163;
                                 }

                                 sourceFluid = bucket.content;
                                 var24 = true;
                              }
                           } catch (Throwable var23) {
                              if (subTransaction != null) {
                                 try {
                                    subTransaction.close();
                                 } catch (Throwable var21) {
                                    var23.addSuppressed(var21);
                                 }
                              }

                              throw var23;
                           }

                           if (subTransaction != null) {
                              subTransaction.close();
                           }

                           label101:
                           if (var24) {
                              subTransaction = Transaction.open(transaction);

                              label111: {
                                 try {
                                    FluidStack fluidStackx = this.getOutput(sourceFluid, hasFilter);
                                    FluidResource fluidTypex = FluidResource.of(fluidStackx);
                                    int amountProducedx = fluidStackx.amount();
                                    int insertedx = this.fluidTank.insert(fluidTypex, amountProducedx, subTransaction, AutomationType.INTERNAL);
                                    if (insertedx > 0) {
                                       subTransaction.commit();
                                       this.suck(level, fluidTypex, pos, addRecurring);
                                       if (insertedx < amountProducedx) {
                                          Mekanism.logger
                                             .warn(
                                                "Fluid removed without successfully picking the full thing up. Fluid {} at {} in {} was valid, but after picking up was {}. Accepted {} out of attempted {}.",
                                                new Object[]{
                                                   fluidState.getType(), pos, level.dimension().identifier(), sourceFluid, insertedx, amountProducedx
                                                }
                                             );
                                       }

                                       var33 = true;
                                       break label111;
                                    }
                                 } catch (Throwable var22) {
                                    if (subTransaction != null) {
                                       try {
                                          subTransaction.close();
                                       } catch (Throwable var20) {
                                          var22.addSuppressed(var20);
                                       }
                                    }

                                    throw var22;
                                 }

                                 if (subTransaction != null) {
                                    subTransaction.close();
                                 }
                                 break label101;
                              }

                              if (subTransaction != null) {
                                 subTransaction.close();
                              }

                              return var33;
                           }

                           Mekanism.logger
                              .warn(
                                 "Fluid removed without successfully picking up. Fluid {} at {} in {} was valid, but after picking up was {}.",
                                 new Object[]{fluidState.getType(), pos, level.dimension().identifier(), sourceFluid}
                              );
                           return false;
                        }

                        if (subTransaction != null) {
                           subTransaction.close();
                        }

                        return var35;
                     }

                     if (subTransaction != null) {
                        subTransaction.close();
                     }

                     return var35;
                  }

                  if (subTransaction != null) {
                     subTransaction.close();
                  }

                  return var33;
               }

               if (subTransaction != null) {
                  subTransaction.close();
               }

               return var33;
            }

            if (subTransaction != null) {
               subTransaction.close();
            }

            return var28;
         } else {
            return false;
         }
      }
   }

   private boolean isInfiniteSource(ServerLevel level, Fluid sourceFluid) {
      if (!MekanismConfig.general.pumpInfiniteFluidSources.get()) {
         if (sourceFluid == Fluids.WATER) {
            return (Boolean)level.getGameRules().get(GameRules.WATER_SOURCE_CONVERSION);
         }

         if (sourceFluid == Fluids.LAVA) {
            return (Boolean)level.getGameRules().get(GameRules.LAVA_SOURCE_CONVERSION);
         }
      }

      return false;
   }

   private FluidStack getOutput(Fluid sourceFluid, boolean hasFilter) {
      if (sourceFluid == Fluids.WATER) {
         if (hasFilter) return MekanismFluids.HEAVY_WATER.asStack(ExtraConfig.extraGeneralConfig.pumpHeavyWaterAmount.get());
         return new FluidStack(sourceFluid, MekanismConfig.general.pumpInfiniteFluidSources.get() ? 1000 : 100_000);
      }
      if (getComponent().getUpgrades(ExtraUpgrade.IONIC_MEMBRANE) > 0) {
         if (sourceFluid == ExtraFluids.NAQUADAH_HEXAFLUORIDE.get()) return ExtraFluids.RICH_NAQUADAH_FUEL.asStack(1000);
         if (sourceFluid == MekanismFluids.URANIUM_HEXAFLUORIDE.get()) return ExtraFluids.RICH_URANIUM_FUEL.asStack(1000);
      }
      return new FluidStack(sourceFluid, 1000);
   }
   private void suck(ServerLevel level, FluidResource fluidType, BlockPos pos, boolean addRecurring) {
      this.activeType = fluidType;
      if (addRecurring) {
         this.recurringNodes.add(pos.immutable());
      }

      level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
   }

   public void reset() {
      this.activeType = FluidResource.EMPTY;
      this.recurringNodes.clear();
   }

   public void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("progress", this.operatingTicks);
      if (!this.activeType.isEmpty()) {
         output.store("fluid", FluidResource.CODEC, this.activeType);
      }

      if (!this.recurringNodes.isEmpty()) {
         TypedOutputList<BlockPos> recurringNodesOutput = output.list("recurring_nodes", BlockPos.CODEC);

         for (BlockPos recurringNode : this.recurringNodes) {
            recurringNodesOutput.add(recurringNode);
         }
      }
   }

   public void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.operatingTicks = input.getIntOr("progress", this.operatingTicks);
      this.activeType = input.read("fluid", FluidResource.CODEC).orElse(FluidResource.EMPTY);

      for (BlockPos pos : input.listOrEmpty("recurring_nodes", BlockPos.CODEC)) {
         this.recurringNodes.add(pos);
      }
   }

   @Deprecated
   public void removeComponentsFromTag(ValueOutput output) {
      super.removeComponentsFromTag(output);
      output.discard("recurring_nodes");
   }

   public InteractionResult onSneakRightClick(Level level, Player player) {
      this.reset();
      player.sendOverlayMessage(MekanismLang.PUMP_RESET.translate());
      return InteractionResult.SUCCESS;
   }

   public InteractionResult onRightClick(Level level, Player player) {
      return InteractionResult.PASS;
   }

   public boolean supportsMode(RedstoneControl mode) {
      return true;
   }

   public void recalculateUpgrades(Upgrade upgrade) {
      super.recalculateUpgrades(upgrade);
      if (upgrade == Upgrade.SPEED) {
         this.ticksRequired = MekanismUtils.getTicks(this, 19);
         this.outputRate = 256 * (1 + this.getUpgrades(Upgrade.SPEED));
      }
   }

   public int getRedstoneLevel() {
      return ContainerType.FLUID.getRedstoneSignalFromContainer(this.fluidTank);
   }

   protected boolean makesComparatorDirty(IContainerType<?, ?> type) {
      return type == ContainerType.FLUID;
   }

   public List<Component> getInfo(Upgrade upgrade) {
      return UpgradeUtils.getMultScaledInfo(this, upgrade);
   }

   public MachineEnergyContainer<TileEntityAdvancedElectricPump> energyContainer() {
      return this.energyContainer;
   }

   public boolean usedEnergy() {
      return this.usedEnergy;
   }

   public FluidResource getActiveType() {
      return this.activeType;
   }

   public void addContainerTrackers(MekanismContainer container) {
      super.addContainerTrackers(container);
      container.track(SyncableBoolean.create(this::usedEnergy, value -> this.usedEnergy = value));
      container.track(SyncableResource.createFluid(this::getActiveType, value -> this.activeType = value));
   }

   @ComputerMethod(nameOverride = "reset", requiresPublicSecurity = true)
   void resetPump() throws ComputerException {
      this.validateSecurityIsPublic();
      this.reset();
   }
}

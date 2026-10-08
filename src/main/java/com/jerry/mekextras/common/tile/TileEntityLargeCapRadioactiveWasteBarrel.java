package com.jerry.mekextras.common.tile;

import mekanism.api.AutomationType;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.MekanismAPITags.Chemicals;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.Capabilities;
import com.jerry.mekextras.common.capabilities.chemical.StackedLargeCapWasteBarrel;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.capabilities.proxy.BelowContainerCache;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.RWBTier;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.ResourceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public class TileEntityLargeCapRadioactiveWasteBarrel extends TileEntityMekanism implements IConfigurable {
   private final RWBTier tier;
   public RWBTier getTier() { return tier; }
   private long lastProcessTick;
   @WrappingComputerMethod(
      wrapper = ComputerChemicalTankWrapper.class,
      methodNames = {"getStored", "getCapacity", "getNeeded", "getFilledPercentage"},
      docPlaceholder = "barrel"
   )
   @UnknownNullability StackedLargeCapWasteBarrel chemicalTank;
   private @Nullable BelowContainerCache<ChemicalResource, IChemicalTank> belowTankCache;
   private int processTicks;

   public TileEntityLargeCapRadioactiveWasteBarrel(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
      tier = ExtraAttribute.getAdvancedTier(blockProvider, RWBTier.class);
      super(blockProvider, pos, state);
      this.delaySupplier = NO_DELAY;
   }

   public IContainerHolder<IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
      MekContainerHelper<IChemicalTank> builder = MekContainerHelper.forSide(this.facingSupplier);
      builder.addContainer(this.chemicalTank = StackedLargeCapWasteBarrel.create(this, listener), new RelativeSide[]{RelativeSide.TOP, RelativeSide.BOTTOM});
      return builder.build();
   }

   protected boolean onUpdateServer(ServerLevel level) {
      boolean sendUpdatePacket = super.onUpdateServer(level);
      long gameTime = level.getGameTime();
      if (gameTime > this.lastProcessTick) {
         this.lastProcessTick = gameTime;
         if (!this.chemicalTank.isEmpty()) {
            ChemicalResource chemicalType = (ChemicalResource)this.chemicalTank.resource();
            int decayAmount = mekanism.api.math.MathUtils.clampToInt(tier.getDecayAmount());
            if (decayAmount > 0
               && !chemicalType.is(Chemicals.WASTE_BARREL_DECAY_BLACKLIST)
               && ++this.processTicks >= tier.getProcessTicks()) {
               this.processTicks = 0;
               Transaction transaction = Transaction.openRoot();

               try {
                  this.chemicalTank.extract(chemicalType, decayAmount, transaction, AutomationType.INTERNAL);
                  transaction.commit();
               } catch (Throwable var11) {
                  if (transaction != null) {
                     try {
                        transaction.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (transaction != null) {
                  transaction.close();
               }
            }
         }

         if (this.getActive()) {
            if (this.belowTankCache == null) {
               this.belowTankCache = new BelowContainerCache(Capabilities.CHEMICAL, level, this.worldPosition);
            }

            int toEmit = this.chemicalTank.amountAsInt();
            IChemicalTank below = (IChemicalTank)this.belowTankCache.getContainer(StackedLargeCapWasteBarrel.class);
            if (below != null) {
               toEmit = Math.min(below.getNeededAsInt(ChemicalResource.EMPTY), toEmit);
            }

            ResourceUtils.emit(this.belowTankCache.getHandler(), this.chemicalTank, toEmit, null);
         }
      }

      return sendUpdatePacket;
   }

   public void setLevel(Level world) {
      super.setLevel(world);
      this.belowTankCache = null;
   }

   public StackedLargeCapWasteBarrel getChemicalTank() {
      return this.chemicalTank;
   }

   public StackedLargeCapWasteBarrel getGasTank() { return chemicalTank; }
   public double getGasScale() { return getChemicalScale(); }
   public mekanism.api.chemical.ChemicalStack getGas() { return chemicalTank.resource().toStack(chemicalTank.amountAsInt()); }
   public double getChemicalScale() {
      return (double)this.chemicalTank.amountAsLong() / this.chemicalTank.capacityAsLong((ChemicalResource)this.chemicalTank.resource());
   }

   public InteractionResult onSneakRightClick(Level level, Player player) {
      if (!level.isClientSide()) {
         this.setActive(!this.getActive());
         level.playSound(
            null,
            this.getBlockPos().getX(),
            this.getBlockPos().getY(),
            this.getBlockPos().getZ(),
            (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(),
            SoundSource.BLOCKS,
            0.3F,
            1.0F
         );
      }

      return InteractionResult.SUCCESS;
   }

   public InteractionResult onRightClick(Level level, Player player) {
      return InteractionResult.PASS;
   }

   public void writeReducedUpdatedTag(ValueOutput output) {
      super.writeReducedUpdatedTag(output);
      output.putChild("chemical", this.chemicalTank);
      output.putInt("progress", this.processTicks);
   }

   public void handleUpdateTag(ValueInput input) {
      super.handleUpdateTag(input);
      input.readChild("chemical", this.chemicalTank);
      this.processTicks = input.getIntOr("progress", this.processTicks);
   }

   public int getRedstoneLevel() {
      return ContainerType.CHEMICAL.getRedstoneSignalFromContainer(this.chemicalTank);
   }

   protected boolean makesComparatorDirty(IContainerType<?, ?> type) {
      return type == ContainerType.CHEMICAL;
   }
}

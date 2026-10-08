package com.jerry.genextras.common.tile.naquadah;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.Locale;
import java.util.function.IntFunction;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ILangEntry;
import mekanism.api.text.IHasTranslationKey.IHasEnumNameTranslationKey;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.tile.interfaces.IHasMode;
import mekanism.common.util.NBTUtils;
import mekanism.generators.common.GeneratorsLang;
import mekanism.generators.common.base.IReactorLogic;
import mekanism.generators.common.base.IReactorLogicMode;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.registries.GenExtraBlocks;
import mekanism.generators.common.registries.GeneratorsDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;

public class TileEntityNaquadahReactorLogicAdapter
   extends TileEntityNaquadahReactorCasing
   implements IReactorLogic<TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic>,
   IHasMode {
   public TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic logicType = TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic.DISABLED;
   private boolean activeCooled;
   private boolean prevOutputting;

   public TileEntityNaquadahReactorLogicAdapter(BlockPos pos, BlockState state) {
      super(GenExtraBlocks.NAQUADAH_REACTOR_LOGIC_ADAPTER, pos, state);
   }

   protected boolean onUpdateServer(ServerLevel level, NaquadahReactorMultiblockData multiblock) {
      boolean needsPacket = super.onUpdateServer(level, multiblock);
      boolean outputting = this.checkMode();
      if (outputting != this.prevOutputting) {
         BlockState state = this.getBlockState();
         Direction side = multiblock.getOutsideSide(this.worldPosition);
         if (side == null) {
            level.updateNeighbourForOutputSignal(this.getBlockPos(), state.getBlock());
         } else if (!EventHooks.onNeighborNotify(level, this.worldPosition, state, EnumSet.of(side), false).isCanceled()) {
            BlockPos toUpdate = this.worldPosition.relative(side);
            level.getBlockState(toUpdate).onNeighborChange(level, toUpdate, this.worldPosition);
         }

         this.prevOutputting = outputting;
      }

      return needsPacket;
   }

   public int getRedstoneLevel(Direction side) {
      return !this.isRemote()
            && ((NaquadahReactorMultiblockData)this.getMultiblock()).isPositionOutsideBounds(this.worldPosition.relative(side))
            && this.checkMode()
         ? 15
         : 0;
   }

   public boolean checkMode() {
      if (this.isRemote()) {
         return this.prevOutputting;
      } else {
         NaquadahReactorMultiblockData multiblock = (NaquadahReactorMultiblockData)this.getMultiblock();
         if (!multiblock.isFormed()) {
            return false;
         } else {
            return switch (this.logicType) {
               case DISABLED -> false;
               case READY -> multiblock.getLastPlasmaTemp() >= multiblock.getIgnitionTemperature(this.activeCooled);
               case CAPACITY -> multiblock.getLastPlasmaTemp() >= multiblock.getMaxPlasmaTemperature(this.activeCooled);
               case DEPLETED -> {
                  if (multiblock.fuelTank.isEmpty()) {
                     int injectionPortion = multiblock.getInjectionRate() / 2;
                     yield injectionPortion == 0
                        || multiblock.naquadahTank.amountAsLong() < injectionPortion
                        || multiblock.uraniumTank.amountAsLong() < injectionPortion;
                  } else {
                     yield false;
                  }
               }
            };
         }
      }
   }

   public void readSustainedData(ValueInput input) {
      super.readSustainedData(input);
      NBTUtils.setEnumIfPresent(input, "logic_type", TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic.BY_ID, logicType -> this.logicType = logicType);
      this.activeCooled = input.getBooleanOr("active_cooled", this.activeCooled);
   }

   public void writeSustainedData(ValueOutput output) {
      super.writeSustainedData(output);
      NBTUtils.writeEnum(output, "logic_type", this.logicType);
      output.putBoolean("active_cooled", this.activeCooled);
   }

   protected void collectImplicitComponents(Builder builder) {
      super.collectImplicitComponents(builder);
      builder.set(com.jerry.genextras.common.registries.GenExtraDataComponents.NAQUADAH_LOGIC_TYPE, this.logicType);
      builder.set(GeneratorsDataComponents.ACTIVE_COOLED, this.activeCooled);
   }

   protected void applyImplicitComponents(DataComponentGetter input) {
      super.applyImplicitComponents(input);
      this.logicType = (TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic)input.getOrDefault(com.jerry.genextras.common.registries.GenExtraDataComponents.NAQUADAH_LOGIC_TYPE, this.logicType);
      this.activeCooled = (Boolean)input.getOrDefault(GeneratorsDataComponents.ACTIVE_COOLED, this.activeCooled);
   }

   public void nextMode() {
      this.activeCooled = !this.activeCooled;
      this.markForSave();
   }

   public void previousMode() {
      this.nextMode();
   }

   @ComputerMethod(nameOverride = "isActiveCooledLogic")
   public boolean isActiveCooled() {
      return this.activeCooled;
   }

   @ComputerMethod(nameOverride = "getLogicMode")
   public TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic getMode() {
      return this.logicType;
   }

   public TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic[] getModes() {
      return TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic.values();
   }

   @ComputerMethod(nameOverride = "setLogicMode")
   public void setLogicTypeFromPacket(TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic logicType) {
      if (this.logicType != logicType) {
         this.logicType = logicType;
         this.markForSave();
      }
   }

   @Override
   public void addContainerTrackers(MekanismContainer container) {
      super.addContainerTrackers(container);
      container.track(
         SyncableEnum.create(
            TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic.BY_ID,
            TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic.DISABLED,
            this::getMode,
            value -> this.logicType = value
         )
      );
      container.track(SyncableBoolean.create(this::isActiveCooled, value -> this.activeCooled = value));
      container.track(SyncableBoolean.create(() -> this.prevOutputting, value -> this.prevOutputting = value));
   }

   @ComputerMethod
   void setActiveCooledLogic(boolean active) {
      if (this.activeCooled != active) {
         this.nextMode();
      }
   }

   public static enum NaquadahReactorLogic
      implements IReactorLogicMode<TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic>,
      IHasEnumNameTranslationKey,
      StringRepresentable {
      DISABLED(GeneratorsLang.REACTOR_LOGIC_DISABLED, GeneratorsLang.DESCRIPTION_REACTOR_DISABLED, Items.GUNPOWDER),
      READY(GeneratorsLang.REACTOR_LOGIC_READY, GeneratorsLang.DESCRIPTION_REACTOR_READY, Items.REDSTONE),
      CAPACITY(GeneratorsLang.REACTOR_LOGIC_CAPACITY, GeneratorsLang.DESCRIPTION_REACTOR_CAPACITY, Items.REDSTONE),
      DEPLETED(GeneratorsLang.REACTOR_LOGIC_DEPLETED, GeneratorsLang.DESCRIPTION_REACTOR_DEPLETED, Items.REDSTONE);

      public static final Codec<TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic> CODEC = StringRepresentable.fromEnum(
         TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic::values
      );
      public static final IntFunction<TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic> BY_ID = ByIdMap.continuous(
         Enum::ordinal, values(), OutOfBoundsStrategy.WRAP
      );
      public static final StreamCodec<ByteBuf, TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic> STREAM_CODEC = ByteBufCodecs.idMapper(
         BY_ID, Enum::ordinal
      );
      private final ILangEntry name;
      private final ILangEntry description;
      private final ItemStackTemplate renderStack;
      private final String serializedName;

      private NaquadahReactorLogic(ILangEntry name, ILangEntry description, Item item) {
         this.name = name;
         this.description = description;
         this.renderStack = new ItemStackTemplate(item);
         this.serializedName = this.name().toLowerCase(Locale.ROOT);
      }

      public ItemStack getRenderStack() {
         return this.renderStack.create();
      }

      public String getTranslationKey() {
         return this.name.getTranslationKey();
      }

      public Component getDescription() {
         return this.description.translate();
      }

      public EnumColor getColor() {
         return EnumColor.RED;
      }

      public String getSerializedName() {
         return this.serializedName;
      }
   }
}

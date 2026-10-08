package com.jerry.mekextras.common.tile;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import mekanism.common.util.ChemicalUtils;
import mekanism.common.Mekanism;
import mekanism.common.component.containers.type.IContainerType;

import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.capabilities.chemical.ExtraChemicalTankChemicalTank;
import com.jerry.mekextras.common.tier.CTTier;
import com.jerry.mekextras.common.upgrade.ExtraChemicalTankUpgradeData;

import mekanism.api.*;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.math.MathUtils;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.SyntheticComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.TileEntityChemicalTank.GasMode;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IHasGasMode;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.upgrade.ChemicalTankUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityExtraChemicalTank extends TileEntityConfigurableMachine implements IHasGasMode {

    @SyntheticComputerMethod(getter = "getDumpingMode", getterDescription = "Get the current Dumping configuration")
    public GasMode dumping = GasMode.IDLE;

    private IChemicalTank chemicalTank;
    private CTTier tier;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDrainItem", docPlaceholder = "drain slot")
    ChemicalInventorySlot drainSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getFillItem", docPlaceholder = "fill slot")
    ChemicalInventorySlot fillSlot;

    public TileEntityExtraChemicalTank(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, tile -> new TileComponentEjector(tile,
                () -> mekanism.api.math.MathUtils.clampToInt(((TileEntityExtraChemicalTank) tile).tier.getOutput())));
        configComponent.setupIOConfig(TransmissionType.ITEM, drainSlot, fillSlot, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, getChemicalTank());
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL)
                .setCanEject(type -> canFunction() && (dumping != GasMode.DUMPING));
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        tier = ExtraAttribute.getAdvancedTier(getBlockHolder(), CTTier.class);
    }

    @Override
    public @Nullable mekanism.common.capabilities.holder.container.IContainerHolder<mekanism.api.chemical.IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
        mekanism.common.capabilities.holder.container.MekContainerHelper<mekanism.api.chemical.IChemicalTank> builder = mekanism.common.capabilities.holder.container.MekContainerHelper.forSideWithChemicalConfig(this);
        builder.addContainer(chemicalTank = ExtraChemicalTankChemicalTank.create(this, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected mekanism.common.capabilities.holder.container.IContainerHolder<mekanism.api.inventory.IInventorySlot> getInitialInventory(IContentsListener listener) {
        mekanism.common.capabilities.holder.container.MekContainerHelper<mekanism.api.inventory.IInventorySlot> builder = mekanism.common.capabilities.holder.container.MekContainerHelper.forSideWithItemConfig(this);
        builder.addContainer(drainSlot = ChemicalInventorySlot.drain(chemicalTank, listener, 16, 16));
        builder.addContainer(fillSlot = ChemicalInventorySlot.fill(chemicalTank, listener, 16, 48));
        drainSlot.setSlotType(ContainerSlotType.OUTPUT);
        drainSlot.setSlotOverlay(SlotOverlay.PLUS);
        fillSlot.setSlotType(ContainerSlotType.INPUT);
        fillSlot.setSlotOverlay(SlotOverlay.MINUS);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer(net.minecraft.server.level.ServerLevel serverLevel) {
        boolean sendUpdatePacket = super.onUpdateServer(serverLevel);
        drainSlot.drainTankIntoSlot(null);
        fillSlot.fillTankFromSlot(null);
        if (dumping != GasMode.IDLE && !chemicalTank.isEmpty()) {
            ChemicalUtils.dump(chemicalTank, dumping, tier.getStorage() / 400, tier.getOutput());
        }
        return sendUpdatePacket;
    }

    @Override
    public void nextMode(int tank) {
        if (tank == 0) {
            dumping = dumping.getNext();
            markForSave();
        }
    }

    @Override
    public boolean shouldDumpRadiation() {
        return true;
    }

    @Override
    public int getRedstoneLevel() {
        IChemicalTank currentTank = getCurrentTank();
        return MekanismUtils.redstoneLevelFromContents(currentTank.amountAsLong(), currentTank.capacityAsLong(currentTank.resource()));
    }

    @Override
    protected boolean makesComparatorDirty(IContainerType<?, ?> type) {
        return type == ContainerType.CHEMICAL;
    }

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerChemicalTankWrapper.class, methodNames = { "getStored", "getCapacity", "getNeeded", "getFilledPercentage" }, docPlaceholder = "tank")
    IChemicalTank getCurrentTank() {
        return chemicalTank;
    }

    public CTTier getTier() {
        return tier;
    }

    public IChemicalTank getChemicalTank() {
        return chemicalTank;
    }

    @Override
    public void parseUpgradeData(IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof ChemicalTankUpgradeData data) {
            redstone = data.redstone;
            setControlType(data.controlType);
            drainSlot.copyContents(data.drainSlot, transaction);
            fillSlot.copyContents(data.fillSlot, transaction);
            dumping = data.dumping;
            chemicalTank.copyContents(data.chemicalTank, transaction);
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(problemPath(), Mekanism.logger)) {
                ValueInput input = TagValueInput.create(reporter, provider, data.components);
                for (ITileComponent component : getComponents()) component.read(input);
            }
        } else {
            super.parseUpgradeData(upgradeData, provider, transaction);
        }
    }

    @NotNull
    @Override
    public ExtraChemicalTankUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new ExtraChemicalTankUpgradeData(provider, redstone, getControlType(), drainSlot, fillSlot, dumping, getChemicalTank(), getComponents(), problemPath());
    }

    @Override
    public void writeSustainedData(ValueOutput dataMap) {
        super.writeSustainedData(dataMap);
        NBTUtils.writeEnum(dataMap, SerializationConstants.DUMP_MODE, dumping);
    }

    @Override
    public void readSustainedData(ValueInput dataMap) {
        super.readSustainedData(dataMap);
        NBTUtils.setEnumIfPresent(dataMap, SerializationConstants.DUMP_MODE, GasMode.BY_ID, mode -> dumping = mode);
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(MekanismDataComponents.DUMP_MODE, dumping);
    }

    @Override
    protected void applyImplicitComponents(@NotNull net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        dumping = input.getOrDefault(MekanismDataComponents.DUMP_MODE, dumping);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(GasMode.BY_ID, GasMode.IDLE, () -> dumping, value -> dumping = value));
    }

    // Methods relating to IComputerTile
    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Set the Dumping mode of the tank")
    void setDumpingMode(GasMode mode) throws ComputerException {
        validateSecurityIsPublic();
        if (dumping != mode) {
            dumping = mode;
            markForSave();
        }
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Advance the Dumping mode to the next configuration in the list")
    void incrementDumpingMode() throws ComputerException {
        validateSecurityIsPublic();
        nextMode(0);
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Descend the Dumping mode to the previous configuration in the list")
    void decrementDumpingMode() throws ComputerException {
        validateSecurityIsPublic();
        dumping = dumping.getPrevious();
        markForSave();
    }
    // End methods IComputerTile
}

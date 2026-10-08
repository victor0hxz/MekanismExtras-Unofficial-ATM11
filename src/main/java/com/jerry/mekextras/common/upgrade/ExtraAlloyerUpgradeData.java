package com.jerry.mekextras.common.upgrade;

import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import mekanism.common.upgrade.MachineUpgradeData;

import net.minecraft.core.HolderLookup;

import java.util.List;

public class ExtraAlloyerUpgradeData extends MachineUpgradeData {

    public final InputInventorySlot extraSlot;
    public final InputInventorySlot secondExtraSlot;

    public ExtraAlloyerUpgradeData(HolderLookup.Provider provider, boolean redstone, RedstoneControl controlType, IEnergyContainer energyContainer, int[] progress,
                                    EnergyInventorySlot energySlot, InputInventorySlot extraSlot, InputInventorySlot secondExtraSlot, List<IInventorySlot> inputSlots,
                                    List<IInventorySlot> outputSlots, boolean sorting, List<ITileComponent> components, net.minecraft.util.ProblemReporter.PathElement path) {
        super(provider, redstone, controlType, energyContainer, progress, energySlot, inputSlots, outputSlots, sorting, components, path);
        this.extraSlot = extraSlot;
        this.secondExtraSlot = secondExtraSlot;
    }
}

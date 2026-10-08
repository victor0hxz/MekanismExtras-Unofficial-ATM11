package com.jerry.mekextras.common.upgrade;

import java.util.List;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.tile.TileEntityChemicalTank.GasMode;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.interfaces.IRedstoneControl;
import mekanism.common.upgrade.ChemicalTankUpgradeData;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;

public class ExtraChemicalTankUpgradeData extends ChemicalTankUpgradeData {
    public ExtraChemicalTankUpgradeData(HolderLookup.Provider provider, boolean redstone,
            IRedstoneControl.RedstoneControl controlType, ChemicalInventorySlot drainSlot,
            ChemicalInventorySlot fillSlot, GasMode dumping, IChemicalTank tank,
            List<ITileComponent> components, ProblemReporter.PathElement path) {
        super(provider, redstone, controlType, drainSlot, fillSlot, dumping, tank, components, path);
    }
}

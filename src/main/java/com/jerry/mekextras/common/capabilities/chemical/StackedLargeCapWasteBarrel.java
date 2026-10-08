package com.jerry.mekextras.common.capabilities.chemical;

import com.jerry.mekextras.common.capabilities.chemical.variable.ExtraVariableCapacityChemicalTank;
import com.jerry.mekextras.common.tile.TileEntityLargeCapRadioactiveWasteBarrel;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import org.jspecify.annotations.NullMarked;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.datamaps.chemical.attribute.ChemicalRadioactivity;
import mekanism.api.datamaps.chemical.attribute.IChemicalAttribute;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.util.WorldUtils;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NullMarked
public class StackedLargeCapWasteBarrel extends ExtraVariableCapacityChemicalTank {

    @SuppressWarnings("removal")
    private static final ChemicalAttributeValidator ATTRIBUTE_VALIDATOR = new ChemicalAttributeValidator() {

        @Override
        public boolean validate(IChemicalAttribute attr) {
            return attr instanceof ChemicalRadioactivity;
        }

        @Override
        public boolean process(Chemical chemical) {
            return chemical.isRadioactive();
        }
    };

    public static StackedLargeCapWasteBarrel create(TileEntityLargeCapRadioactiveWasteBarrel tile, @Nullable IContentsListener listener) {
        Objects.requireNonNull(tile, "Radioactive Waste Barrel tile entity cannot be null");
        return new StackedLargeCapWasteBarrel(tile, listener);
    }

    private final TileEntityLargeCapRadioactiveWasteBarrel tile;

    protected StackedLargeCapWasteBarrel(TileEntityLargeCapRadioactiveWasteBarrel tile, @Nullable IContentsListener listener) {
        super(tile.getTier().getStorage(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrue(), ATTRIBUTE_VALIDATOR, listener);
        this.tile = tile;
    }

    @Override
    public int insert(mekanism.api.chemical.ChemicalResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction, AutomationType automationType) {
        int inserted = super.insert(resource, amount, transaction, automationType);
        if (inserted < amount && resource().equals(resource)) {
            var above = WorldUtils.getTileEntity(TileEntityLargeCapRadioactiveWasteBarrel.class, tile.getLevel(), tile.getBlockPos().above());
            if (above != null) inserted += above.getGasTank().insert(resource, amount - inserted, transaction, AutomationType.EXTERNAL);
        }
        return inserted;
    }
}
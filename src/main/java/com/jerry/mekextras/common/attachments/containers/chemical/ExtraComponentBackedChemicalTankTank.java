package com.jerry.mekextras.common.attachments.containers.chemical;

import com.jerry.mekextras.common.item.block.ItemBlockExtraChemicalTank;
import com.jerry.mekextras.common.tier.CTTier;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.math.MathUtils;
import mekanism.common.component.containers.chemical.ComponentBackedChemicalTank;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ExtraComponentBackedChemicalTankTank extends ComponentBackedChemicalTank {
    public static ExtraComponentBackedChemicalTankTank create(ItemAccess access, int tankIndex) {
        if (!(access.getResource().getItem() instanceof ItemBlockExtraChemicalTank item)) {
            throw new IllegalStateException("Attached item must be an Extras chemical tank");
        }
        return new ExtraComponentBackedChemicalTankTank(access, tankIndex, item.getAdvancedTier());
    }
    private ExtraComponentBackedChemicalTankTank(ItemAccess access, int tankIndex, CTTier tier) {
        super(access, tankIndex, ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(),
            ConstantPredicates.alwaysTrue(), tier::getStorage, () -> MathUtils.clampToInt(tier.getOutput()), null);
    }
}

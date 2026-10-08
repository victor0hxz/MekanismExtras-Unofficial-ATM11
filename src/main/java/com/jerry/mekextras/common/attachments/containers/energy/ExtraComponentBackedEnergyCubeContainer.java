package com.jerry.mekextras.common.attachments.containers.energy;

import com.jerry.mekextras.common.item.block.ItemBlockExtraEnergyCube;
import com.jerry.mekextras.common.tier.ECTier;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.math.MathUtils;
import mekanism.common.component.containers.energy.ComponentBackedEnergyContainer;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ExtraComponentBackedEnergyCubeContainer extends ComponentBackedEnergyContainer {
    public static ExtraComponentBackedEnergyCubeContainer create(ItemAccess attachedAccess) {
        if (!(attachedAccess.getResource().getItem() instanceof ItemBlockExtraEnergyCube item)) {
            throw new IllegalStateException("Energy cube container attached to another item");
        }
        return new ExtraComponentBackedEnergyCubeContainer(attachedAccess, item.getAdvancedTier());
    }

    private ExtraComponentBackedEnergyCubeContainer(ItemAccess attachedAccess, ECTier tier) {
        super(attachedAccess, ConstantPredicates.alwaysTrue(), ConstantPredicates.alwaysTrue(),
            tier::getMaxEnergy, () -> MathUtils.clampToInt(tier.getOutput()));
    }
}

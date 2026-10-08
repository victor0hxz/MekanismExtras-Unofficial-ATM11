package com.jerry.mekextras.common.attachments.containers.fluid;
import com.jerry.mekextras.common.item.block.machine.ItemBlockExtraFluidTank;
import com.jerry.mekextras.common.tier.FTTier;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.component.containers.fluid.ComponentBackedFluidTank;
import net.neoforged.neoforge.transfer.access.ItemAccess;
public class ExtraComponentBackedFluidTankFluidTank extends ComponentBackedFluidTank {
    public static ExtraComponentBackedFluidTankFluidTank create(ItemAccess access, int tankIndex) {
        if (!(access.getResource().getItem() instanceof ItemBlockExtraFluidTank item)) throw new IllegalStateException("Attached item must be an Extras fluid tank");
        return new ExtraComponentBackedFluidTankFluidTank(access, tankIndex, item.getAdvancedTier());
    }
    private ExtraComponentBackedFluidTankFluidTank(ItemAccess access, int tankIndex, FTTier tier) {
        super(access, tankIndex, ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrue(), tier::getStorage, tier::getOutput);
    }
}
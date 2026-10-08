package com.jerry.mekextras.common.capabilities.fluid;

import com.jerry.mekextras.common.tile.TileEntityExtraFluidTank;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.transaction.ITransactionHelper;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.util.WorldUtils;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ExtraFluidTankFluidTank extends BasicFluidTank {
    private final TileEntityExtraFluidTank tile;

    public static ExtraFluidTankFluidTank create(TileEntityExtraFluidTank tile, @Nullable IContentsListener listener) {
        return new ExtraFluidTankFluidTank(tile, listener);
    }

    private ExtraFluidTankFluidTank(TileEntityExtraFluidTank tile, @Nullable IContentsListener listener) {
        super(tile.tier.getStorage(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(),
            ConstantPredicates.alwaysTrue(),
            ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(tile::getGameTime, tile.tier::getOutput),
            ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(tile::getGameTime, tile.tier::getOutput), listener);
        this.tile = tile;
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction, AutomationType automationType) {
        int inserted = super.insert(resource, amount, transaction, automationType);
        // Every tank participates in the same transaction, including simulation and rollback.
        if (inserted < amount && resource().equals(resource)) {
            TileEntityExtraFluidTank above = WorldUtils.getTileEntity(TileEntityExtraFluidTank.class,
                tile.getLevel(), tile.getBlockPos().above());
            if (above != null) {
                inserted += above.fluidTank.insert(resource, amount - inserted, transaction, AutomationType.EXTERNAL);
            }
        }
        return inserted;
    }
}

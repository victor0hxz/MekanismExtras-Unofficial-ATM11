package com.jerry.mekextras.common.capabilities.energy;

import com.jerry.mekextras.common.tile.TileEntityExtraEnergyCube;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.math.MathUtils;
import mekanism.api.transaction.ITransactionHelper;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import org.jspecify.annotations.Nullable;

public class ExtraEnergyCubeEnergyContainer extends BasicEnergyContainer {
    public static ExtraEnergyCubeEnergyContainer create(TileEntityExtraEnergyCube tile, @Nullable IContentsListener listener) {
        return new ExtraEnergyCubeEnergyContainer(tile, listener);
    }

    private ExtraEnergyCubeEnergyContainer(TileEntityExtraEnergyCube tile, @Nullable IContentsListener listener) {
        super(tile.getAdvanceTier().getMaxEnergy(), ConstantPredicates.alwaysTrue(), ConstantPredicates.alwaysTrue(),
            ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(tile::getGameTime, () -> MathUtils.clampToInt(tile.getAdvanceTier().getOutput())),
            ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(tile::getGameTime, () -> MathUtils.clampToInt(tile.getAdvanceTier().getOutput())), listener);
    }
}

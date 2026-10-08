package com.jerry.mekextras.common.capabilities.chemical;

import com.jerry.mekextras.common.tile.TileEntityExtraChemicalTank;
import com.jerry.mekextras.common.tier.CTTier;
import java.util.function.IntSupplier;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.math.MathUtils;
import mekanism.api.transaction.ITransactionHelper;
import mekanism.api.transaction.RateLimitTracker;
import org.jspecify.annotations.Nullable;

public class ExtraChemicalTankChemicalTank extends BasicChemicalTank {
    public static ExtraChemicalTankChemicalTank create(TileEntityExtraChemicalTank tile, @Nullable IContentsListener listener) {
        CTTier tier = tile.getTier();
        IntSupplier rate = () -> MathUtils.clampToInt(tier.getOutput());
        return new ExtraChemicalTankChemicalTank(tier,
            ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(tile::getGameTime, rate),
            ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(tile::getGameTime, rate), listener);
    }
    private ExtraChemicalTankChemicalTank(CTTier tier, @Nullable RateLimitTracker insertion,
                                          @Nullable RateLimitTracker extraction, @Nullable IContentsListener listener) {
        super(tier.getStorage(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(),
            ConstantPredicates.alwaysTrue(), insertion, extraction, null, listener);
    }
}

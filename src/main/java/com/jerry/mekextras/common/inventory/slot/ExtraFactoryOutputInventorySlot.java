package com.jerry.mekextras.common.inventory.slot;

import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import org.jspecify.annotations.Nullable;

public class ExtraFactoryOutputInventorySlot extends BasicInventorySlot {
    private long stackMultiplier;
    @Override public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        long capacity = super.capacityAsLong(resource);
        return resource.isEmpty() ? capacity : Math.min(capacity, (long)resource.getMaxStackSize() * stackMultiplier);
    }

    public static ExtraFactoryOutputInventorySlot at(TileEntityExtraFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        return new ExtraFactoryOutputInventorySlot(factory, listener, x, y);
    }

    private ExtraFactoryOutputInventorySlot(TileEntityExtraFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        super(Math.min(Integer.MAX_VALUE, 99L * (8L << factory.tier.ordinal())),
            ConstantPredicates.alwaysTrueBi(), ConstantPredicates.internalOnly(), ConstantPredicates.alwaysTrue(), null, null, listener, x, y);
        obeyStackLimit = false;
        stackMultiplier = 8L << factory.tier.ordinal();
        setSlotType(ContainerSlotType.OUTPUT);
    }
}

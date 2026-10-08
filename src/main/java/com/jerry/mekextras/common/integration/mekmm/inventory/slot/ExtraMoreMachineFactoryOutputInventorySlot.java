package com.jerry.mekextras.common.integration.mekmm.inventory.slot;

import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import org.jspecify.annotations.Nullable;

public class ExtraMoreMachineFactoryOutputInventorySlot extends BasicInventorySlot {
    private long stackMultiplier;
    @Override public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        long capacity = super.capacityAsLong(resource);
        return resource.isEmpty() ? capacity : Math.min(capacity, (long)resource.getMaxStackSize() * stackMultiplier);
    }

    public static ExtraMoreMachineFactoryOutputInventorySlot at(TileEntityExtraMoreMachineFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        return new ExtraMoreMachineFactoryOutputInventorySlot(factory, listener, x, y);
    }

    private ExtraMoreMachineFactoryOutputInventorySlot(TileEntityExtraMoreMachineFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        super(Math.min(Integer.MAX_VALUE, 99L * (8L << factory.tier.ordinal())),
            ConstantPredicates.alwaysTrueBi(), ConstantPredicates.internalOnly(), ConstantPredicates.alwaysTrue(), null, null, listener, x, y);
        obeyStackLimit = false;
        stackMultiplier = 8L << factory.tier.ordinal();
        setSlotType(ContainerSlotType.OUTPUT);
    }
}

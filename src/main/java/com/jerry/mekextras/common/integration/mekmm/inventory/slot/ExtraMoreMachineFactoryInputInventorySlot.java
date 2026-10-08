package com.jerry.mekextras.common.integration.mekmm.inventory.slot;

import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;

import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;

import net.minecraft.world.item.Item;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public class ExtraMoreMachineFactoryInputInventorySlot extends InputInventorySlot {
    private long stackMultiplier;
    @Override public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        long capacity = super.capacityAsLong(resource);
        return resource.isEmpty() ? capacity : Math.min(capacity, (long)resource.getMaxStackSize() * stackMultiplier);
    }


    public static ExtraMoreMachineFactoryInputInventorySlot create(TileEntityExtraMoreMachineFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener,
                                                              int x, int y) {
        return create(factory, process, outputSlot, null, listener, x, y);
    }

    public static ExtraMoreMachineFactoryInputInventorySlot create(TileEntityExtraMoreMachineFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot,
                                                              @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputSlot, "Primary output slot cannot be null");
        return new ExtraMoreMachineFactoryInputInventorySlot(factory, process, outputSlot, secondaryOutputSlot, listener, x, y);
    }

    private ExtraMoreMachineFactoryInputInventorySlot(TileEntityExtraMoreMachineFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot,
                                                 @Nullable IContentsListener listener, int x, int y) {
        super(Math.min(Integer.MAX_VALUE, (long) Item.ABSOLUTE_MAX_STACK_SIZE * (8L << factory.tier.ordinal())), (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputSlot, secondaryOutputSlot, false),
                factory::isValidInputItem, null, null, listener, x, y);
        obeyStackLimit = false;
        stackMultiplier = 8L << factory.tier.ordinal();
    }
}

package com.jerry.mekextras.common.inventory.slot;
import mekanism.common.inventory.slot.InputInventorySlot;

import java.util.Objects;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import org.jspecify.annotations.Nullable;

public class ExtraFactoryInputInventorySlot extends InputInventorySlot {
    private long stackMultiplier;
    @Override public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        long capacity = super.capacityAsLong(resource);
        return resource.isEmpty() ? capacity : Math.min(capacity, (long)resource.getMaxStackSize() * stackMultiplier);
    }

   public static ExtraFactoryInputInventorySlot create(
      TileEntityExtraFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener, int x, int y
   ) {
      return create(factory, process, outputSlot, null, listener, x, y);
   }

   public static ExtraFactoryInputInventorySlot create(
      TileEntityExtraFactory<?> factory,
      int process,
      IInventorySlot outputSlot,
      @Nullable IInventorySlot secondaryOutputSlot,
      @Nullable IContentsListener listener,
      int x,
      int y
   ) {
      Objects.requireNonNull(factory, "Factory cannot be null");
      Objects.requireNonNull(outputSlot, "Primary output slot cannot be null");
      return new ExtraFactoryInputInventorySlot(factory, process, outputSlot, secondaryOutputSlot, listener, x, y);
   }

   private ExtraFactoryInputInventorySlot(
      TileEntityExtraFactory<?> factory,
      int process,
      IInventorySlot outputSlot,
      @Nullable IInventorySlot secondaryOutputSlot,
      @Nullable IContentsListener listener,
      int x,
      int y
   ) {
      super(
         Math.min(Integer.MAX_VALUE, 99L * (8L << factory.tier.ordinal())),
         (itemType, var5) -> factory.isItemValidForSlot(itemType) && factory.inputProducesOutput(process, itemType, outputSlot, secondaryOutputSlot, false),
         factory::isValidInputItem,
         null,
         null,
         listener,
         x,
         y
      );
        obeyStackLimit = false;
        stackMultiplier = 8L << factory.tier.ordinal();
   }
}

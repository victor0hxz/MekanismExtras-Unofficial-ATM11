package com.jerry.mekextras.common.inventory.slot;

import com.jerry.mekextras.common.attachments.containers.item.ExtraComponentBackedBinInventorySlot;
import com.jerry.mekextras.common.item.block.ItemBlockExtraBin;
import com.jerry.mekextras.common.tier.BTier;
import java.util.Objects;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.resource.IResourceContainer;
import mekanism.api.resource.ResourceContainerWrapper;
import mekanism.common.component.containers.item.ComponentBackedBinInventorySlot;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.BinInventorySlot;
import mekanism.common.item.block.ItemBlockBin;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ExtraBinInventorySlot extends BasicInventorySlot {
    public static final Predicate<ItemResource> validator = resource ->
        !(resource.getItem() instanceof ItemBlockExtraBin) && !(resource.getItem() instanceof ItemBlockBin);
    private final LockJournal lockJournal = new LockJournal();

    public static @Nullable ExtraComponentBackedBinInventorySlot getForStack(ItemAccess access) {
        if (!access.getResource().isEmpty() && access.getResource().getItem() instanceof ItemBlockExtraBin
            && ContainerType.ITEM.createContainer(access, 0) instanceof ExtraComponentBackedBinInventorySlot slot) {
            return slot;
        }
        return null;
    }

    public static ExtraBinInventorySlot create(@Nullable IContentsListener listener, BTier tier) {
        return new ExtraBinInventorySlot(listener, Objects.requireNonNull(tier));
    }

    private ExtraBinInventorySlot(@Nullable IContentsListener listener, BTier tier) {
        super(tier.getStorage(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), validator, null, null, listener, 0, 0);
        obeyStackLimit = false;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction, AutomationType automationType) {
        if (isEmpty() && isLocked() && !getLockType().equals(resource)) return 0;
        return super.insert(resource, amount, transaction, automationType);
    }

    @Override
    public @Nullable InventoryContainerSlot createContainerSlot() { return null; }

    public boolean setLocked(boolean lock) {
        if (isLocked() == lock || lock && isEmpty()) return false;
        setLockType(lock ? resource() : ItemResource.EMPTY, null);
        return true;
    }

    public void setLockType(ItemResource resource, @Nullable TransactionContext transaction) {
        lockJournal.set(resource, transaction);
    }
    public ItemResource getLockType() { return lockJournal.type; }
    public boolean isLocked() { return !getLockType().isEmpty(); }
    public ItemResource getBinItemType() { return isLocked() ? getLockType() : resource(); }

    @Override
    public void copyContents(IResourceContainer<ItemResource> other, @Nullable TransactionContext transaction) {
        if (other instanceof ResourceContainerWrapper<ItemResource, ?> wrapper) other = wrapper.getInternal();
        super.copyContents(other, transaction);
        if (other instanceof ExtraBinInventorySlot slot) setLockType(slot.getLockType(), transaction);
        else if (other instanceof BinInventorySlot slot) setLockType(slot.getLockType(), transaction);
        else if (other instanceof ExtraComponentBackedBinInventorySlot slot) setLockType(slot.getLockType(), transaction);
        else if (other instanceof ComponentBackedBinInventorySlot slot) setLockType(slot.getLockType(), transaction);
    }

    @Override
    public void serialize(ValueOutput output) {
        super.serialize(output);
        if (isLocked()) output.store("lock_type", ItemResource.CODEC, getLockType());
    }

    @Override
    public void deserialize(ValueInput input) {
        setLockType(input.read("lock_type", ItemResource.CODEC).orElse(ItemResource.EMPTY), null);
        super.deserialize(input);
    }

    private static class LockJournal extends SnapshotJournal<ItemResource> {
        private ItemResource type = ItemResource.EMPTY;
        void set(ItemResource type, @Nullable TransactionContext transaction) {
            if (transaction != null) updateSnapshots(transaction);
            this.type = type;
        }
        @Override protected ItemResource createSnapshot() { return type; }
        @Override protected void revertToSnapshot(ItemResource snapshot) { type = snapshot; }
    }
}

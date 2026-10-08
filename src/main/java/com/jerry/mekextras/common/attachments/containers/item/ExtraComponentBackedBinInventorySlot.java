package com.jerry.mekextras.common.attachments.containers.item;

import com.jerry.mekextras.common.inventory.slot.ExtraBinInventorySlot;
import com.jerry.mekextras.common.item.block.ItemBlockExtraBin;
import com.jerry.mekextras.common.tier.BTier;
import mekanism.api.AutomationType;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.resource.IResourceContainer;
import mekanism.api.resource.ResourceContainerWrapper;
import mekanism.common.component.LockData;
import mekanism.common.component.containers.item.ComponentBackedBinInventorySlot;
import mekanism.common.component.containers.item.ComponentBackedInventorySlot;
import mekanism.common.component.containers.resource.AttachedResources;
import mekanism.common.inventory.slot.BinInventorySlot;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.ItemAccessUtils;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ExtraComponentBackedBinInventorySlot extends ComponentBackedInventorySlot {
    public static ExtraComponentBackedBinInventorySlot create(ItemAccess access, int slotIndex) {
        if (!(access.getResource().getItem() instanceof ItemBlockExtraBin item)) {
            throw new IllegalStateException("Attached item must be an Extras bin");
        }
        return new ExtraComponentBackedBinInventorySlot(access, slotIndex, item.getAdvancedTier());
    }
    private ExtraComponentBackedBinInventorySlot(ItemAccess access, int slotIndex, BTier tier) {
        super(access, slotIndex, ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), ExtraBinInventorySlot.validator, false, tier::getStorage);
    }
    @Override
    protected int insert(AttachedResources<ItemResource> attached, ItemResource currentType, long currentAmount,
                         long capacity, ItemResource resource, int amount, TransactionContext transaction, AutomationType automationType) {
        ItemResource lock = getLockType();
        if (currentType.isEmpty() && !lock.isEmpty() && !lock.equals(resource)) return 0;
        return super.insert(attached, currentType, currentAmount, capacity, resource, amount, transaction, automationType);
    }
    public boolean setLockType(ItemResource lock, @Nullable TransactionContext transaction) {
        ItemResource item = attachedAccess.getResource();
        if (item.isEmpty()) return false;
        ItemResource updated = lock.isEmpty() ? item.without(MekanismDataComponents.LOCK)
            : item.with(MekanismDataComponents.LOCK, LockData.create(lock));
        return ItemAccessUtils.exchange(attachedAccess, updated, transaction);
    }
    public ItemResource getLockType() {
        return attachedAccess.getResource().getOrDefault(MekanismDataComponents.LOCK, LockData.EMPTY).lock();
    }
    @Override
    public void copyContents(IResourceContainer<ItemResource> other, @Nullable TransactionContext transaction) {
        if (other instanceof ResourceContainerWrapper<ItemResource, ?> wrapper) other = wrapper.getInternal();
        super.copyContents(other, transaction);
        if (other instanceof ExtraBinInventorySlot slot) setLockType(slot.getLockType(), transaction);
        else if (other instanceof BinInventorySlot slot) setLockType(slot.getLockType(), transaction);
        else if (other instanceof ExtraComponentBackedBinInventorySlot slot) setLockType(slot.getLockType(), transaction);
        else if (other instanceof ComponentBackedBinInventorySlot slot) setLockType(slot.getLockType(), transaction);
    }
    @Override public void serialize(ValueOutput output) {
        super.serialize(output);
        ItemResource lock = getLockType();
        if (!lock.isEmpty()) output.store("lock_type", ItemResource.CODEC, lock);
    }
    @Override public void deserialize(ValueInput input) {
        setLockType(input.read("lock_type", ItemResource.CODEC).orElse(ItemResource.EMPTY), null);
        super.deserialize(input);
    }
}

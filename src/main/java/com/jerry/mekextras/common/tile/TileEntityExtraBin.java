package com.jerry.mekextras.common.tile;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.inventory.slot.ExtraBinInventorySlot;
import com.jerry.mekextras.common.tier.BTier;
import com.jerry.mekextras.common.upgrade.ExtraBinUpgradeData;

import mekanism.api.Action;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.common.component.LockData;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.BinInventorySlot;
import mekanism.common.lib.inventory.HandlerTransitRequest;
import mekanism.common.lib.inventory.TransitRequest;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.base.CapabilityTileEntity;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.upgrade.BinUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityExtraBin extends TileEntityMekanism implements IConfigurable {

    @Nullable
    private BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> targetInventory;
    public int addTicks = 0;
    public int removeTicks = 0;
    private int delayTicks;
    private boolean needsSync;
    private BTier tier;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getStored", docPlaceholder = "bin")
    ExtraBinInventorySlot binSlot;

    public TileEntityExtraBin(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        tier = ExtraAttribute.getAdvancedTier(blockProvider, BTier.class);
        super(blockProvider, pos, state);
        delaySupplier = NO_DELAY;
    }

    @NotNull
    @Override
    protected mekanism.common.capabilities.holder.container.IContainerHolder<mekanism.api.inventory.IInventorySlot> getInitialInventory(IContentsListener listener) {
        mekanism.common.capabilities.holder.container.MekContainerHelper<mekanism.api.inventory.IInventorySlot> builder = mekanism.common.capabilities.holder.container.MekContainerHelper.forSide(this::getDirection);
        builder.addContainer(binSlot = ExtraBinInventorySlot.create(listener, tier));
        return builder.build();
    }

    public BTier getTier() {
        return tier;
    }

    public ExtraBinInventorySlot getBinSlot() {
        return binSlot;
    }

    @Override
    protected boolean onUpdateServer(net.minecraft.server.level.ServerLevel serverLevel) {
        boolean sendUpdatePacket = super.onUpdateServer(serverLevel);
        addTicks = Math.max(0, addTicks - 1);
        removeTicks = Math.max(0, removeTicks - 1);
        delayTicks = Math.max(0, delayTicks - 1);
        if (delayTicks == 0) {
            if (getActive()) {
                // Note: We can't just pass "this" and have to instead look up the capability to make sure we respect
                // any sidedness
                // we short circuit looking it up from the world though, and just query the provider we add to the tile
                // directly
                ResourceHandler<ItemResource> capability = CapabilityTileEntity.ITEM_HANDLER_PROVIDER.getCapability(this, Direction.DOWN);
                HandlerTransitRequest request = new HandlerTransitRequest(capability);
                ItemResource stored = binSlot.resource();
                if (!stored.isEmpty()) {
                    request.addItem(stored, Math.min(binSlot.amountAsInt(), stored.getMaxStackSize()), 0);
                    if (targetInventory == null) {
                        targetInventory = Capabilities.ITEM.createCache(serverLevel, getBlockPos().below(), Direction.UP);
                    }
                    try (Transaction transaction = Transaction.openRoot()) {
                        TransitRequest.TransitResponse response = request.eject(this, targetInventory.getCapability(), 1, null, transaction);
                        if (response.useAll(transaction)) transaction.commit();
                    }
                }
                delayTicks = MekanismUtils.TICKS_PER_HALF_SECOND;
            }
        } else {
            delayTicks--;
        }
        if (needsSync) {
            sendUpdatePacket = true;
            needsSync = false;
        }
        return sendUpdatePacket;
    }

    @Override
    public InteractionResult onSneakRightClick(Level world, Player player) {
        setActive(!getActive());
        if (world != null) {
            world.playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Level world, Player player) {
        return InteractionResult.PASS;
    }

    public boolean toggleLock() {
        return setLocked(!binSlot.isLocked());
    }

    public boolean setLocked(boolean isLocked) {
        if (binSlot.setLocked(isLocked)) {
            if (getLevel() != null && !isRemote()) {
                needsSync = true;
                markForSave();
                getLevel().playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public void parseUpgradeData(IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof ExtraBinUpgradeData data) {
            redstone = data.redstone();
            binSlot.copyContents(data.binSlot(), transaction);
        } else if (upgradeData instanceof BinUpgradeData data) {
            redstone = data.redstone();
            binSlot.copyContents(data.binSlot(), transaction);
        } else {
            super.parseUpgradeData(upgradeData, provider, transaction);
        }
    }

    @NotNull
    @Override
    public ExtraBinUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new ExtraBinUpgradeData(redstone, getBinSlot());
    }

    @Override
    public void onContentsChanged() {
        super.onContentsChanged();
        if (level != null && !isRemote()) {
            needsSync = true;
        }
    }

    @NotNull
    @Override
    public void writeReducedUpdatedTag(ValueOutput output) {
        super.writeReducedUpdatedTag(output);
        output.putChild(SerializationConstants.ITEM, binSlot);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        input.readChild(SerializationConstants.ITEM, binSlot);
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        // Note: In theory doing this before super doesn't matter, but we want to make sure that the lock is set before
        // setting the data on the item just for good measure
        builder.set(MekanismDataComponents.LOCK, LockData.create(binSlot.getLockType()));
        super.collectImplicitComponents(builder);
    }

    @Override
    protected void applyImplicitComponents(@NotNull net.minecraft.core.component.DataComponentGetter input) {
        // Apply the lock before processing the stored data
        binSlot.setLockType(input.getOrDefault(MekanismDataComponents.LOCK, LockData.EMPTY).lock(), null);
        super.applyImplicitComponents(input);
    }

    // Methods relating to IComputerTile
    @ComputerMethod(methodDescription = "Get the maximum number of items the bin can contain.")
    long getCapacity() {
        return binSlot.capacityAsLong(binSlot.resource());
    }

    @ComputerMethod(methodDescription = "If true, the Bin is locked to a particular item type.")
    boolean isLocked() {
        return binSlot.isLocked();
    }

    @ComputerMethod(methodDescription = "Get the type of item the Bin is locked to (or Air if not locked)")
    ItemStack getLock() {
        return binSlot.getLockType().toStack();
    }

    @ComputerMethod(methodDescription = "Lock the Bin to the currently stored item type. The Bin must not be creative, empty, or already locked")
    void lock() throws ComputerException {
        if (binSlot.isEmpty()) {
            throw new ComputerException("Empty bins cannot be locked!");
        } else if (!setLocked(true)) {
            throw new ComputerException("This bin is already locked!");
        }
    }

    @ComputerMethod(methodDescription = "Unlock the Bin's fixed item type. The Bin must not be creative, or already unlocked")
    void unlock() throws ComputerException {
        if (!setLocked(false)) {
            throw new ComputerException("This bin is not locked!");
        }
    }
    // End methods IComputerTile
}

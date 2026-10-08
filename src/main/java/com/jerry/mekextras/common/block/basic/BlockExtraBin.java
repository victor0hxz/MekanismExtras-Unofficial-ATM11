package com.jerry.mekextras.common.block.basic;

import mekanism.api.AutomationType;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import com.jerry.mekextras.common.inventory.slot.ExtraBinInventorySlot;
import mekanism.common.lib.transaction.TransactionHelper;
import com.jerry.mekextras.common.tile.TileEntityExtraBin;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class BlockExtraBin
extends BlockTile<TileEntityExtraBin, BlockTypeTile<TileEntityExtraBin>> {
    public BlockExtraBin(BlockTypeTile<TileEntityExtraBin> type, BlockBehaviour.Properties properties) {
        super(type, properties);
    }

    protected void attack(BlockState state, Level world, BlockPos pos, Player player) {
        ExtraBinInventorySlot binSlot;
        BlockHitResult mop;
        TileEntityExtraBin bin;
        if (!world.isClientSide() && (bin = WorldUtils.getTileEntity(TileEntityExtraBin.class, (BlockGetter)world, pos)) != null && (mop = MekanismUtils.rayTrace(player)).getType() != HitResult.Type.MISS && mop.getDirection() == bin.getDirection() && !(binSlot = bin.getBinSlot()).isEmpty() && bin.removeTicks == 0) {
            bin.removeTicks = 3;
            ItemResource binItemType = binSlot.getBinItemType();
            try (Transaction transaction = TransactionHelper.openTransactionSafe();){
                int extracted = binSlot.extract(binItemType, player.isShiftKeyDown() ? binItemType.getMaxStackSize() : 1, (TransactionContext)transaction, AutomationType.MANUAL);
                if (extracted > 0) {
                    PlayerInventoryWrapper playerInv = PlayerInventoryWrapper.of((Player)player);
                    int inserted = playerInv.insert(binItemType, extracted, (TransactionContext)transaction);
                    transaction.commit();
                    if (inserted < extracted) {
                        Vec3 dropPos = Vec3.upFromBottomCenterOf((Vec3i)pos.relative(bin.getDirection()), (double)0.3);
                        world.addFreshEntity((Entity)new ItemEntity(world, dropPos.x(), dropPos.y(), dropPos.z(), binItemType.toStack(extracted - inserted), 0.0, 0.0, 0.0));
                    } else {
                        world.playSound(null, (double)((float)pos.getX() + 0.5f), (double)((float)pos.getY() + 0.5f), (double)((float)pos.getZ() + 0.5f), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    }
                }
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        TileEntityExtraBin bin = WorldUtils.getTileEntity(TileEntityExtraBin.class, (BlockGetter)world, pos);
        if (bin == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        InteractionResult wrenchResult = bin.tryWrench(world, state, player, stack).getInteractionResult();
        if (wrenchResult != InteractionResult.PASS) {
            return wrenchResult;
        }
        if (hit.getDirection() != bin.getDirection()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            return bin.toggleLock() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        if (world.isClientSide()) return InteractionResult.SUCCESS_SERVER;
        ExtraBinInventorySlot binSlot = bin.getBinSlot();
        ItemResource binItemType = binSlot.getBinItemType();
        long binMaxSize = binSlot.capacityAsLong(binItemType);
        if (binSlot.amountAsLong() >= binMaxSize) return InteractionResult.SUCCESS_SERVER;
        if (bin.addTicks == 0) {
            if (!stack.isEmpty()) {
                try (Transaction transaction = TransactionHelper.openTransactionSafe();){
                    ItemResource resource = ItemResource.of((ItemStack)stack);
                    int inserted = binSlot.insert(resource, stack.count(), (TransactionContext)transaction, AutomationType.MANUAL);
                    if (PlayerInventoryWrapper.of((Player)player).getHandSlot(hand).extract(resource, inserted, (TransactionContext)transaction) != inserted) return InteractionResult.SUCCESS_SERVER;
                    bin.addTicks = 5;
                    if (inserted <= 0) return InteractionResult.SUCCESS_SERVER;
                    transaction.commit();
                    InteractionResult.Success success = InteractionResult.SUCCESS_SERVER;
                    return success;
                }
            } else {
                if (binItemType.isEmpty()) return InteractionResult.SUCCESS_SERVER;
                bin.addTicks = 5;
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (bin.addTicks <= 0 || binItemType.isEmpty()) return InteractionResult.SUCCESS_SERVER;
        try (Transaction transaction = TransactionHelper.openTransactionSafe();){
            boolean added = false;
            PlayerInventoryWrapper playerInv = PlayerInventoryWrapper.of((Player)player);
            ResourceHandler<ItemResource> playerInvHandler = playerInv.getMainSlots();
            int size = playerInvHandler.size();
            for (int slot = 0; slot < size; ++slot) {
                ItemResource itemType = playerInvHandler.getResource(slot);
                if (itemType.isEmpty()) continue;
                try (Transaction transfer = Transaction.open(transaction)) {
                    int inserted = binSlot.insert(itemType, playerInvHandler.getAmountAsInt(slot), transfer, AutomationType.MANUAL);
                    if (inserted > 0 && playerInvHandler.extract(slot, itemType, inserted, transfer) == inserted) {
                        transfer.commit();
                        added = true;
                    }
                }
                if (binSlot.amountAsLong() == binMaxSize) break;
            }
            if (!added) return InteractionResult.SUCCESS_SERVER;
            transaction.commit();
            bin.addTicks = 5;
            player.containerMenu.sendAllDataToRemote();
            return InteractionResult.SUCCESS_SERVER;
        }
    }
}

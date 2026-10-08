package com.jerry.mekextras.common.recipe.bin;

import mekanism.api.AutomationType;
import com.jerry.mekextras.common.attachments.containers.item.ExtraComponentBackedBinInventorySlot;
import com.jerry.mekextras.common.item.block.ItemBlockExtraBin;
import mekanism.common.lib.transaction.TransactionHelper;
import com.jerry.mekextras.common.recipe.bin.ExtraBinRecipe;
import mekanism.common.registries.MekanismDataComponents;
import com.jerry.mekextras.common.registries.ExtraRecipeSerializersInternal;
import mekanism.common.util.ItemAccessUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ExtraBinInsertRecipe
extends ExtraBinRecipe {
    public static final ExtraBinInsertRecipe INSTANCE = new ExtraBinInsertRecipe();

    public boolean matches(CraftingInput inv, Level world) {
        ItemResource binType = ItemResource.EMPTY;
        ItemResource foundType = ItemResource.EMPTY;
        int slots = inv.size();
        for (int i = 0; i < slots; ++i) {
            ItemStack stackInSlot = inv.getItem(i);
            if (stackInSlot.isEmpty()) continue;
            if (stackInSlot.getItem() instanceof ItemBlockExtraBin) {
                if (!binType.isEmpty() || stackInSlot.count() > 1) {
                    return false;
                }
                binType = ItemResource.of((ItemStack)stackInSlot);
                continue;
            }
            if (foundType.isEmpty()) {
                foundType = ItemResource.of((ItemStack)stackInSlot);
                continue;
            }
            if (foundType.matches(stackInSlot)) continue;
            return false;
        }
        if (binType.isEmpty() || foundType.isEmpty()) {
            return false;
        }
        ExtraComponentBackedBinInventorySlot slot = ExtraBinInsertRecipe.convertToSlot(ItemAccessUtils.sideEffectFreeAccess((TypedInstance<Item>)binType));
        try (Transaction simulation = TransactionHelper.openTransactionSafe();){
            boolean bl = slot.insert(foundType, 1, (TransactionContext)simulation, AutomationType.MANUAL) > 0;
            return bl;
        }
    }

    public ItemStack assemble(CraftingInput inv) {
        ItemResource binType = ItemResource.EMPTY;
        ItemResource foundType = ItemResource.EMPTY;
        int toInsert = 0;
        int slots = inv.size();
        for (int i = 0; i < slots; ++i) {
            ItemStack stackInSlot = inv.getItem(i);
            if (stackInSlot.isEmpty()) continue;
            if (stackInSlot.getItem() instanceof ItemBlockExtraBin) {
                if (!binType.isEmpty() || stackInSlot.count() > 1) {
                    return ItemStack.EMPTY;
                }
                binType = ItemResource.of((ItemStack)stackInSlot);
                continue;
            }
            if (foundType.isEmpty()) {
                foundType = ItemResource.of((ItemStack)stackInSlot);
            } else if (!foundType.matches(stackInSlot)) {
                return ItemStack.EMPTY;
            }
            ++toInsert;
        }
        if (binType.isEmpty() || foundType.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemAccess binAccess = ItemAccessUtils.sideEffectFreeAccess((TypedInstance<Item>)binType);
        ExtraComponentBackedBinInventorySlot slot = ExtraBinInsertRecipe.convertToSlot(binAccess);
        try (Transaction transaction = TransactionHelper.openTransactionSafe();){
            int inserted = slot.insert(foundType, toInsert, (TransactionContext)transaction, AutomationType.MANUAL);
            if (inserted == 0) {
                ItemStack itemStack = ItemStack.EMPTY;
                return itemStack;
            }
            if (inserted == toInsert) {
                ItemAccessUtils.exchange(binAccess, binAccess.getResource().with(MekanismDataComponents.FROM_RECIPE, true), (TransactionContext)transaction);
            }
            transaction.commit();
            ItemStack itemStack = ItemAccessUtils.asStack(binAccess);
            return itemStack;
        }
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        int slots = inv.size();
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(slots, ItemStack.EMPTY);
        ItemResource binType = ItemResource.EMPTY;
        ItemResource foundType = ItemResource.EMPTY;
        boolean[] foundSlots = new boolean[slots];
        for (int i = 0; i < slots; ++i) {
            ItemStack stackInSlot = inv.getItem(i);
            if (stackInSlot.isEmpty()) continue;
            if (stackInSlot.getItem() instanceof ItemBlockExtraBin) {
                if (!binType.isEmpty() || stackInSlot.count() > 1) {
                    return remainingItems;
                }
                binType = ItemResource.of((ItemStack)stackInSlot);
                continue;
            }
            if (foundType.isEmpty()) {
                foundType = ItemResource.of((ItemStack)stackInSlot);
            } else if (!foundType.matches(stackInSlot)) {
                return remainingItems;
            }
            foundSlots[i] = true;
        }
        if (binType.isEmpty() || foundType.isEmpty()) {
            return remainingItems;
        }
        ItemAccess binAccess = ItemAccessUtils.sideEffectFreeAccess((TypedInstance<Item>)binType);
        ExtraComponentBackedBinInventorySlot slot = ExtraBinInsertRecipe.convertToSlot(binAccess);
        try (Transaction transaction = TransactionHelper.openTransactionSafe();){
            for (int i = 0; i < foundSlots.length; ++i) {
                int inserted;
                if (!foundSlots[i] || (inserted = slot.insert(foundType, 1, (TransactionContext)transaction, AutomationType.MANUAL)) != 0) continue;
                remainingItems.set(i, foundType.toStack());
            }
            transaction.commit();
        }
        return remainingItems;
    }

    public RecipeSerializer<ExtraBinInsertRecipe> getSerializer() {
        return ExtraRecipeSerializersInternal.BIN_INSERT.get();
    }

    public static void onCrafting(PlayerEvent.ItemCraftedEvent event) {
        ExtraComponentBackedBinInventorySlot slot;
        ItemResource storedResource;
        Boolean fromRecipe;
        ItemStack result = event.getCrafting();
        if (!result.isEmpty() && result.getItem() instanceof ItemBlockExtraBin && result.count() == 1 && (fromRecipe = (Boolean)result.remove(MekanismDataComponents.FROM_RECIPE)) != null && fromRecipe.booleanValue() && !(storedResource = (ItemResource)(slot = ExtraBinInsertRecipe.convertToSlot(ItemAccess.forStack((ItemStack)result))).resource()).isEmpty()) {
            try (Transaction transaction = TransactionHelper.openTransactionSafe();){
                Container craftingMatrix = event.getInventory();
                int slots = craftingMatrix.getContainerSize();
                for (int i = 0; i < slots; ++i) {
                    ItemStack stack = craftingMatrix.getItem(i);
                    if (stack.count() <= 1 || !storedResource.matches(stack)) continue;
                    int toInsert = stack.count() - 1;
                    int inserted = slot.insert(storedResource, toInsert, (TransactionContext)transaction, AutomationType.MANUAL);
                    if (inserted == toInsert) {
                        craftingMatrix.setItem(i, storedResource.toStack());
                        continue;
                    }
                    if (inserted >= toInsert) continue;
                    craftingMatrix.setItem(i, storedResource.toStack(toInsert + 1 - inserted));
                }
                transaction.commit();
            }
        }
    }
}

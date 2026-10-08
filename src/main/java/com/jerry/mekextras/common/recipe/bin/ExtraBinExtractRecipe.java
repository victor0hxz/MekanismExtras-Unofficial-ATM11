package com.jerry.mekextras.common.recipe.bin;

import com.google.common.primitives.Ints;
import mekanism.api.AutomationType;
import mekanism.api.resource.LargeResourceStack;
import com.jerry.mekextras.common.attachments.containers.item.ExtraComponentBackedBinInventorySlot;
import com.jerry.mekextras.common.item.block.ItemBlockExtraBin;
import mekanism.common.lib.transaction.TransactionHelper;
import com.jerry.mekextras.common.recipe.bin.ExtraBinRecipe;
import com.jerry.mekextras.common.registries.ExtraRecipeSerializersInternal;
import mekanism.common.util.ItemAccessUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ExtraBinExtractRecipe
extends ExtraBinRecipe {
    public static final ExtraBinExtractRecipe INSTANCE = new ExtraBinExtractRecipe();

    public boolean matches(CraftingInput inv, Level world) {
        ItemResource binData = this.findBinData(inv);
        if (binData.isEmpty()) {
            return false;
        }
        return !ExtraBinExtractRecipe.convertToSlot(ItemAccessUtils.sideEffectFreeAccess((TypedInstance<Item>)binData)).isEmpty();
    }

    public ItemStack assemble(CraftingInput inv) {
        ItemResource binData = this.findBinData(inv);
        if (binData.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ExtraComponentBackedBinInventorySlot slot = ExtraBinExtractRecipe.convertToSlot(ItemAccessUtils.sideEffectFreeAccess((TypedInstance<Item>)binData));
        LargeResourceStack<ItemResource> stack = slot.asStack();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemResource stored = (ItemResource)stack.resource();
        int toExtract = Math.min(Ints.saturatedCast((long)stack.amount()), stored.getMaxStackSize());
        return stored.toStack(toExtract);
    }

    private ItemResource findBinData(CraftingInput inv) {
        ItemResource binData = ItemResource.EMPTY;
        int slots = inv.size();
        for (int i = 0; i < slots; ++i) {
            ItemStack stackInSlot = inv.getItem(i);
            if (stackInSlot.isEmpty()) continue;
            if (stackInSlot.getItem() instanceof ItemBlockExtraBin) {
                if (!binData.isEmpty() || stackInSlot.count() > 1) {
                    return ItemResource.EMPTY;
                }
                binData = ItemResource.of((ItemStack)stackInSlot);
                continue;
            }
            return ItemResource.EMPTY;
        }
        return binData;
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        int slots = inv.size();
        NonNullList<ItemStack> remaining = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; ++i) {
            ItemStack stackInSlot = inv.getItem(i);
            if (!(stackInSlot.getItem() instanceof ItemBlockExtraBin)) continue;
            ItemAccess binAccess = ItemAccess.forStack((ItemStack)stackInSlot.copy());
            ExtraComponentBackedBinInventorySlot slot = ExtraBinExtractRecipe.convertToSlot(binAccess);
            LargeResourceStack<ItemResource> stack = slot.asStack();
            if (stack.isEmpty()) break;
            ItemResource stored = (ItemResource)stack.resource();
            try (Transaction transaction = TransactionHelper.openTransactionSafe();){
                int toExtract = Math.min(Ints.saturatedCast((long)stack.amount()), stored.getMaxStackSize());
                if (slot.extract(stored, toExtract, (TransactionContext)transaction, AutomationType.MANUAL) != toExtract) break;
                remaining.set(i, ItemAccessUtils.asStack(binAccess));
                transaction.commit();
                break;
            }
        }
        return remaining;
    }

    public RecipeSerializer<ExtraBinExtractRecipe> getSerializer() {
        return ExtraRecipeSerializersInternal.BIN_EXTRACT.get();
    }
}

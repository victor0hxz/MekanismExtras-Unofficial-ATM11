package com.jerry.mekextras.common.recipe.bin;

import com.jerry.mekextras.common.attachments.containers.item.ExtraComponentBackedBinInventorySlot;
import mekanism.common.component.containers.type.ContainerType;
import com.jerry.mekextras.common.item.block.ItemBlockExtraBin;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

public abstract class ExtraBinRecipe
extends CustomRecipe {
    protected static ExtraComponentBackedBinInventorySlot convertToSlot(ItemAccess binAccess) {
        if (!binAccess.getResource().isEmpty() && binAccess.getResource().getItem() instanceof ItemBlockExtraBin
            && ContainerType.ITEM.createContainer(binAccess, 0) instanceof ExtraComponentBackedBinInventorySlot slot) {
            return slot;
        }
        throw new IllegalStateException("Expected Extras bin to have an inventory");
    }
}

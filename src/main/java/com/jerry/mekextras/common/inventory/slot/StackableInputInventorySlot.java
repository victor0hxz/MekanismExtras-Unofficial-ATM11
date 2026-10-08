package com.jerry.mekextras.common.inventory.slot;

import com.jerry.mekextras.common.tier.ExtraFactoryTier;

import mekanism.api.IContentsListener;
import org.jspecify.annotations.NullMarked;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.InputInventorySlot;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

@NullMarked
public class StackableInputInventorySlot extends InputInventorySlot {

    private final ExtraFactoryTier tier;

    public static StackableInputInventorySlot at(ExtraFactoryTier tier, Predicate<@NotNull ItemStack> isItemValid, @Nullable IContentsListener listener, int x, int y) {
        return at(tier, ConstantPredicates.alwaysTrue(), isItemValid, listener, x, y);
    }

    public static StackableInputInventorySlot at(ExtraFactoryTier tier, Predicate<@NotNull ItemStack> insertPredicate, Predicate<@NotNull ItemStack> isItemValid, @Nullable IContentsListener listener,
                                                 int x, int y) {
        Objects.requireNonNull(insertPredicate, "Insertion check cannot be null");
        Objects.requireNonNull(isItemValid, "Item validity check cannot be null");
        return new StackableInputInventorySlot(tier, insertPredicate, isItemValid, listener, x, y);
    }

    protected StackableInputInventorySlot(ExtraFactoryTier tier, Predicate<@NotNull ItemStack> insertPredicate, Predicate<@NotNull ItemStack> isItemValid, @Nullable IContentsListener listener, int x, int y) {
        super(99L * (8L << tier.ordinal()), (resource, automation) -> insertPredicate.test(resource.toStack()), resource -> isItemValid.test(resource.toStack()), null, null, listener, x, y);
        obeyStackLimit = false;
        this.tier = tier;
        setSlotType(ContainerSlotType.EXTRA);
    }

    @Override
    public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        return resource.isEmpty() ? super.capacityAsLong(resource) : Math.min(super.capacityAsLong(resource), resource.getMaxStackSize() * (8L << tier.ordinal()));
    }
}
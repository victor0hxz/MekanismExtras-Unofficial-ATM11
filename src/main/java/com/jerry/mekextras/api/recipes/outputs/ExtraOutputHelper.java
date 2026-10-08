package com.jerry.mekextras.api.recipes.outputs;

import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.NullMarked;
import java.util.Objects;
import java.util.function.IntSupplier;

@NullMarked
public final class ExtraOutputHelper {
    private ExtraOutputHelper() {}

    public static IOutputHandler<ItemStackTemplate> getOutputHandler(IInventorySlot slot,
            RecipeError notEnoughSpaceError, IntSupplier baselineMaxOperations) {
        return limitOperations(OutputHelper.getOutputHandler(slot, notEnoughSpaceError), baselineMaxOperations);
    }

    public static IOutputHandler<ChanceOutput> getOutputHandler(IInventorySlot mainSlot,
            RecipeError mainSlotNotEnoughSpaceError, IInventorySlot secondarySlot,
            RecipeError secondarySlotNotEnoughSpaceError, IntSupplier baselineMaxOperations) {
        return limitOperations(OutputHelper.getOutputHandler(mainSlot, mainSlotNotEnoughSpaceError,
                secondarySlot, secondarySlotNotEnoughSpaceError), baselineMaxOperations);
    }

    private static <OUTPUT> IOutputHandler<OUTPUT> limitOperations(IOutputHandler<OUTPUT> delegate,
            IntSupplier baselineMaxOperations) {
        Objects.requireNonNull(baselineMaxOperations, "Operation limit cannot be null.");
        return new IOutputHandler<>() {
            @Override
            public boolean handleOutput(OUTPUT output, int operations, TransactionContext transaction) {
                return delegate.handleOutput(output, operations, transaction);
            }

            @Override
            public void calculateOperationsCanSupport(OperationTracker tracker, OUTPUT output) {
                tracker.updateOperations(Math.max(0, baselineMaxOperations.getAsInt()));
                if (tracker.shouldContinueChecking()) {
                    delegate.calculateOperationsCanSupport(tracker, output);
                }
            }
        };
    }
}

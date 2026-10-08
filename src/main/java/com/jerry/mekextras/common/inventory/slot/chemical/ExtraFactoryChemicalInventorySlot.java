package com.jerry.mekextras.common.inventory.slot.chemical;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.util.ItemAccessUtils;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class ExtraFactoryChemicalInventorySlot extends ChemicalInventorySlot {
    private final long multiplier;
    private ExtraFactoryChemicalInventorySlot(TileEntityExtraFactory<?> factory, IChemicalTank tank, Supplier<Level> level, @Nullable IContentsListener listener, int x, int y) {
        super(tank, level, (item, automation) -> !automation.isExternal() || !canFillOrConvert(tank, level, item),
            (item, automation) -> automation.isInternal() || canFillOrConvert(tank, level, item), null, null, listener, x, y);
        multiplier = (8L << factory.tier.ordinal()) * factory.tier.processes;
    }
    public static ExtraFactoryChemicalInventorySlot fillOrConverts(TileEntityExtraFactory<?> factory, IChemicalTank tank, Supplier<Level> level, @Nullable IContentsListener listener, int x, int y) {
        return new ExtraFactoryChemicalInventorySlot(factory, tank, level, listener, x, y);
    }
    @Override public long capacityAsLong(ItemResource resource) {
        if (!resource.isEmpty() && Capabilities.CHEMICAL.getCapability(ItemAccessUtils.sideEffectFreeAccess(resource)) != null) return super.capacityAsLong(resource);
        return Math.min(Integer.MAX_VALUE, multiplier * (resource.isEmpty() ? 99L : resource.getMaxStackSize()));
    }
}
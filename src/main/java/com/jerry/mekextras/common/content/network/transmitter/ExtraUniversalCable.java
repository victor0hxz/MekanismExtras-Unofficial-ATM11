package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.tier.transmitter.CTier;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.common.content.network.transmitter.UniversalCable;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.upgrade.transmitter.UniversalCableUpgradeData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;

public class ExtraUniversalCable extends UniversalCable implements IExtraUpgradeableTransmitter<UniversalCableUpgradeData> {
    public ExtraUniversalCable(Holder<Block> blockProvider, TileEntityTransmitter tile) { super(blockProvider, tile); }
    @Override public long getCapacity() { return CTier.getCapacityAsLong(tier); }
    public long getCapacityAsFloatingLong() { return getCapacity(); }
    @Override public void parseUpgradeData(UniversalCableUpgradeData data) { super.parseUpgradeData(data, null); }
}
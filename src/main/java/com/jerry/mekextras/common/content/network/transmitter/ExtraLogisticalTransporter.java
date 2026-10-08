package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import com.jerry.mekextras.common.tier.transmitter.TPTier;
import mekanism.common.content.network.transmitter.LogisticalTransporter;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.upgrade.transmitter.LogisticalTransporterUpgradeData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;

public class ExtraLogisticalTransporter extends LogisticalTransporter implements IExtraUpgradeableTransmitter<LogisticalTransporterUpgradeData> {
    public ExtraLogisticalTransporter(Holder<Block> blockProvider, TileEntityTransmitter tile) { super(blockProvider, tile); }
    @Override public void parseUpgradeData(LogisticalTransporterUpgradeData data) { super.parseUpgradeData(data, null); }
}
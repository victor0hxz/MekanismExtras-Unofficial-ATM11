package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.tier.transmitter.TTier;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.common.content.network.transmitter.PressurizedTube;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.upgrade.transmitter.ResourceTransmitterUpgradeData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import mekanism.api.AutomationType;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.util.ResourceUtils;
import mekanism.api.chemical.ChemicalResource;

public class ExtraPressurizedTube extends PressurizedTube implements IExtraUpgradeableTransmitter<ResourceTransmitterUpgradeData<ChemicalResource>> {
    public ExtraPressurizedTube(Holder<Block> blockProvider, TileEntityTransmitter tile) { super(blockProvider, tile); }
    @Override public long getCapacity() { return TTier.getTubeCapacity(tier); }
    @Override public void parseUpgradeData(ResourceTransmitterUpgradeData<ChemicalResource> data) { super.parseUpgradeData(data, null); }
    @Override public void pullFromAcceptors(ServerLevel level) {
        if (!hasPullSide) return;
        var container = getContainer();
        for (Direction side : Direction.values()) {
            if (!isConnectionType(side, ConnectionType.PULL)) continue;
            ResourceHandler<ChemicalResource> acceptor = getAcceptorCache().getConnectedAcceptor(side);
            if (acceptor == null) continue;
            ChemicalResource resource = ResourceUtils.getTypeToExtract(container.resource(), acceptor, ConstantPredicates.alwaysTrue(), null);
            if (resource.isEmpty()) continue;
            int available = Math.min(mekanism.api.math.MathUtils.clampToInt(TTier.getTubePullAmount(tier)), container.getNeededAsInt(resource));
            if (available <= 0) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = acceptor.extract(resource, available, transaction);
                if (extracted > 0 && container.insert(resource, extracted, transaction, AutomationType.INTERNAL) == extracted) transaction.commit();
            }
        }
    }
}
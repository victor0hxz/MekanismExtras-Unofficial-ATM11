package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.tier.transmitter.PTier;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.common.content.network.transmitter.MechanicalPipe;
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
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class ExtraMechanicalPipe extends MechanicalPipe implements IExtraUpgradeableTransmitter<ResourceTransmitterUpgradeData<FluidResource>> {
    public ExtraMechanicalPipe(Holder<Block> blockProvider, TileEntityTransmitter tile) { super(blockProvider, tile); }
    @Override public long getCapacity() { return PTier.getPipeCapacity(tier); }
    @Override public void parseUpgradeData(ResourceTransmitterUpgradeData<FluidResource> data) { super.parseUpgradeData(data, null); }
    @Override public void pullFromAcceptors(ServerLevel level) {
        if (!hasPullSide) return;
        var container = getContainer();
        for (Direction side : Direction.values()) {
            if (!isConnectionType(side, ConnectionType.PULL)) continue;
            ResourceHandler<FluidResource> acceptor = getAcceptorCache().getConnectedAcceptor(side);
            if (acceptor == null) continue;
            FluidResource resource = ResourceUtils.getTypeToExtract(container.resource(), acceptor, ConstantPredicates.alwaysTrue(), null);
            if (resource.isEmpty()) continue;
            int available = Math.min(PTier.getPipePullAmount(tier), container.getNeededAsInt(resource));
            if (available <= 0) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = acceptor.extract(resource, available, transaction);
                if (extracted > 0 && container.insert(resource, extracted, transaction, AutomationType.INTERNAL) == extracted) transaction.commit();
            }
        }
    }
}
package com.jerry.mekextras.mixin;

import com.jerry.mekextras.api.mixin.IMixinLogisticalTransporterBase;
import com.jerry.mekextras.common.content.network.transmitter.ExtraLogisticalTransporter;
import com.jerry.mekextras.common.network.to_client.transmitter.ExtraPacketTransporterBatch;
import com.jerry.mekextras.common.network.to_client.transmitter.ExtraPacketTransporterSync;
import com.jerry.mekextras.common.tier.transmitter.TPTier;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_client.transmitter.PacketTransporterBatch;
import mekanism.common.network.to_client.transmitter.PacketTransporterSync;
import mekanism.common.tier.TransporterTier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LogisticalTransporterBase.class)
public abstract class MixinLogisticalTransporterBase implements IMixinLogisticalTransporterBase {
    @Shadow protected abstract void entityEntering(TransporterStack stack, int progress);
    @Override public void mekanismExtras$getEntity(TransporterStack stack, int progress) { entityEntering(stack, progress); }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lmekanism/common/tier/TransporterTier;getSpeed()I"))
    private int extras$speed(TransporterTier tier) {
        return (Object)this instanceof ExtraLogisticalTransporter ? TPTier.getSpeed(tier) : tier.getSpeed();
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lmekanism/common/tier/TransporterTier;getPullAmount()I"))
    private int extras$pullAmount(TransporterTier tier) {
        return (Object)this instanceof ExtraLogisticalTransporter ? TPTier.getPullAmount(tier) : tier.getPullAmount();
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lmekanism/common/network/PacketUtils;sendToAllTracking(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void extras$sync(CustomPacketPayload packet, BlockEntity tile) {
        if ((Object)this instanceof ExtraLogisticalTransporter) {
            if (packet instanceof PacketTransporterSync sync) packet = new ExtraPacketTransporterSync(sync.pos(), sync.stackId(), sync.stack());
            else if (packet instanceof PacketTransporterBatch batch) packet = new ExtraPacketTransporterBatch(batch.pos(), batch.deletes(), batch.updates());
        }
        PacketUtils.sendToAllTracking(packet, tile);
    }
}

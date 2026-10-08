package com.jerry.mekextras.common.item.block.transmitter;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.jerry.mekextras.common.tier.transmitter.ExtraTransmitterTier;
import com.jerry.mekextras.common.tier.transmitter.TPTier;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraLogisticalTransporter;

import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.transmitter.BlockLargeTransmitter;
import mekanism.common.tier.TransporterTier;
import mekanism.common.util.MekanismUtils;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ItemBlockExtraLogisticalTransporter extends ItemBlockExtraTransporter<TileEntityExtraLogisticalTransporter> {

    public ItemBlockExtraLogisticalTransporter(BlockLargeTransmitter<TileEntityExtraLogisticalTransporter> block, Properties properties) {
        super(block, properties);
    }

    @NotNull
    @Override
    public TransporterTier getTier() {
        return Objects.requireNonNull(Attribute.getTierNN(getBlock(), TransporterTier.class));
    }

    public @NotNull Component getName(@NotNull ItemStack stack) {
        return TextComponentUtil.build(ExtraTransmitterTier.getAdvancedTier(getTier()).getColor(), super.getName(stack));
    }

    @Override
    protected void addStats(@NotNull ItemStack stack, @NotNull ItemAccess itemAccess, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        super.addStats(stack, itemAccess, context, tooltipDisplay, tooltip, flag);
        TransporterTier tier = getTier();
        float tickRate = Math.max(context.tickRate(), TickRateManager.MIN_TICKRATE);
        float speed = TPTier.getSpeed(tier) / (5 * SharedConstants.TICKS_PER_SECOND / tickRate);
        float pull = TPTier.getPullAmount(tier) * tickRate / MekanismUtils.TICKS_PER_HALF_SECOND;
        tooltip.accept(MekanismLang.SPEED.translateColored(EnumColor.INDIGO, EnumColor.GRAY, speed));
        tooltip.accept(MekanismLang.PUMP_RATE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, pull));
    }
}

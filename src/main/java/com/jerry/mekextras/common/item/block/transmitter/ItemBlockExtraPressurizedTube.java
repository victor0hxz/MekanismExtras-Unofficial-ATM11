package com.jerry.mekextras.common.item.block.transmitter;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.jerry.mekextras.common.tier.transmitter.ExtraTransmitterTier;
import com.jerry.mekextras.common.tier.transmitter.TTier;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraPressurizedTube;

import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.transmitter.BlockSmallTransmitter;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.tier.TubeTier;
import mekanism.common.util.text.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ItemBlockExtraPressurizedTube extends ItemBlockTooltip<BlockSmallTransmitter<TileEntityExtraPressurizedTube>> {

    public ItemBlockExtraPressurizedTube(BlockSmallTransmitter<TileEntityExtraPressurizedTube> block, Properties properties) {
        super(block, true, properties);
    }

    @NotNull
    @Override
    public TubeTier getTier() {
        return Objects.requireNonNull(Attribute.getTierNN(getBlock(), TubeTier.class));
    }

    public @NotNull Component getName(@NotNull ItemStack stack) {
        return TextComponentUtil.build(ExtraTransmitterTier.getAdvancedTier(getTier()).getColor(), super.getName(stack));
    }

    @Override
    protected void addDetails(@NotNull ItemStack stack, @NotNull ItemAccess itemAccess, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        super.addDetails(stack, itemAccess, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(MekanismLang.CAPABLE_OF_TRANSFERRING.translateColored(EnumColor.DARK_GRAY));
        tooltip.accept(MekanismLang.CHEMICAL.translateColored(EnumColor.PURPLE, MekanismLang.MEKANISM));
    }

    @Override
    protected void addStats(@NotNull ItemStack stack, @NotNull ItemAccess itemAccess, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        super.addStats(stack, itemAccess, context, tooltipDisplay, tooltip, flag);
        TubeTier tier = getTier();
        tooltip.accept(MekanismLang.CAPACITY_MB_PER_TICK.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(TTier.getTubeCapacity(tier))));
        tooltip.accept(MekanismLang.PUMP_RATE_MB.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(TTier.getTubePullAmount(tier))));
    }
}

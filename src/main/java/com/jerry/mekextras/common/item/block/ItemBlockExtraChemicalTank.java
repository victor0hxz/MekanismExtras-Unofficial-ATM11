package com.jerry.mekextras.common.item.block;

import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.util.ItemAccessUtils;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.CTTier;
import com.jerry.mekextras.common.tile.TileEntityExtraChemicalTank;

import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.component.component.AttachedEjector;
import mekanism.common.component.component.AttachedSideConfig;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.TileEntityChemicalTank.GasMode;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.text.TextUtils;

import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ItemBlockExtraChemicalTank extends ItemBlockExtraTooltip<BlockTile.BlockTileModel<TileEntityExtraChemicalTank, Machine<TileEntityExtraChemicalTank>>> {

    private static final AttachedSideConfig SIDE_CONFIG = Util.make(() -> {
        Map<TransmissionType, AttachedSideConfig.LightConfigInfo> configInfo = new EnumMap<>(TransmissionType.class);
        configInfo.put(TransmissionType.ITEM, AttachedSideConfig.LightConfigInfo.FRONT_OUT_NO_EJECT);
        configInfo.put(TransmissionType.CHEMICAL, AttachedSideConfig.LightConfigInfo.FRONT_OUT_EJECT);
        return new AttachedSideConfig(configInfo);
    });

    public ItemBlockExtraChemicalTank(BlockTile.BlockTileModel<TileEntityExtraChemicalTank, Machine<TileEntityExtraChemicalTank>> block, Properties properties) {
        super(block, true, properties
                .component(MekanismDataComponents.DUMP_MODE, GasMode.IDLE)
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, SIDE_CONFIG));
    }

    @Override
    public CTTier getAdvancedTier() {
        return ExtraAttribute.getAdvancedTier(getBlock(), CTTier.class);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        CTTier tier = getAdvancedTier();
        StorageUtils.addStoredSubstance(ItemAccessUtils.sideEffectFreeAccess(stack), tooltip, false);
        tooltip.accept(MekanismLang.CAPACITY_MB.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(tier.getStorage())));
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        // No bar for empty or stacked containers as bars are drawn on top of stack count number
        if (stack.getCount() > 1) {
            // Note: Technically this is handled by the below checks as the capability isn't exposed,
            // but we may as well short circuit it here
            return false;
        }
        return !ContainerType.CHEMICAL.getFirstResourceFromAttachment(ItemAccessUtils.sideEffectFreeAccess(stack)).isEmpty();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return StorageUtils.getBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return ContainerType.CHEMICAL.getRGBDurabilityForDisplay(stack);
    }
}

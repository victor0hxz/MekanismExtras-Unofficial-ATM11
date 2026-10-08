package com.jerry.mekextras.common.item.block;

import mekanism.common.util.ItemAccessUtils;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.ICTier;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionCell;

import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.text.EnergyDisplay;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ItemBlockExtraInductionCell extends ItemBlockExtraTooltip<BlockTile<TileEntityExtraInductionCell, BlockTypeTile<TileEntityExtraInductionCell>>> {

    public ItemBlockExtraInductionCell(BlockTile<TileEntityExtraInductionCell, BlockTypeTile<TileEntityExtraInductionCell>> block, Properties properties) {
        super(block, properties);
    }

    @NotNull
    @Override
    public ICTier getAdvancedTier() {
        return Objects.requireNonNull(ExtraAttribute.getAdvancedTier(getBlock(), ICTier.class));
    }

    @Override
    protected void addStats(@NotNull ItemStack stack, @NotNull ItemAccess itemAccess, @Nullable TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        ICTier tier = getAdvancedTier();
        tooltip.accept(MekanismLang.CAPACITY.translateColored(tier.getAdvanceTier().getColor(), EnumColor.GRAY, EnergyDisplay.of(tier.getMaxEnergy())));
        StorageUtils.addStoredEnergy(ItemAccessUtils.sideEffectFreeAccess(stack), tooltip, false);
    }

    @Override
    protected boolean exposesEnergyCap() {
        return false;
    }
}

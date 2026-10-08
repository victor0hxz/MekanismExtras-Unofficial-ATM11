package com.jerry.mekextras.common.item.block;

import com.jerry.mekextras.api.tier.IAdvancedTier;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.block.interfaces.IColoredBlock;
import mekanism.common.block.interfaces.IHasDescription;
import mekanism.common.item.block.ItemBlockTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class ItemBlockExtraTooltip<BLOCK extends Block & IHasDescription> extends ItemBlockTooltip<BLOCK> {
    public ItemBlockExtraTooltip(BLOCK block, Properties properties) {
        super(block, properties);
    }

    protected ItemBlockExtraTooltip(BLOCK block, boolean hasDetails, Properties properties) {
        super(block, hasDetails, properties);
    }

    public @Nullable IAdvancedTier getAdvancedTier() {
        return null;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getBlock() instanceof IColoredBlock coloredBlock) {
            return TextComponentUtil.build(coloredBlock.getColor(), super.getName(stack));
        }
        IAdvancedTier tier = getAdvancedTier();
        return tier == null ? super.getName(stack) : TextComponentUtil.build(tier.getAdvanceTier().getColor(), super.getName(stack));
    }
}

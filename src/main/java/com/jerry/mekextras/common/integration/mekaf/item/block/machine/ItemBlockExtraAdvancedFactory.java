package com.jerry.mekextras.common.integration.mekaf.item.block.machine;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.block.prefab.BlockExtraAdvancedFactoryMachine.BlockExtraAdvancedFactory;
import com.jerry.mekextras.common.item.block.ItemBlockExtraTooltip;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;

import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.component.component.AttachedEjector;
import mekanism.common.component.component.AttachedSideConfig;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.registries.MekanismDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.jerry.mekaf.common.block.attribute.AttributeAdvancedFactoryType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemBlockExtraAdvancedFactory extends ItemBlockExtraTooltip<BlockTile<?, ?>> {

    private static AttachedSideConfig getSideConfig(BlockExtraAdvancedFactory<?> block) {
        return switch (Attribute.getOrThrow(block.builtInRegistryHolder(), AttributeAdvancedFactoryType.class).getAdvancedFactoryType()) {
            case OXIDIZING, PIGMENT_EXTRACTING -> AttachedSideConfig.CHEMICAL_OUT_MACHINE;
            case DISSOLVING -> AttachedSideConfig.DISSOLUTION;
            case WASHING -> AttachedSideConfig.WASHER;
            case PRESSURISED_REACTING -> AttachedSideConfig.REACTION;
            case CRYSTALLIZING -> AttachedSideConfig.CRYSTALLIZER;
            case CENTRIFUGING -> AttachedSideConfig.CENTRIFUGE;
            case LIQUIFYING -> AttachedSideConfig.LIQUIFIER;
            case PAINTING -> AttachedSideConfig.PAINTING;
        };
    }

    public ItemBlockExtraAdvancedFactory(BlockExtraAdvancedFactory<?> block, Properties properties) {
        super(block, true, properties
                .component(MekanismDataComponents.SORTING, false)
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, getSideConfig(block)));
    }

    @Override
    public ExtraFactoryTier getAdvancedTier() {
        return ExtraAttribute.getAdvancedTier(getBlock(), ExtraFactoryTier.class);
    }

    @Override
    protected void addTypeDetails(@NotNull ItemStack stack, @NotNull ItemAccess itemAccess, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        // Should always be present but validate it just in case
        AttributeAdvancedFactoryType factoryType = Attribute.get(getBlock(), AttributeAdvancedFactoryType.class);
        if (factoryType != null) {
            tooltip.accept(MekanismLang.FACTORY_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, factoryType.getAdvancedFactoryType()));
        }
        super.addTypeDetails(stack, itemAccess, context, tooltipDisplay, tooltip, flag);
    }
}

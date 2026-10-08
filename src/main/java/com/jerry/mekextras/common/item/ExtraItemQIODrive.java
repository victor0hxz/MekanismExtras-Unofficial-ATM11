package com.jerry.mekextras.common.item;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.jerry.mekextras.common.tier.ExtraQIODriveTier;

import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.component.qio.DriveMetadata;
import mekanism.common.content.qio.IQIODriveItem;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.text.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExtraItemQIODrive extends Item implements IQIODriveItem {

    private final ExtraQIODriveTier tier;

    public ExtraItemQIODrive(ExtraQIODriveTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        DriveMetadata meta = stack.getOrDefault(MekanismDataComponents.DRIVE_METADATA, DriveMetadata.EMPTY);
        tooltip.accept(MekanismLang.QIO_ITEMS_DETAIL.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                TextUtils.format(meta.count()), TextUtils.format(getCountCapacity())));
        tooltip.accept(MekanismLang.QIO_TYPES_DETAIL.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                TextUtils.format(meta.types()), TextUtils.format(getTypeCapacity())));
    }

    @NotNull
    @Override
    public Component getName(@NotNull ItemStack stack) {
        return TextComponentUtil.build(tier.getAdvanceTier().getColor(), super.getName(stack));
    }

    @Override
    public long getCountCapacity() {
        return tier.getCount();
    }

    @Override
    public int getTypeCapacity() {
        return tier.getTypes();
    }
}

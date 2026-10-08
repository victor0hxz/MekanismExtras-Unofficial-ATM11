package com.jerry.mekextras.common.item.block;

import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.util.ItemAccessUtils;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import com.jerry.mekextras.common.attachments.containers.energy.ExtraComponentBackedEnergyCubeContainer;
import com.jerry.mekextras.common.block.BlockExtraEnergyCube;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.ECTier;

import mekanism.api.RelativeSide;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.component.component.AttachedEjector;
import mekanism.common.component.component.AttachedSideConfig;
import mekanism.common.component.containers.energy.EnergyContainerBuilder;
import mekanism.common.component.containers.creator.IContainerCreator;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.config.MekanismConfig;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.text.EnergyDisplay;

import net.minecraft.util.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemBlockExtraEnergyCube extends ItemBlockExtraTooltip<BlockExtraEnergyCube> implements CreativeTabDeferredRegister.ICustomCreativeTabContents {

    public static final AttachedSideConfig SIDE_CONFIG = sideConfig(AttachedSideConfig.LightConfigInfo.FRONT_OUT_EJECT);
    public static final AttachedSideConfig ALL_INPUT = Util.make(() -> {
        Map<RelativeSide, DataType> sideData = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : EnumUtils.SIDES) {
            sideData.put(side, DataType.INPUT);
        }
        return sideConfig(new AttachedSideConfig.LightConfigInfo(sideData, false));
    });
    public static final AttachedSideConfig ALL_OUTPUT = Util.make(() -> {
        Map<RelativeSide, DataType> sideData = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : EnumUtils.SIDES) {
            sideData.put(side, DataType.OUTPUT);
        }
        return sideConfig(new AttachedSideConfig.LightConfigInfo(sideData, true));
    });

    private static AttachedSideConfig sideConfig(AttachedSideConfig.LightConfigInfo energyConfig) {
        Map<TransmissionType, AttachedSideConfig.LightConfigInfo> configInfo = new EnumMap<>(TransmissionType.class);
        configInfo.put(TransmissionType.ITEM, AttachedSideConfig.LightConfigInfo.FRONT_OUT_NO_EJECT);
        configInfo.put(TransmissionType.ENERGY, energyConfig);
        return new AttachedSideConfig(configInfo);
    }

    public ItemBlockExtraEnergyCube(BlockExtraEnergyCube block, Properties properties) {
        super(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, SIDE_CONFIG));
    }

    @Override
    public ECTier getAdvancedTier() {
        return ExtraAttribute.getAdvancedTier(getBlock(), ECTier.class);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        StorageUtils.addStoredEnergy(ItemAccessUtils.sideEffectFreeAccess(stack), tooltip, true);
        tooltip.accept(MekanismLang.CAPACITY.translateColored(EnumColor.INDIGO, EnumColor.GRAY, EnergyDisplay.of(getAdvancedTier().getMaxEnergy())));
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
    }

    @Override
    protected void addTypeDetails(@NotNull ItemStack stack, @NotNull ItemAccess itemAccess, @Nullable Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        // Don't call super so that we can exclude the stored energy from being shown as we show it in hover text
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        // If we are currently stacked, don't display the bar as it will overlap the stack count
        return stack.getCount() == 1;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return StorageUtils.getEnergyBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return MekanismConfig.client.energyColor.get();
    }

    @Override
    public void addItems(Holder<Item> item, Consumer<ItemStack> tabOutput) {
        tabOutput.accept(ContainerType.ENERGY.getFilledVariant(item, null));
    }

    @Override
    public boolean addDefault() {
        return CreativeTabDeferredRegister.ICustomCreativeTabContents.super.addDefault();
    }

    @Override
    protected IContainerCreator<IEnergyContainer, Long> getDefaultEnergyContainer() {
        return EnergyContainerBuilder.creator(ExtraComponentBackedEnergyCubeContainer::create);
    }
}

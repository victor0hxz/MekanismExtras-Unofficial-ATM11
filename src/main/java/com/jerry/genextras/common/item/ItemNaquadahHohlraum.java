package com.jerry.genextras.common.item;

import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.util.ItemAccessUtils;
import net.neoforged.neoforge.transfer.ResourceHandler;
import mekanism.api.chemical.ChemicalResource;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.item.component.TooltipDisplay;
import com.jerry.mekextras.common.registries.ExtraChemicals;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.util.StorageUtils;
import mekanism.generators.common.GeneratorsLang;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ItemNaquadahHohlraum extends Item implements CreativeTabDeferredRegister.ICustomCreativeTabContents {

    public ItemNaquadahHohlraum(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context,
                                @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag) {
        ResourceHandler<ChemicalResource> handler = Capabilities.CHEMICAL.getCapability(ItemAccessUtils.sideEffectFreeAccess(stack));
        if (handler != null && handler.size() > 0 && !handler.getResource(0).isEmpty() && handler.getAmountAsLong(0) > 0) {
            tooltip.accept(MekanismLang.STORED.translate(handler.getResource(0), handler.getAmountAsLong(0)));
            tooltip.accept(handler.getAmountAsLong(0) == handler.getCapacityAsLong(0, handler.getResource(0))
                ? GeneratorsLang.READY_FOR_REACTION.translateColored(EnumColor.DARK_GREEN)
                : GeneratorsLang.INSUFFICIENT_FUEL.translateColored(EnumColor.DARK_RED));
        } else {
            tooltip.accept(MekanismLang.NO_CHEMICAL.translate());
            tooltip.accept(GeneratorsLang.INSUFFICIENT_FUEL.translateColored(EnumColor.DARK_RED));
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return StorageUtils.getBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return ContainerType.CHEMICAL.getRGBDurabilityForDisplay(stack);
    }

    @Override
    public void addItems(Holder<Item> item, Consumer<ItemStack> tabOutput) {
        tabOutput.accept(ContainerType.CHEMICAL.getFilledVariant(item, ChemicalResource.of(ExtraChemicals.NAQUADAH_URANIUM_FUEL), null));
    }
}

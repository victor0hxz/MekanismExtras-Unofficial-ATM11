package com.jerry.mekextras.client.gui.machine;

import com.jerry.mekextras.common.tile.machine.TileEntityAdvancedElectricPump;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.TextUtils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GuiAdvanceElectricPump extends GuiMekanismTile<TileEntityAdvancedElectricPump, MekanismTileContainer<TileEntityAdvancedElectricPump>> {

    public GuiAdvanceElectricPump(MekanismTileContainer<TileEntityAdvancedElectricPump> container, Inventory inv, Component title) {
        super(container, inv, title);
        titleLabelY = 5;
        inventoryLabelY += 2;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this, 54, 23, 80, 41, () -> {
            List<Component> list = new ArrayList<>();
            list.add(EnergyDisplay.of(tile.energyContainer()).getTextComponent());
            FluidStack fluidStack = tile.fluidTank.resource().toStack(tile.fluidTank.amountAsInt());
            if (fluidStack.isEmpty()) {
                FluidStack fallBack = tile.getActiveType().toStack(1);
                if (fallBack.isEmpty()) {
                    list.add(MekanismLang.NO_FLUID.translate());
                } else {
                    list.add(fallBack.getHoverName());
                }
            } else {
                list.add(MekanismLang.GENERIC_STORED_MB.translate(fluidStack, TextUtils.format(fluidStack.getAmount())));
            }
            return list;
        }));
        addRenderableWidget(new GuiDownArrow(this, 32, 39));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energyContainer(), 164, 15))
                .warning(WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> {
                    MachineEnergyContainer<TileEntityAdvancedElectricPump> energyContainer = tile.energyContainer();
                    return energyContainer.getEnergyPerTick() > energyContainer.getAmountAsLong();
                });
        addRenderableWidget(new GuiFluidGauge(() -> tile.fluidTank, () -> tile.getFluidTanks(), GaugeType.STANDARD, this, 6, 13))
                .warning(WarningTracker.WarningType.NO_SPACE_IN_OUTPUT, () -> tile.fluidTank.getNeededAsInt(tile.fluidTank.resource()) < tile.estimateIncrementAmount());
        // TODO: Eventually we may want to consider showing a warning if the block under the pump is of the wrong type
        // or there wasn't a valid spot to suck
        addRenderableWidget(new GuiEnergyTab(this, tile.energyContainer(), tile::usedEnergy));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}

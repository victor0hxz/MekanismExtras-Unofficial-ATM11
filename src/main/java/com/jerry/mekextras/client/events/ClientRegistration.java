package com.jerry.mekextras.client.events;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.client.gui.*;
import com.jerry.mekextras.client.gui.machine.GuiAdvanceElectricPump;
import com.jerry.mekextras.client.gui.machine.GuiExtraAdvancedFactory;
import com.jerry.mekextras.client.gui.machine.GuiExtraFactory;
import com.jerry.mekextras.client.gui.machine.GuiExtraMoreMachineFactory;
import com.jerry.mekextras.client.model.ColorModelEnergyCore;
import com.jerry.mekextras.client.model.energycube.ExtraEnergyCubeModelLoader;
import com.jerry.mekextras.client.render.item.block.RenderExtraEnergyCubeItem;
import com.jerry.mekextras.client.render.item.block.RenderExtraFluidTankItem;
import com.jerry.mekextras.client.render.tileentity.RenderExtraBin;
import com.jerry.mekextras.client.render.tileentity.RenderExtraEnergyCube;
import com.jerry.mekextras.client.render.tileentity.RenderExtraFluidTank;
import com.jerry.mekextras.client.render.transmitter.*;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.integration.mekaf.registries.ExtraAdvancedFactoryContainerTypes;
import com.jerry.mekextras.common.integration.mekmm.registries.ExtraMoreMachineContainerTypes;
import com.jerry.mekextras.common.item.block.ItemBlockExtraEnergyCube;
import com.jerry.mekextras.common.item.block.machine.ItemBlockExtraFluidTank;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import com.jerry.mekextras.common.registries.ExtraContainerTypes;
import com.jerry.mekextras.common.registries.ExtraFluids;
import com.jerry.mekextras.common.registries.ExtraTileEntityTypes;
import com.jerry.mekextras.common.tier.ECTier;
import com.jerry.mekextras.common.tier.FTTier;
import com.jerry.mekextras.common.tier.TierColor;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraLogisticalTransporter;

import com.jerry.genextras.client.gui.*;
import com.jerry.genextras.client.render.RenderNaquadahReactor;
import com.jerry.genextras.common.registries.GenExtraBlocks;
import com.jerry.genextras.common.registries.GenExtraContainerTypes;
import com.jerry.genextras.common.registries.GenExtraFluids;
import com.jerry.genextras.common.registries.GenExtraTileEntityTypes;

import mekanism.api.text.EnumColor;
import mekanism.client.ClientRegistrationUtil;
import mekanism.client.render.RenderPropertiesProvider;
import mekanism.client.render.item.TransmitterTypeDecorator;
import mekanism.common.util.WorldUtils;


import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = MekanismExtras.MOD_ID, value = Dist.CLIENT)
public class ClientRegistration {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(new ClientTick());

    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraBin::new, ExtraTileEntityTypes.ABSOLUTE_BIN, ExtraTileEntityTypes.SUPREME_BIN, ExtraTileEntityTypes.COSMIC_BIN,
                ExtraTileEntityTypes.INFINITE_BIN);
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraEnergyCube::new, ExtraTileEntityTypes.ABSOLUTE_ENERGY_CUBE, ExtraTileEntityTypes.SUPREME_ENERGY_CUBE,
                ExtraTileEntityTypes.COSMIC_ENERGY_CUBE, ExtraTileEntityTypes.INFINITE_ENERGY_CUBE);
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraFluidTank::new, ExtraTileEntityTypes.ABSOLUTE_FLUID_TANK, ExtraTileEntityTypes.SUPREME_FLUID_TANK,
                ExtraTileEntityTypes.COSMIC_FLUID_TANK, ExtraTileEntityTypes.INFINITE_FLUID_TANK);
        // Transmitters
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraLogisticalTransporter::new, ExtraTileEntityTypes.ABSOLUTE_LOGISTICAL_TRANSPORTER, ExtraTileEntityTypes.SUPREME_LOGISTICAL_TRANSPORTER,
                ExtraTileEntityTypes.COSMIC_LOGISTICAL_TRANSPORTER, ExtraTileEntityTypes.INFINITE_LOGISTICAL_TRANSPORTER);
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraMechanicalPipe::new, ExtraTileEntityTypes.ABSOLUTE_MECHANICAL_PIPE,
                ExtraTileEntityTypes.SUPREME_MECHANICAL_PIPE, ExtraTileEntityTypes.COSMIC_MECHANICAL_PIPE, ExtraTileEntityTypes.INFINITE_MECHANICAL_PIPE);
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraPressurizedTube::new, ExtraTileEntityTypes.ABSOLUTE_PRESSURIZED_TUBE,
                ExtraTileEntityTypes.SUPREME_PRESSURIZED_TUBE, ExtraTileEntityTypes.COSMIC_PRESSURIZED_TUBE, ExtraTileEntityTypes.INFINITE_PRESSURIZED_TUBE);
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraUniversalCable::new, ExtraTileEntityTypes.ABSOLUTE_UNIVERSAL_CABLE,
                ExtraTileEntityTypes.SUPREME_UNIVERSAL_CABLE, ExtraTileEntityTypes.COSMIC_UNIVERSAL_CABLE, ExtraTileEntityTypes.INFINITE_UNIVERSAL_CABLE);
        ClientRegistrationUtil.bindTileEntityRenderer(event, RenderExtraThermodynamicConductor::new, ExtraTileEntityTypes.ABSOLUTE_THERMODYNAMIC_CONDUCTOR,
                ExtraTileEntityTypes.SUPREME_THERMODYNAMIC_CONDUCTOR, ExtraTileEntityTypes.COSMIC_THERMODYNAMIC_CONDUCTOR, ExtraTileEntityTypes.INFINITE_THERMODYNAMIC_CONDUCTOR);

        // Generator Extras
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            event.registerBlockEntityRenderer(GenExtraTileEntityTypes.NAQUADAH_REACTOR_CONTROLLER.get(), RenderNaquadahReactor::new);
        }
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.ADVANCE_ELECTRIC_PUMP, GuiAdvanceElectricPump::new);

        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.EXTRA_ENERGY_CUBE, GuiExtraEnergyCube::new);
        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.EXTRA_FLUID_TANK, GuiExtraFluidTank::new);
        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.EXTRA_CHEMICAL_TANK, GuiExtraChemicalTank::new);

        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.FACTORY, GuiExtraFactory::new);

        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.REINFORCED_INDUCTION_MATRIX, GuiReinforcedInductionMatrix::new);
        ClientRegistrationUtil.registerScreen(event, ExtraContainerTypes.REINFORCED_MATRIX_STATS, GuiReinforcedMatrixStats::new);

        // Generator Extras
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            ClientRegistrationUtil.registerScreen(event, GenExtraContainerTypes.NAQUADAH_REACTOR_CONTROLLER, GuiNaquadahReactorController::new);
            ClientRegistrationUtil.registerScreen(event, GenExtraContainerTypes.NAQUADAH_REACTOR_FUEL, GuiNaquadahReactorFuel::new);
            ClientRegistrationUtil.registerScreen(event, GenExtraContainerTypes.NAQUADAH_REACTOR_HEAT, GuiNaquadahReactorHeat::new);
            ClientRegistrationUtil.registerScreen(event, GenExtraContainerTypes.NAQUADAH_REACTOR_LOGIC_ADAPTER, GuiNaquadahReactorLogicAdapter::new);
            ClientRegistrationUtil.registerScreen(event, GenExtraContainerTypes.NAQUADAH_REACTOR_STATS, GuiNaquadahReactorStats::new);
        }

        // MoreMachine
        if (MekanismExtras.hooks.mekmm.isLoaded()) {
            ClientRegistrationUtil.registerScreen(event, ExtraAdvancedFactoryContainerTypes.ADVANCED_FACTORY, GuiExtraAdvancedFactory::new);
            ClientRegistrationUtil.registerScreen(event, ExtraMoreMachineContainerTypes.MORE_MACHINE_FACTORY, GuiExtraMoreMachineFactory::new);
        }
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
        event.register(MekanismExtras.rl("energy_cube"), ExtraEnergyCubeModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerBlockColorHandlers(RegisterColorHandlersEvent.BlockTintSources event) {
        net.minecraft.client.color.block.BlockTintSource tierColor = state -> {
            var tier = ExtraAttribute.getAdvancedTier(state.getBlock(), com.jerry.mekextras.api.tier.IAdvancedTier.class);
            return tier == null ? -1 : TierColor.getPackedColor(tier);
        };
        ClientRegistrationUtil.registerBlockColorHandler(event, java.util.List.of(state -> -1, tierColor),
            ExtraBlocks.ABSOLUTE_FLUID_TANK, ExtraBlocks.SUPREME_FLUID_TANK, ExtraBlocks.COSMIC_FLUID_TANK, ExtraBlocks.INFINITE_FLUID_TANK,
            ExtraBlocks.ABSOLUTE_ENERGY_CUBE, ExtraBlocks.SUPREME_ENERGY_CUBE, ExtraBlocks.COSMIC_ENERGY_CUBE, ExtraBlocks.INFINITE_ENERGY_CUBE);
        net.minecraft.client.color.block.BlockTintSource transporterColor = new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) { return -1; }
            @Override
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.client.renderer.block.BlockAndTintGetter world, net.minecraft.core.BlockPos pos) {
                var tile = WorldUtils.getTileEntity(TileEntityExtraLogisticalTransporter.class, world, pos);
                EnumColor color = tile == null ? null : tile.getTransmitter().getColor();
                return color == null ? -1 : color.getPackedColor();
            }
        };
        ClientRegistrationUtil.registerBlockColorHandler(event, java.util.List.of(state -> -1, transporterColor),
            ExtraBlocks.ABSOLUTE_LOGISTICAL_TRANSPORTER, ExtraBlocks.SUPREME_LOGISTICAL_TRANSPORTER,
            ExtraBlocks.COSMIC_LOGISTICAL_TRANSPORTER, ExtraBlocks.INFINITE_LOGISTICAL_TRANSPORTER);
    }

    @SubscribeEvent
    public static void registerItemColorHandlers(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(MekanismExtras.rl("tier"), com.jerry.mekextras.client.model.ExtraTierTintSource.MAP_CODEC);
    }
    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        TransmitterTypeDecorator.registerDecorators(event, ExtraBlocks.ABSOLUTE_PRESSURIZED_TUBE, ExtraBlocks.SUPREME_PRESSURIZED_TUBE,
                ExtraBlocks.COSMIC_PRESSURIZED_TUBE, ExtraBlocks.INFINITE_PRESSURIZED_TUBE, ExtraBlocks.ABSOLUTE_THERMODYNAMIC_CONDUCTOR,
                ExtraBlocks.SUPREME_THERMODYNAMIC_CONDUCTOR, ExtraBlocks.COSMIC_THERMODYNAMIC_CONDUCTOR, ExtraBlocks.INFINITE_THERMODYNAMIC_CONDUCTOR,
                ExtraBlocks.ABSOLUTE_UNIVERSAL_CABLE, ExtraBlocks.SUPREME_UNIVERSAL_CABLE, ExtraBlocks.COSMIC_UNIVERSAL_CABLE, ExtraBlocks.INFINITE_UNIVERSAL_CABLE);
    }

    @SubscribeEvent
    public static void registerSpecialRenderers(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) {
        event.register(MekanismExtras.rl("fluid_tank"), RenderExtraFluidTankItem.Unbaked.MAP_CODEC);
        event.register(MekanismExtras.rl("energy_cube"), RenderExtraEnergyCubeItem.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {

        ClientRegistrationUtil.registerBlockExtensions(event, ExtraBlocks.EXTRA_BLOCKS);
        ClientRegistrationUtil.registerFluidExtensions(event, ExtraFluids.EXTRA_FLUIDS);
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            ClientRegistrationUtil.registerBlockExtensions(event, GenExtraBlocks.GEN_EXTRA_BLOCKS);
            ClientRegistrationUtil.registerFluidExtensions(event, GenExtraFluids.GEN_EXTRA_FLUIDS);
        }
    }
    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        ClientRegistrationUtil.registerFluidModels(event, ExtraFluids.EXTRA_FLUIDS);
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            ClientRegistrationUtil.registerFluidModels(event, GenExtraFluids.GEN_EXTRA_FLUIDS);
        }
    }}

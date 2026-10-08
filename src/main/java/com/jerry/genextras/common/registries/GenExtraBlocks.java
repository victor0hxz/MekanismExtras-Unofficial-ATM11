package com.jerry.genextras.common.registries;

import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.jerry.mekextras.MekanismExtras;

import com.jerry.genextras.common.block.naquadah.BlockLeadCoatedLaserFocusMatrix;
import com.jerry.genextras.common.item.ItemBlockNaquadahLogicAdapter;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorCasing;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorController;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorLogicAdapter;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorPort;

import mekanism.common.block.interfaces.IHasDescription;
import mekanism.common.block.prefab.BlockBasicMultiblock;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class GenExtraBlocks {

    private GenExtraBlocks() {}

    public static final BlockDeferredRegister GEN_EXTRA_BLOCKS = new BlockDeferredRegister(MekanismExtras.MOD_ID);

    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityNaquadahReactorController>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityNaquadahReactorController>>> NAQUADAH_REACTOR_CONTROLLER = registerTooltipBlock("naquadah_reactor_controller", properties -> new BlockBasicMultiblock<>(GenExtraBlockTypes.NAQUADAH_REACTOR_CONTROLLER, BlockBasicMultiblock.defaultProperties(properties).mapColor(MapColor.COLOR_ORANGE)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityNaquadahReactorCasing>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityNaquadahReactorCasing>>> NAQUADAH_REACTOR_CASING = registerTooltipBlock("naquadah_reactor_casing", properties -> new BlockBasicMultiblock<>(GenExtraBlockTypes.NAQUADAH_REACTOR_CASING, BlockBasicMultiblock.defaultProperties(properties).mapColor(MapColor.TERRACOTTA_BROWN)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityNaquadahReactorPort>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityNaquadahReactorPort>>> NAQUADAH_REACTOR_PORT = registerTooltipBlock("naquadah_reactor_port", properties -> new BlockBasicMultiblock<>(GenExtraBlockTypes.NAQUADAH_REACTOR_PORT, BlockBasicMultiblock.defaultProperties(properties).mapColor(MapColor.TERRACOTTA_BROWN)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityNaquadahReactorLogicAdapter>, ItemBlockNaquadahLogicAdapter> NAQUADAH_REACTOR_LOGIC_ADAPTER = GEN_EXTRA_BLOCKS.register("naquadah_reactor_logic_adapter", properties -> new BlockBasicMultiblock<>(GenExtraBlockTypes.NAQUADAH_REACTOR_LOGIC_ADAPTER, BlockBasicMultiblock.defaultProperties(properties).mapColor(MapColor.TERRACOTTA_BROWN)), ItemBlockNaquadahLogicAdapter::new);
    public static final BlockRegistryObject<BlockLeadCoatedLaserFocusMatrix, ItemBlockTooltip<BlockLeadCoatedLaserFocusMatrix>> LEAD_COATED_LASER_FOCUS_MATRIX = registerTooltipBlock("lead_coated_laser_focus_matrix", BlockLeadCoatedLaserFocusMatrix::new);

    private static <BLOCK extends Block & IHasDescription> BlockRegistryObject<BLOCK, ItemBlockTooltip<BLOCK>> registerTooltipBlock(String name, Function<BlockBehaviour.Properties, ? extends BLOCK> blockCreator) {
        return GEN_EXTRA_BLOCKS.register(name, blockCreator, ItemBlockTooltip::new);
    }
}

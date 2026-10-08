package com.jerry.mekextras.common.block.prefab;

import com.jerry.mekextras.common.content.blocktype.ExtraFactory;
import com.jerry.mekextras.common.content.blocktype.ExtraMachine.ExtraFactoryMachine;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.block.states.IStateFluidLoggable;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.tile.base.TileEntityMekanism;


public class BlockExtraFactoryMachine<TILE extends TileEntityMekanism, MACHINE extends ExtraFactoryMachine<TILE>> extends BlockTile<TILE, MACHINE> {

    public BlockExtraFactoryMachine(MACHINE machineType, Properties properties) {
        super(machineType, properties);
    }

    public static class BlockExtraFactoryMachineModel<TILE extends TileEntityMekanism, MACHINE extends ExtraFactoryMachine<TILE>> extends BlockExtraFactoryMachine<TILE, MACHINE> implements IStateFluidLoggable {

        public BlockExtraFactoryMachineModel(MACHINE machineType, Properties properties) {
            super(machineType, properties);
        }
    }

    public static class BlockExtraFactory<TILE extends TileEntityExtraFactory<?>> extends BlockExtraFactoryMachineModel<TILE, ExtraFactory<TILE>> {

        public BlockExtraFactory(ExtraFactory<TILE> factoryType, Properties properties) {
            super(factoryType, defaultProperties(properties).mapColor(BlockResourceInfo.STEEL.getMapColor()));
        }
    }
}

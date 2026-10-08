package com.jerry.mekextras.common.block.basic;

import com.jerry.mekextras.common.tile.TileEntityExtraFluidTank;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.lib.transaction.TransactionHelper;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class BlockExtraFluidTank extends BlockTile.BlockTileModel<TileEntityExtraFluidTank, Machine<TileEntityExtraFluidTank>> {
    public BlockExtraFluidTank(Machine<TileEntityExtraFluidTank> type, Properties properties) {
        super(type, defaultProperties(properties).mapColor(BlockResourceInfo.STEEL.getMapColor()));
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        int ambientLight = super.getLightEmission(state, world, pos);
        TileEntityExtraFluidTank tile = WorldUtils.getTileEntity(TileEntityExtraFluidTank.class, world, pos);
        if (ambientLight < 15 && tile != null && !tile.fluidTank.isEmpty()) {
            ambientLight = Math.max(ambientLight, tile.fluidTank.resource().getFluidType().getLightLevel());
        }
        return ambientLight;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        TileEntityExtraFluidTank tile = WorldUtils.getTileEntity(TileEntityExtraFluidTank.class, world, pos, true);
        if (tile == null) return InteractionResult.FAIL;
        if (world.isClientSide()) return genericClientActivated(stack, tile);
        InteractionResult wrenchResult = tile.tryWrench(world, state, player, stack).getInteractionResult();
        if (wrenchResult != InteractionResult.PASS) return wrenchResult;
        if (!player.isShiftKeyDown()) {
            if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, world, pos, tile)) return InteractionResult.FAIL;
            var handler = Capabilities.FLUID.getCapabilityIfLoaded(world, pos, null, tile, hit.getDirection());
            if (handler != null) {
                try (Transaction transaction = TransactionHelper.openTransactionSafe()) {
                    if (ContainerType.FLUID.interactWithHandler(player, hand, pos, handler, transaction)) {
                        transaction.commit();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}

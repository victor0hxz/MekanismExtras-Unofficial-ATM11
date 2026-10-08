package com.jerry.mekextras.client.render.tileentity;

import com.jerry.mekextras.common.inventory.slot.ExtraBinInventorySlot;
import com.jerry.mekextras.common.tile.TileEntityExtraBin;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.api.math.MathUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import mekanism.client.render.tileentity.RenderBin;
import mekanism.client.render.tileentity.RenderBin.BinRenderState;
import mekanism.common.util.WorldUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderExtraBin extends MekanismTileEntityRenderer<TileEntityExtraBin, BinRenderState> {
    private final RenderBin renderer;
    private final ItemModelResolver itemModels;

    public RenderExtraBin(BlockEntityRendererProvider.Context context) {
        super(context);
        renderer = new RenderBin(context);
        itemModels = context.itemModelResolver();
    }
    @Override public BinRenderState createRenderState() { return new BinRenderState(); }

    @Override
    public void extractRenderState(TileEntityExtraBin tile, BinRenderState state, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        state.facing = null;
        state.displayCount = null;
        Level level = tile.getLevel();
        ExtraBinInventorySlot slot = tile.getBinSlot();
        if (level == null || slot.isEmpty() && !slot.isLocked()) return;
        BlockPos coverPos = state.blockPos.relative(tile.getDirection());
        var cover = WorldUtils.getBlockState(level, coverPos);
        if (cover.isPresent() && cover.get().canOcclude() && cover.get().isFaceSturdy(level, coverPos, tile.getDirection().getOpposite())) return;
        state.facing = tile.getDirection();
        state.lightCoords = LevelRenderer.getLightCoords(level, coverPos);
        itemModels.updateForTopItem(state.item, slot.getBinItemType().toStack(), ItemDisplayContext.GUI,
            level, null, MathUtils.clampToInt(state.blockPos.asLong()));
        state.displayCount = TextComponentUtil.build(slot.isLocked() ? EnumColor.AQUA : EnumColor.WHITE, slot.amountAsLong());
    }
    @Override
    public void submit(BinRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        renderer.submit(state, poses, collector, camera);
    }
    @Override protected String getProfilerSection() { return "extraBin"; }
}

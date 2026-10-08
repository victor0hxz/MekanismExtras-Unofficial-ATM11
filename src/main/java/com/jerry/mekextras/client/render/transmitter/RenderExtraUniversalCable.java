package com.jerry.mekextras.client.render.transmitter;

import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraUniversalCable;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.transmitter.RenderUniversalCable;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.client.render.transmitter.TransmitterRenderState.CableRenderState;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.network.EnergyNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RenderExtraUniversalCable extends RenderTransmitterBase<TileEntityExtraUniversalCable, CableRenderState> {
    private final RenderUniversalCable renderer;

    public RenderExtraUniversalCable(BlockEntityRendererProvider.Context context) {
        super(context);
        renderer = new RenderUniversalCable(context);
    }

    @Override
    public CableRenderState createRenderState() {
        return renderer.createRenderState();
    }

    @Override
    public void extractRenderState(TileEntityExtraUniversalCable tile, CableRenderState state,
            float partialTick, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        EnergyNetwork network = tile.getTransmitter().getTransmitterNetwork();
        if (network == null) {
            return;
        }
        state.currentScale = network.currentScale;
    }

    @Override
    public void submit(CableRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        renderer.submit(state, poseStack, collector, camera);
    }

    @Override
    protected boolean shouldRenderTransmitter(TileEntityExtraUniversalCable tile, Vec3 camera) {
        if (!super.shouldRenderTransmitter(tile, camera) || !tile.getTransmitter().hasTransmitterNetwork()) {
            return false;
        }
        EnergyNetwork network = tile.getTransmitter().getTransmitterNetwork();
        return network != null && network.currentScale > 0;
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.UNIVERSAL_CABLE;
    }
}

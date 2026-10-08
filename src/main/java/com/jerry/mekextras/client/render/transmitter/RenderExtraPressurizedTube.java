package com.jerry.mekextras.client.render.transmitter;

import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraPressurizedTube;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.transmitter.RenderPressurizedTube;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.client.render.transmitter.TransmitterRenderState.TubeRenderState;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.network.ChemicalNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RenderExtraPressurizedTube extends RenderTransmitterBase<TileEntityExtraPressurizedTube, TubeRenderState> {
    private final RenderPressurizedTube renderer;

    public RenderExtraPressurizedTube(BlockEntityRendererProvider.Context context) {
        super(context);
        renderer = new RenderPressurizedTube(context);
    }

    @Override
    public TubeRenderState createRenderState() {
        return renderer.createRenderState();
    }

    @Override
    public void extractRenderState(TileEntityExtraPressurizedTube tile, TubeRenderState state,
            float partialTick, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        ChemicalNetwork network = tile.getTransmitter().getTransmitterNetwork();
        if (network == null) {
            return;
        }
        state.currentScale = Math.max(0.2F, network.currentScale);
        state.chemicalTexture = MekanismRenderer.getChemicalTexture(network.getLastType());
        state.chemicalTint = MekanismRenderer.getTint(network.getLastType().typeHolder());
    }

    @Override
    public void submit(TubeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        renderer.submit(state, poseStack, collector, camera);
    }

    @Override
    protected boolean shouldRenderTransmitter(TileEntityExtraPressurizedTube tile, Vec3 camera) {
        if (!super.shouldRenderTransmitter(tile, camera) || !tile.getTransmitter().hasTransmitterNetwork()) {
            return false;
        }
        ChemicalNetwork network = tile.getTransmitter().getTransmitterNetwork();
        return network != null && !network.getLastType().isEmpty() && !network.getContainer().isEmpty() && network.currentScale > 0;
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.PRESSURIZED_TUBE;
    }
}

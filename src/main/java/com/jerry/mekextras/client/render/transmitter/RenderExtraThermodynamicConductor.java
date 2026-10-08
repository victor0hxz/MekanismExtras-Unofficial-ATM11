package com.jerry.mekextras.client.render.transmitter;

import com.jerry.mekextras.common.content.network.transmitter.ExtraThermodynamicConductor;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraThermodynamicConductor;
import mekanism.client.render.transmitter.RenderThermodynamicConductor;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.client.render.transmitter.TransmitterRenderState.ConductorRenderState;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.util.HeatUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RenderExtraThermodynamicConductor extends RenderTransmitterBase<TileEntityExtraThermodynamicConductor, ConductorRenderState> {
    private final RenderThermodynamicConductor renderer;

    public RenderExtraThermodynamicConductor(BlockEntityRendererProvider.Context context) {
        super(context);
        renderer = new RenderThermodynamicConductor(context);
    }

    @Override
    public ConductorRenderState createRenderState() {
        return renderer.createRenderState();
    }

    @Override
    public void extractRenderState(TileEntityExtraThermodynamicConductor tile, ConductorRenderState state,
            float partialTick, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        ExtraThermodynamicConductor conductor = tile.getTransmitter();
        state.tempColor = HeatUtils.getColorFromTemp(conductor.getTemperature(), conductor.getBaseColor()).argb();
    }

    @Override
    public void submit(ConductorRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        renderer.submit(state, poseStack, collector, camera);
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.THERMODYNAMIC_CONDUCTOR;
    }
}

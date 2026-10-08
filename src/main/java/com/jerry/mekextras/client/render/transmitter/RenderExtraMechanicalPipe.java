package com.jerry.mekextras.client.render.transmitter;

import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.transmitter.RenderMechanicalPipe;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import mekanism.api.fluid.IFluidTank;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.ModelRenderer;
import mekanism.client.render.RenderResizableCuboid;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.client.render.transmitter.TransmitterRenderState;
import mekanism.common.content.network.FluidNetwork;
import mekanism.common.content.network.transmitter.MechanicalPipe;
import mekanism.common.lib.transmitter.ConnectionType;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraMechanicalPipe;
import mekanism.common.util.EnumUtils;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class RenderExtraMechanicalPipe
extends RenderTransmitterBase<TileEntityExtraMechanicalPipe, TransmitterRenderState.PipeRenderState> {
    private static final int STAGES = 100;
    private static final float HEIGHT = 0.45f;
    private static final float OFFSET = 0.02f;

    private final RenderMechanicalPipe renderer;
    public RenderExtraMechanicalPipe(BlockEntityRendererProvider.Context context) {
        super(context);
        renderer = new RenderMechanicalPipe(context);
    }

    public TransmitterRenderState.PipeRenderState createRenderState() {
        return new TransmitterRenderState.PipeRenderState();
    }

    public void extractRenderState(TileEntityExtraMechanicalPipe pipe, TransmitterRenderState.PipeRenderState state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        boolean renderBase;
        int stage;
        super.extractRenderState(pipe, state, partialTick, cameraPosition, breakProgress);
        state.fluidTexture = null;
        state.currentScale = 0;
        MechanicalPipe transmitter = (MechanicalPipe)pipe.getTransmitter();
        FluidNetwork network = (FluidNetwork)transmitter.getTransmitterNetwork();
        if (network == null) {
            return;
        }
        FluidResource fluidType = (FluidResource)network.getLastType();
        if (fluidType.isEmpty()) {
            return;
        }
        state.currentScale = network.currentScale;
        state.fluidTexture = MekanismRenderer.getSinglePicker(MekanismRenderer.getFluidTexture((TypedInstance<Fluid>)fluidType, MekanismRenderer.FluidTextureType.STILL));
        state.fluidTint = MekanismRenderer.getColorARGB(fluidType, state.currentScale);
        state.stage = stage = Math.max(3, ModelRenderer.getStage(fluidType, 100, (double)state.currentScale));
        state.glow = MekanismRenderer.calculateGlowLight(state.lightCoords, fluidType);
        ArrayList<String> connectionContents = new ArrayList<>();
        boolean[] renderSides = new boolean[6];
        boolean hasHorizontalSide = false;
        int verticalSides = 0;
        for (Direction side : EnumUtils.DIRECTIONS) {
            ConnectionType connectionType = transmitter.getConnectionType(side);
            if (connectionType == ConnectionType.PUSH || connectionType == ConnectionType.PULL) {
                connectionContents.add((side.getSerializedName() + connectionType.getSerializedName().toUpperCase(Locale.ROOT)));
            }
            boolean bl = renderSides[side.ordinal()] = connectionType != ConnectionType.NORMAL;
            if (connectionType == ConnectionType.NONE) continue;
            if (side.getAxis().isHorizontal()) {
                hasHorizontalSide = true;
                continue;
            }
            ++verticalSides;
        }
        state.connectionContents = connectionContents;
        state.renderBase = renderBase = hasHorizontalSide || verticalSides < 2;
        byte coreSideRender = 0;
        for (Direction side : EnumUtils.DIRECTIONS) {
            if (!renderSides[side.ordinal()] && (!renderBase || stage == 99 || !side.getAxis().isVertical())) continue;
            coreSideRender = (byte)(coreSideRender | RenderResizableCuboid.SideRender.of(side));
        }
        state.coreSideRender = coreSideRender;
        Arrays.fill(state.renderSideModel, false);
        for (Direction side : EnumUtils.DIRECTIONS) {
            ConnectionType connectionType = transmitter.getConnectionType(side);
            if (connectionType != ConnectionType.NORMAL) continue;
            state.renderSideModel[side.ordinal()] = true;
        }
    }

    @Override
    public void submit(TransmitterRenderState.PipeRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        renderer.submit(state, poses, collector, camera);
    }

    @Override
    protected String getProfilerSection() {
        return "mechanicalPipe";
    }

    @Override
    protected boolean shouldRenderTransmitter(TileEntityExtraMechanicalPipe tile, Vec3 camera) {
        MechanicalPipe pipe;
        if (super.shouldRenderTransmitter(tile, camera) && (pipe = (MechanicalPipe)tile.getTransmitter()).hasTransmitterNetwork()) {
            FluidNetwork network = (FluidNetwork)pipe.getTransmitterNetworkNN();
            return !((FluidResource)network.getLastType()).isEmpty() && !((IFluidTank)network.getContainer()).isEmpty() && network.currentScale > 0.0f;
        }
        return false;
    }
}

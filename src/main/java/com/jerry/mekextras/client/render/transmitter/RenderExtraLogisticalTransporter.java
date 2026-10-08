package com.jerry.mekextras.client.render.transmitter;

import com.jerry.mekextras.common.tier.transmitter.TPTier;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraLogisticalTransporterBase;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashSet;
import mekanism.api.text.EnumColor;
import mekanism.client.render.transmitter.RenderLogisticalTransporter;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.client.render.transmitter.TransmitterRenderState.TransporterRenderState;
import mekanism.client.render.transmitter.TransmitterRenderState.TransporterRenderState.TransporterStackRenderState;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import mekanism.common.util.TransporterUtils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class RenderExtraLogisticalTransporter extends RenderTransmitterBase<TileEntityExtraLogisticalTransporterBase, TransporterRenderState> {
    private final RenderLogisticalTransporter<TileEntityLogisticalTransporterBase, TransporterRenderState> renderer;
    private final ItemModelResolver itemModels;
    public RenderExtraLogisticalTransporter(BlockEntityRendererProvider.Context context) {
        super(context);
        renderer = new RenderLogisticalTransporter<>(context, TransporterRenderState::new);
        itemModels = context.itemModelResolver();
    }
    @Override public TransporterRenderState createRenderState() { return new TransporterRenderState(); }
    @Override
    public void extractRenderState(TileEntityExtraLogisticalTransporterBase tile, TransporterRenderState state,
                                   float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        state.stacks = new ArrayList<>();
        var transporter = tile.getTransmitter();
        var level = tile.getLevel();
        if (level == null) return;
        var seen = new HashSet<VisibleStack>();
        float partial = partialTick * TPTier.getSpeed(transporter.tier);
        for (var stack : transporter.getTransit()) {
            if (stack.isEmpty() || !seen.add(new VisibleStack(stack.getItemType(), stack.color, stack.progress))) continue;
            var itemState = new TransporterStackRenderState(TransporterUtils.getStackPosition(transporter, stack, partial), stack.color);
            itemModels.updateForTopItem(itemState.item(), stack.asItemStack(), ItemDisplayContext.GROUND, level, null, 0);
            state.stacks.add(itemState);
        }
    }
    @Override
    public void submit(TransporterRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        renderer.submit(state, poses, collector, camera);
    }
    @Override protected String getProfilerSection() { return "extraLogisticalTransporter"; }
    private record VisibleStack(ItemResource resource, @Nullable EnumColor color, int progress) {}
}

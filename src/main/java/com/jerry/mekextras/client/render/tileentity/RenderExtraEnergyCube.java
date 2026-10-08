package com.jerry.mekextras.client.render.tileentity;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import com.jerry.mekextras.common.tier.TierColor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mekanism.client.model.ModelEnergyCore;
import mekanism.client.render.tileentity.RenderEnergyCube.EnergyCubeRenderState;
import mekanism.common.Mekanism;
import com.jerry.mekextras.common.tile.TileEntityExtraEnergyCube;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class RenderExtraEnergyCube extends MekanismTileEntityRenderer<TileEntityExtraEnergyCube, EnergyCubeRenderState> {
   public static final ModelLayerLocation CORE_LAYER = new ModelLayerLocation(Mekanism.rl("energy_core"), "main");
   public static final Axis coreVec = Axis.of(new Vector3f(0.0F, MekanismUtils.ONE_OVER_ROOT_TWO, MekanismUtils.ONE_OVER_ROOT_TWO));
   private final ModelPart energyCore;

   public static LayerDefinition createCoreLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("core", CubeListBuilder.create().addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 64);
   }

   public RenderExtraEnergyCube(Context context) {
      super(context);
      this.energyCore = context.bakeLayer(CORE_LAYER);
   }

   public EnergyCubeRenderState createRenderState() {
      return new EnergyCubeRenderState();
   }

   public void extractRenderState(
      TileEntityExtraEnergyCube cube, EnergyCubeRenderState state, float partialTick, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress
   ) {
      super.extractRenderState(cube, state, partialTick, cameraPosition, breakProgress);
      state.coreTint = TierColor.getPackedColor(cube.getAdvanceTier(), cube.getEnergyScale());
      state.ticks = (float)cube.getGameTime() + partialTick;
   }

   public void submit(EnergyCubeRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
      float scaledTicks = 4.0F * state.ticks;
      poseStack.pushPose();
      poseStack.translate(0.5, 0.5, 0.5);
      poseStack.scale(0.4F, 0.4F, 0.4F);
      poseStack.translate(0.0, Math.sin(Math.toRadians(3.0F * state.ticks)) / 7.0, 0.0);
      poseStack.mulPose(Axis.YP.rotationDegrees(scaledTicks));
      poseStack.mulPose(coreVec.rotationDegrees(36.0F + scaledTicks));
      nodeCollector.submitModelPart(this.energyCore, poseStack, ModelEnergyCore.RENDER_TYPE, 15728880, OverlayTexture.NO_OVERLAY, null, state.coreTint, null);
      poseStack.popPose();
   }

   protected String getProfilerSection() {
      return "energyCube";
   }

   public boolean shouldRender(TileEntityExtraEnergyCube tile, Vec3 camera) {
      return tile.getEnergyScale() > 0.0F && super.shouldRender(tile, camera);
   }
}

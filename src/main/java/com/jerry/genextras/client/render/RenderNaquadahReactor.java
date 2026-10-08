package com.jerry.genextras.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mekanism.api.text.EnumColor;
import mekanism.client.model.ModelEnergyCore;
import mekanism.client.render.tileentity.MultiblockTileEntityRenderer;
import mekanism.client.render.tileentity.RenderEnergyCube;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderNaquadahReactor
   extends MultiblockTileEntityRenderer<NaquadahReactorMultiblockData, TileEntityNaquadahReactorController, RenderNaquadahReactor.NaquadahRenderState> {
   private static final double SCALE = 1.0E8;
   private final ModelPart energyCore;

   public RenderNaquadahReactor(Context context) {
      super(context);
      this.energyCore = context.bakeLayer(RenderEnergyCube.CORE_LAYER);
   }

   public RenderNaquadahReactor.NaquadahRenderState createRenderState() {
      return new RenderNaquadahReactor.NaquadahRenderState();
   }

   public void extractRenderState(
      TileEntityNaquadahReactorController controller,
      RenderNaquadahReactor.NaquadahRenderState state,
      float partialTick,
      Vec3 cameraPosition,
      net.minecraft.client.renderer.feature.ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      super.extractRenderState(controller, state, partialTick, cameraPosition, breakProgress);
      NaquadahReactorMultiblockData multiblock = (NaquadahReactorMultiblockData)controller.getMultiblock();
      state.scaledTemp = Math.round(multiblock.getLastPlasmaTemp() / 1.0E8);
      state.ticks = Minecraft.getInstance().levelRenderer.getTicks() + partialTick;
   }

   public void submit(RenderNaquadahReactor.NaquadahRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.translate(0.5, -3.5, 0.5);
      float scale = 1.0F + 0.7F * sinDegrees(3.14F * (float)state.scaledTemp + 135.0F);
      this.renderPart(state, poseStack, nodeCollector, EnumColor.DARK_GREEN, scale, -6, -7, 0, 36);
      scale = 1.0F + 0.8F * sinDegrees((float)(3L * state.scaledTemp));
      this.renderPart(state, poseStack, nodeCollector, EnumColor.YELLOW, scale, 4, 4, 0, 36);
      scale = 1.0F - 0.9F * sinDegrees((float)(4L * state.scaledTemp + 90L));
      this.renderPart(state, poseStack, nodeCollector, EnumColor.WHITE, scale, 5, -3, -35, 106);
      poseStack.popPose();
   }

   private static float sinDegrees(float degrees) {
      return Mth.sin(degrees % 360.0F * (float) (Math.PI / 180.0));
   }

   protected String getProfilerSection() {
      return "naquadahReactor";
   }

   private void renderPart(
      RenderNaquadahReactor.NaquadahRenderState state,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      EnumColor color,
      float scale,
      int mult1,
      int mult2,
      int shift1,
      int shift2
   ) {
      poseStack.pushPose();
      poseStack.scale(2 * scale, 2 * scale, 2 * scale);
      poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks * mult1 + shift1));
      poseStack.mulPose(RenderEnergyCube.coreVec.rotationDegrees(state.ticks * mult2 + shift2));
      nodeCollector.submitModelPart(
         this.energyCore, poseStack, ModelEnergyCore.RENDER_TYPE, 15728880, OverlayTexture.NO_OVERLAY, null, color.getPackedColor(), null
      );
      poseStack.popPose();
   }

   protected boolean shouldRender(TileEntityNaquadahReactorController tile, NaquadahReactorMultiblockData multiblock, Vec3 camera) {
      return multiblock.isBurning();
   }

   public static class NaquadahRenderState extends BlockEntityRenderState {
      public long scaledTemp;
      public float ticks;
   }
}

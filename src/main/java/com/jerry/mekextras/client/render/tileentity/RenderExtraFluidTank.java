package com.jerry.mekextras.client.render.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.ModelRenderer;
import mekanism.client.render.RenderResizableCuboid;
import mekanism.client.render.MekanismRenderer.FluidTextureType;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.client.render.tileentity.RenderFluidTank;
import mekanism.client.render.tileentity.RenderFluidTank.FluidTankRenderState;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import com.jerry.mekextras.common.tile.TileEntityExtraFluidTank;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class RenderExtraFluidTank extends MekanismTileEntityRenderer<TileEntityExtraFluidTank, FluidTankRenderState> {
   private static final int stages = 1400;
   public static final float CONTENTS_MIN_XZ = 0.135F;
   public static final float CONTENTS_MAX_XZ = 0.865F;
   public static final float CONTENTS_MIN_Y = 0.12375F;
   public static final float VALVE_MIN_XZ = 0.3225F;
   public static final float VALVE_MAX_XZ = 0.6775F;
   public static final float VALVE_MAX_Y = 0.87625F;

   public RenderExtraFluidTank(Context context) {
      super(context);
   }

   public FluidTankRenderState createRenderState() {
      return new FluidTankRenderState();
   }

   public void extractRenderState(
      TileEntityExtraFluidTank tank, FluidTankRenderState state, float partialTick, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress
   ) {
      super.extractRenderState(tank, state, partialTick, cameraPosition, breakProgress);
      FluidResource fluid = (FluidResource)tank.fluidTank.resource();
      state.fluidScale = fluid.isEmpty() ? 0.0F : tank.prevScale;
      state.fluidTint = MekanismRenderer.getColorARGB(fluid, state.fluidScale);
      state.fluidGlow = MekanismRenderer.calculateGlowLight(state.lightCoords, fluid);
      state.fluidScale = fluid.isEmpty() ? 0.0F : tank.prevScale;
      boolean gaseous = MekanismUtils.lighterThanAirGas(fluid);
      state.contentsMaxY = state.fluidScale > 0.0F ? contentsMaxY(state.fluidScale, gaseous) : 0.0F;
      state.fluidTexture = fluid.isEmpty() ? null : MekanismRenderer.getSinglePicker(MekanismRenderer.getFluidTexture(fluid, FluidTextureType.STILL));
      FluidResource valveFluid = tank.getValveFluid();
      if (!valveFluid.isEmpty() && !gaseous) {
         state.valveMinY = valveMinY(state.fluidScale);
         state.valveTint = MekanismRenderer.getColorARGB(valveFluid);
         state.valveGlow = MekanismRenderer.calculateGlowLight(state.lightCoords, valveFluid);
         state.valveFluidTexture = MekanismRenderer.getValveTexture(valveFluid);
      } else {
         state.valveFluidTexture = null;
      }
   }

   public void submit(FluidTankRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
      RenderType renderType = Sheets.translucentBlockSheet();
      if (state.fluidScale > 0.0F) {
         RenderResizableCuboid.renderCube(
            (byte)62,
            0.135F,
            0.12375F,
            0.135F,
            0.865F,
            state.contentsMaxY,
            0.865F,
            poseStack,
            renderType,
            nodeCollector,
            state.fluidTint,
            state.fluidGlow,
            OverlayTexture.NO_OVERLAY,
            FaceDisplay.FRONT,
            camera.pos,
            Vec3.atLowerCornerOf(state.blockPos),
            state.fluidTexture
         );
      }

      if (state.valveFluidTexture != null) {
         RenderResizableCuboid.renderCube(
            (byte)60,
            0.3225F,
            state.valveMinY,
            0.3225F,
            0.6775F,
            0.87625F,
            0.6775F,
            poseStack,
            renderType,
            nodeCollector,
            state.valveTint,
            state.valveGlow,
            OverlayTexture.NO_OVERLAY,
            FaceDisplay.FRONT,
            camera.pos,
            Vec3.atLowerCornerOf(state.blockPos),
            state.valveFluidTexture
         );
      }
   }

   protected String getProfilerSection() {
      return "fluidTank";
   }

   public static float valveMinY(float fluidScale) {
      int stageToUse = Math.min(1399, (int)(fluidScale * 1399.0F));
      float stageFraction = stageToUse / 1400.0F;
      return 0.12375F + 0.7525F * stageFraction;
   }

   public static float contentsMaxY(float fluidScale, boolean gaseous) {
      int stage = ModelRenderer.getStage(gaseous, 1400, fluidScale);
      return 0.12375F + 0.75225F * (stage / 1400.0F);
   }
}

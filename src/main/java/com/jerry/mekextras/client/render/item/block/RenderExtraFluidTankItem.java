package com.jerry.mekextras.client.render.item.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import mekanism.client.ModelUtil;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.RenderResizableCuboid;
import mekanism.client.render.MekanismRenderer.FluidTextureType;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.client.render.RenderResizableCuboid.TexturePicker;
import mekanism.client.render.tileentity.RenderFluidTank;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.util.ItemAccessUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class RenderExtraFluidTankItem implements SpecialModelRenderer<RenderExtraFluidTankItem.TankRenderState> {
   private final Lazy<Vector3fc[]> extents = Lazy.of(() -> ModelUtil.computeExtents(MekanismBlocks.CREATIVE_FLUID_TANK));

   public void submit(
      RenderExtraFluidTankItem.@Nullable TankRenderState argument,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      int overlayCoords,
      boolean hasFoil,
      int outlineColor
   ) {
      if (argument != null) {
         if (argument.contentsMaxY > 0.0F) {
            int lightToUse = MekanismRenderer.calculateGlowLight(lightCoords, argument.fluidLight);
            RenderResizableCuboid.renderCube(
               (byte)62,
               0.135F,
               0.12375F,
               0.135F,
               0.865F,
               argument.contentsMaxY,
               0.865F,
               poseStack,
               Sheets.translucentBlockSheet(),
               submitNodeCollector,
               argument.fluidColor,
               lightToUse,
               overlayCoords,
               FaceDisplay.FRONT,
               Minecraft.getInstance().gameRenderer.getMainCamera().position(),
               null,
               argument.fluidTexture
            );
         }

         argument.blockModelRenderState.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
      }
   }

   public void getExtents(Consumer<Vector3fc> output) {
      for (Vector3fc extent : (Vector3fc[])this.extents.get()) {
         output.accept(extent);
      }
   }

   public RenderExtraFluidTankItem.@Nullable TankRenderState extractArgument(ItemStack stack) {
      ResourceHandler<FluidResource> handler = (ResourceHandler<FluidResource>)Capabilities.FLUID.getCapability(ItemAccessUtils.sideEffectFreeAccess(stack));
      int fluidLight = 0;
      int fluidColor = 0;
      float contentsMaxY = 0.0F;
      TexturePicker fluidTexture = null;
      if (handler != null) {
         FluidResource fluid = (FluidResource)handler.getResource(0);
         if (!fluid.isEmpty()) {
            float fluidScale = (float)handler.getAmountAsLong(0) / (float)handler.getCapacityAsLong(0, fluid);
            contentsMaxY = fluidScale > 0.0F ? RenderFluidTank.contentsMaxY(fluidScale, MekanismUtils.lighterThanAirGas(fluid)) : 0.0F;
            fluidLight = fluid.getFluidType().getLightLevel();
            fluidColor = MekanismRenderer.getColorARGB(fluid, fluidScale);
            fluidTexture = MekanismRenderer.getSinglePicker(MekanismRenderer.getFluidTexture(fluid, FluidTextureType.STILL));
         }
      }

      BlockState blockState = ((BlockItem)stack.getItem()).getBlock().defaultBlockState();
      BlockModelRenderState blockModel = new BlockModelRenderState();
      mc().getBlockModelResolver().update(blockModel, blockState, ModelUtil.BLOCK_DISPLAY_NO_CONTEXT);
      return new RenderExtraFluidTankItem.TankRenderState(fluidLight, fluidColor, contentsMaxY, fluidTexture, blockModel);
   }

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   public record TankRenderState(
      int fluidLight, int fluidColor, float contentsMaxY, @Nullable TexturePicker fluidTexture, BlockModelRenderState blockModelRenderState
   ) {
   }

   public static class Unbaked implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<RenderExtraFluidTankItem.TankRenderState> {
      public static final RenderExtraFluidTankItem.Unbaked INSTANCE = new RenderExtraFluidTankItem.Unbaked();
      public static final MapCodec<RenderExtraFluidTankItem.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

      public @Nullable RenderExtraFluidTankItem bake(BakingContext context) {
         return new RenderExtraFluidTankItem();
      }

      public MapCodec<RenderExtraFluidTankItem.Unbaked> type() {
         return MAP_CODEC;
      }
   }
}

package com.jerry.mekextras.client.render.item.block;
import com.jerry.mekextras.common.tier.TierColor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import mekanism.api.RelativeSide;
import mekanism.client.ModelUtil;
import mekanism.client.model.ModelEnergyCore;
import mekanism.client.model.blockstate.EnergyCubeModel;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.tileentity.RenderEnergyCube;
import com.jerry.mekextras.common.block.BlockExtraEnergyCube;
import mekanism.common.component.component.AttachedSideConfig;
import com.jerry.mekextras.common.item.block.ItemBlockExtraEnergyCube;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import com.jerry.mekextras.common.tier.ECTier;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.IPersistentConfigInfo;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.StorageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class RenderExtraEnergyCubeItem implements SpecialModelRenderer<RenderExtraEnergyCubeItem.CubeState> {
   private final ModelEnergyCore core;
   private final Lazy<Vector3fc[]> extents = Lazy.of(() -> ModelUtil.computeExtents(MekanismBlocks.CREATIVE_ENERGY_CUBE));

   public RenderExtraEnergyCubeItem(EntityModelSet entityModels) {
      this.core = new ModelEnergyCore(entityModels);
   }

   public void submit(
      RenderExtraEnergyCubeItem.@Nullable CubeState state,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      int overlayCoords,
      boolean hasFoil,
      int outlineColor
   ) {
      if (state != null) {
         state.blockRenderState.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
         if (state.coreState != null) {
            float scaledTicks = 4.0F * state.ticks();
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.4F, 0.4F, 0.4F);
            poseStack.translate(0.0, Math.sin(Math.toRadians(3.0F * state.ticks())) / 7.0, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(scaledTicks));
            poseStack.mulPose(RenderEnergyCube.coreVec.rotationDegrees(36.0F + scaledTicks));
            this.core.collect(state.coreState, poseStack, submitNodeCollector, 15728880, overlayCoords, false);
            poseStack.popPose();
         }
      }
   }

   public void getExtents(Consumer<Vector3fc> output) {
      for (Vector3fc vector3fc : (Vector3fc[])this.extents.get()) {
         output.accept(vector3fc);
      }
   }

   public RenderExtraEnergyCubeItem.@Nullable CubeState extractArgument(ItemStack stack) {
      ItemBlockExtraEnergyCube itemBlock = (ItemBlockExtraEnergyCube)stack.getItem();
      ECTier tier = itemBlock.getAdvancedTier();
      TileEntityEnergyCube.CubeSideState[] sideStates = new TileEntityEnergyCube.CubeSideState[EnumUtils.SIDES.length];
      AttachedSideConfig fallback = ItemBlockExtraEnergyCube.SIDE_CONFIG;
      IPersistentConfigInfo sideConfig = AttachedSideConfig.getStoredConfigInfo(stack, fallback, TransmissionType.ENERGY);

      for (RelativeSide side : EnumUtils.SIDES) {
         DataType dataType = sideConfig.getDataType(side);
         TileEntityEnergyCube.CubeSideState state = TileEntityEnergyCube.CubeSideState.INACTIVE;
         if (dataType != DataType.NONE) {
            state = dataType.canOutput() ? TileEntityEnergyCube.CubeSideState.ACTIVE_LIT : TileEntityEnergyCube.CubeSideState.ACTIVE_UNLIT;
         }

         sideStates[side.ordinal()] = state;
      }

      BlockModelRenderState modelRenderState = new BlockModelRenderState();
      BlockState blockState = ((BlockExtraEnergyCube)itemBlock.getBlock()).defaultBlockState();
      if (models().getBlockStateModelSet().get(blockState) instanceof EnergyCubeModel energyCubeModel) {
         List<BlockStateModelPart> partList = modelRenderState.setupModel(ModelUtil.IDENTITY, (energyCubeModel.materialFlags() & 1) != 0);
         energyCubeModel.collectParts(partList, sideStates);
         modelRenderState.tintLayers().add(TierColor.getPackedColor(tier));
      } else {
         mc().getBlockModelResolver().update(modelRenderState, blockState, ModelUtil.BLOCK_DISPLAY_NO_CONTEXT);
      }

      float ticks = mc().levelRenderer.getTicks() + MekanismRenderer.getPartialTick();
      float energyRatio = (float)StorageUtils.getEnergyRatio(stack);
      return new RenderExtraEnergyCubeItem.CubeState(
         energyRatio > 0.0F ? TierColor.getPackedColor(tier, energyRatio) : null, ticks, stack.hasFoil(), modelRenderState
      );
   }

   private static ModelManager models() {
      return mc().getModelManager();
   }

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   public record CubeState(@Nullable Integer coreState, float ticks, boolean hasFoil, BlockModelRenderState blockRenderState) {
   }

   public static class Unbaked implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<RenderExtraEnergyCubeItem.CubeState> {
      public static final RenderExtraEnergyCubeItem.Unbaked INSTANCE = new RenderExtraEnergyCubeItem.Unbaked();
      public static final MapCodec<RenderExtraEnergyCubeItem.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

      public @Nullable SpecialModelRenderer<RenderExtraEnergyCubeItem.CubeState> bake(BakingContext context) {
         return new RenderExtraEnergyCubeItem(context.entityModelSet());
      }

      public MapCodec<? extends net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<RenderExtraEnergyCubeItem.CubeState>> type() {
         return MAP_CODEC;
      }
   }
}

package com.jerry.mekextras.mixin.client;

import com.jerry.mekextras.client.render.transmitter.RenderExtraMechanicalPipe;

import mekanism.client.render.MekanismRenderer;

import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MekanismRenderer.class, remap = false)
public class MixinMekanismRenderer {

    @Inject(method = "onStitch", at = @At("TAIL"))
    private static void onExtraStitch(TextureAtlasStitchedEvent event, CallbackInfo ci) {

        if (event.getAtlas().location().equals(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)) {
            RenderExtraMechanicalPipe.onStitch();
        }
    }
}

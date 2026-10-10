package com.jerry.mekextras.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.SerializationConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.heat.IHeatCapacitor.CapacitorState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CapacitorState.class, remap = false)
public class MixinHeatCapacitorState {
    @Shadow @Final @Mutable public static Codec<CapacitorState> CODEC;
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void correctCapacity(CallbackInfo callback) {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SerializerHelper.NON_NEGATIVE_DOUBLE.optionalFieldOf(SerializationConstants.HEAT_STORED, 0D).forGetter(CapacitorState::heat),
            SerializerHelper.ONE_OR_GREATER_DOUBLE.optionalFieldOf(SerializationConstants.HEAT_CAPACITY, 1D).forGetter(CapacitorState::heatCapacity)
        ).apply(instance, CapacitorState::new));
    }
}

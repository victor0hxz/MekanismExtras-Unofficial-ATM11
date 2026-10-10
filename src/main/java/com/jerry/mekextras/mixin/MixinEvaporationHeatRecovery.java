package com.jerry.mekextras.mixin;

import com.jerry.mekextras.common.util.HeatStateRepair;
import mekanism.common.content.evaporation.EvaporationMultiblockData;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EvaporationMultiblockData.class, remap = false)
public class MixinEvaporationHeatRecovery {
    @Inject(method = "tick", at = @At("HEAD"))
    private void recoverEvaporationBeforeProcessing(ServerLevel level, CallbackInfoReturnable<Boolean> callback) {
        HeatStateRepair.recover(((EvaporationMultiblockData) (Object) this).getHeatCapacitor(), "thermal evaporation plant");
    }
}

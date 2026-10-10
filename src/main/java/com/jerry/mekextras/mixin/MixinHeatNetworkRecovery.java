package com.jerry.mekextras.mixin;

import com.jerry.mekextras.common.util.HeatStateRepair;
import com.jerry.mekextras.common.content.network.transmitter.ExtraThermodynamicConductor;
import com.jerry.mekextras.common.tier.transmitter.TCTier;
import mekanism.common.content.network.HeatNetwork;
import mekanism.common.content.network.transmitter.ThermodynamicConductor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeatNetwork.class, remap = false)
public class MixinHeatNetworkRecovery {
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void recoverConductorsBeforeTransfer(CallbackInfo callback) {
        for (ThermodynamicConductor conductor : ((HeatNetwork) (Object) this).getTransmitters()) {
            var capacitor = conductor.getHeatCapacitor(null);
            double expectedCapacity = conductor instanceof ExtraThermodynamicConductor extra
                ? TCTier.getHeatCapacity(extra.getTier()) : conductor.getTier().getHeatCapacity();
            // Legacy saves encoded stored heat as capacity; restore the configured tier.
            if (capacitor.getHeatCapacity() == capacitor.getHeat() && capacitor.getHeatCapacity() != expectedCapacity) {
                capacitor.setHeatCapacity(expectedCapacity, null);
            }
            HeatStateRepair.recover(capacitor, "thermodynamic conductor");
        }
    }
}

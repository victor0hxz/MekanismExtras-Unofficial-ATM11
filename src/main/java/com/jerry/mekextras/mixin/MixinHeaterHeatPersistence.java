package com.jerry.mekextras.mixin;

import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityMekanism.class, remap = false)
public class MixinHeaterHeatPersistence {
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void repairSavedHeater(ValueInput input, CallbackInfo callback) {
        if ((Object) this instanceof TileEntityResistiveHeater heater) {
            // This machine's physical heat capacity is fixed; old saves used its heat instead.
            heater.getHeatCapacitor(null).setHeatCapacity(TileEntityResistiveHeater.HEAT_CAPACITY, null);
        }
    }
}

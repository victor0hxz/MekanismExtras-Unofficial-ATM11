package com.jerry.mekextras.common.util;

import com.jerry.mekextras.MekanismExtras;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;

/** Migration for impossible thermal states left by the old heat-capacity codec. */
public final class HeatStateRepair {
    // This is a corruption detector for heaters, evaporation plants and conductors,
    // not a normal operating limit or a reactor temperature cap.
    public static final double CORRUPT_TEMPERATURE = 1.0E15;

    private HeatStateRepair() { }

    public static boolean recover(IHeatCapacitor capacitor, String source) {
        if (capacitor == null) return false;
        double temperature = capacitor.getTemperature();
        if (Double.isFinite(temperature) && temperature < CORRUPT_TEMPERATURE) return false;
        double capacity = capacitor.getHeatCapacity();
        if (!Double.isFinite(capacity) || capacity < 1) return false;
        capacitor.setHeat(capacity * HeatAPI.AMBIENT_TEMP, null);
        MekanismExtras.LOGGER.warn("Recovered corrupt thermal state in {}: temperature {} K; restored ambient heat", source, temperature);
        return true;
    }
}

package com.jerry.mekextras.client.model;

import mekanism.client.model.ModelEnergyCore;
import mekanism.client.render.tileentity.RenderEnergyCube;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public class ColorModelEnergyCore extends ModelEnergyCore {
    public static final ModelLayerLocation CORE_LAYER = RenderEnergyCube.CORE_LAYER;

    public ColorModelEnergyCore(EntityModelSet models) {
        super(models);
    }

    public static LayerDefinition createLayerDefinition() {
        return RenderEnergyCube.createCoreLayer();
    }
}

package com.jerry.mekextras.client.model.energycube;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import mekanism.client.model.energycube.EnergyCubeBaseLoader;
import mekanism.client.model.energycube.EnergyCubeBaseUnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

public class ExtraEnergyCubeModelLoader implements UnbakedModelLoader<EnergyCubeBaseUnbakedModel> {
    public static final ExtraEnergyCubeModelLoader INSTANCE = new ExtraEnergyCubeModelLoader();

    @Override
    public EnergyCubeBaseUnbakedModel read(JsonObject object, JsonDeserializationContext context) {
        return EnergyCubeBaseLoader.INSTANCE.read(object, context);
    }
}

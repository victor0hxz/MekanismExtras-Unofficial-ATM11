package com.jerry.mekextras.client.recipe_viewer.jei;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.ExtraLang;
import com.jerry.mekextras.common.registries.ExtraFluids;

import com.jerry.genextras.common.GenExtraLang;
import com.jerry.genextras.common.registries.GenExtraFluids;

import org.jspecify.annotations.NullMarked;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;

import com.jerry.mekmm.client.recipe_viewer.MMRecipeViewerRecipeType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

@JeiPlugin
@NullMarked
public class ExtrasJEI implements IModPlugin {

    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        if (!MekanismJEI.shouldLoad()) return;
        MekanismJEI.registerItemSubtypes(registration, com.jerry.mekextras.common.registries.ExtraItems.EXTRA_ITEMS.getEntries());
        MekanismJEI.registerItemSubtypes(registration, com.jerry.mekextras.common.registries.ExtraBlocks.EXTRA_BLOCKS.getSecondaryEntries());
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            MekanismJEI.registerItemSubtypes(registration, com.jerry.genextras.common.registries.GenExtraItems.GEN_EXTRA_ITEMS.getEntries());
            MekanismJEI.registerItemSubtypes(registration, com.jerry.genextras.common.registries.GenExtraBlocks.GEN_EXTRA_BLOCKS.getSecondaryEntries());
        }
        if (MekanismExtras.hooks.mekmm.isLoaded()) {
            MekanismJEI.registerItemSubtypes(registration, com.jerry.mekextras.common.integration.mekmm.registries.ExtraMoreMachineBlocks.MM_BLOCKS.getSecondaryEntries());
            MekanismJEI.registerItemSubtypes(registration, com.jerry.mekextras.common.integration.mekaf.registries.ExtraAdvancedFactoryBlocks.AF_BLOCKS.getSecondaryEntries());
        }
    }

    @Override
    public Identifier getPluginUid() {
        // 不能使用MekanismExtras.rl()，原因见MekanismJEI.class
        return Identifier.fromNamespaceAndPath(MekanismExtras.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!MekanismJEI.shouldLoad()) {
            return;
        }
        registration.addIngredientInfo(ExtraFluids.RICH_NAQUADAH_FUEL.asStack(FluidType.BUCKET_VOLUME), NeoForgeTypes.FLUID_STACK,
                ExtraLang.RECIPE_VIEWER_INFO_RICH_NAQUADAH_FUEL.translate());
        registration.addIngredientInfo(ExtraFluids.RICH_URANIUM_FUEL.asStack(FluidType.BUCKET_VOLUME), NeoForgeTypes.FLUID_STACK,
                ExtraLang.RECIPE_VIEWER_INFO_RICH_URANIUM_FUEL.translate());
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            registration.addIngredientInfo(GenExtraFluids.POLONIUM_CONTAINING_SOLUTION.asStack(FluidType.BUCKET_VOLUME), NeoForgeTypes.FLUID_STACK,
                    GenExtraLang.RECIPE_VIEWER_INFO_POLONIUM_CONTAINING_SOLUTION.translate());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        if (!MekanismJEI.shouldLoad()) {
            return;
        }
        // 只是添加JEI的侧面栏的显示
        ExtraCatalystRegistryHelper.register(registry, RecipeViewerRecipeType.ENRICHING, RecipeViewerRecipeType.CRUSHING, RecipeViewerRecipeType.COMBINING,
                RecipeViewerRecipeType.PURIFYING, RecipeViewerRecipeType.COMPRESSING, RecipeViewerRecipeType.INJECTING, RecipeViewerRecipeType.SAWING,
                RecipeViewerRecipeType.METALLURGIC_INFUSING, RecipeViewerRecipeType.SMELTING, RecipeViewerRecipeType.CHEMICAL_CONVERSION);

        ExtraCatalystRegistryHelper.register(registry, RecipeTypes.SMELTING, RecipeViewerRecipeType.VANILLA_SMELTING.workstations());

        if (MekanismExtras.hooks.mekmm.isLoaded()) {
            ExtraMMCatalystRegistryHelper.register(registry, MMRecipeViewerRecipeType.RECYCLER, MMRecipeViewerRecipeType.PLANTING_STATION, MMRecipeViewerRecipeType.REPLICATOR,
                    MMRecipeViewerRecipeType.FLUID_REPLICATOR, MMRecipeViewerRecipeType.CHEMICAL_REPLICATOR, MMRecipeViewerRecipeType.STAMPING, MMRecipeViewerRecipeType.LATHE, MMRecipeViewerRecipeType.ROLLING_MILL,
                    MMRecipeViewerRecipeType.PRESSING);
            ExtraAFCatalystRegistryHelper.register(registry, RecipeViewerRecipeType.OXIDIZING, RecipeViewerRecipeType.DISSOLUTION, RecipeViewerRecipeType.WASHING, RecipeViewerRecipeType.CRYSTALLIZING,
                    RecipeViewerRecipeType.REACTION, RecipeViewerRecipeType.CENTRIFUGING, RecipeViewerRecipeType.NUTRITIONAL_LIQUIFICATION, RecipeViewerRecipeType.PIGMENT_EXTRACTING, RecipeViewerRecipeType.PAINTING);
        }
    }
}

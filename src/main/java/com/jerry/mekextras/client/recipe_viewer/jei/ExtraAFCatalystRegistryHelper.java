package com.jerry.mekextras.client.recipe_viewer.jei;

import com.jerry.mekextras.common.integration.mekaf.registries.ExtraAdvancedFactoryBlocks;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.util.ExtraEnumUtils;

import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.common.block.attribute.Attribute;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import com.jerry.mekaf.common.block.attribute.AttributeAdvancedFactoryType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import java.util.List;

public class ExtraAFCatalystRegistryHelper {

    private ExtraAFCatalystRegistryHelper() {}

    public static void register(IRecipeCatalystRegistration registry, IRecipeViewerRecipeType<?>... categories) {
        for (IRecipeViewerRecipeType<?> category : categories) {
            // Core Mekanism workstations do not carry MoreMachine's advanced-factory attribute.
            // Map each processing category explicitly so all addon tiers remain discoverable.
            var type = factoryType(category);
            if (type != null) {
                for (ExtraFactoryTier tier : ExtraEnumUtils.EXTRA_FACTORY_TIERS) {
                    registry.addRecipeCatalyst(ExtraAdvancedFactoryBlocks.getExtraAdvancedFactory(tier, type), MekanismJEI.genericRecipeType(category));
                }
            } else {
                register(registry, MekanismJEI.genericRecipeType(category), category.workstations());
            }
        }
    }

    private static com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType factoryType(IRecipeViewerRecipeType<?> category) {
        var types = com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType.class;
        String name;
        if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.OXIDIZING) name = "OXIDIZING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.DISSOLUTION) name = "DISSOLVING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.WASHING) name = "WASHING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.CRYSTALLIZING) name = "CRYSTALLIZING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.REACTION) name = "PRESSURISED_REACTING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.CENTRIFUGING) name = "CENTRIFUGING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.NUTRITIONAL_LIQUIFICATION) name = "LIQUIFYING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.PIGMENT_EXTRACTING) name = "PIGMENT_EXTRACTING";
        else if (category == mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.PAINTING) name = "PAINTING";
        else return null;
        return Enum.valueOf(types, name);
    }

    public static void register(IRecipeCatalystRegistration registry, IRecipeType<?> recipeType, List<ItemLike> workstations) {
        for (ItemLike workstation : workstations) {
            Item item = workstation.asItem();
            if (item instanceof BlockItem blockItem) {
                AttributeAdvancedFactoryType factoryType = Attribute.get(blockItem.getBlock(), AttributeAdvancedFactoryType.class);
                if (factoryType != null) {
                    for (ExtraFactoryTier tier : ExtraEnumUtils.EXTRA_FACTORY_TIERS) {
                        registry.addRecipeCatalyst(ExtraAdvancedFactoryBlocks.getExtraAdvancedFactory(tier, factoryType.getAdvancedFactoryType()), recipeType);
                    }
                }
            }
        }
    }
}

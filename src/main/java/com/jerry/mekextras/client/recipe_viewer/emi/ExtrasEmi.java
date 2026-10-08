package com.jerry.mekextras.client.recipe_viewer.emi;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.ExtraLang;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import com.jerry.mekextras.common.registries.ExtraFluids;
import com.jerry.mekextras.common.registries.ExtraItems;

import com.jerry.genextras.common.GenExtraLang;
import com.jerry.genextras.common.registries.GenExtraFluids;

import dev.emi.emi.api.stack.Comparison;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiStack;

import java.util.List;

@EmiEntrypoint
public class ExtrasEmi implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        addCategories(registry);

        ExtraItems.EXTRA_ITEMS.getEntries().forEach(item -> registry.setDefaultComparison(item.get(), Comparison.compareComponents()));
        ExtraBlocks.EXTRA_BLOCKS.getSecondaryEntries().forEach(item -> registry.setDefaultComparison(item.get(), Comparison.compareComponents()));
    }

    private void addCategories(EmiRegistry registry) {
        registry.addRecipe(infoRecipe(List.of(EmiStack.of(ExtraFluids.RICH_NAQUADAH_FUEL.value())), List.of(
                ExtraLang.RECIPE_VIEWER_INFO_RICH_NAQUADAH_FUEL.translate()), MekanismExtras.rl("info/rich_naquadah_fuel")));
        registry.addRecipe(infoRecipe(List.of(EmiStack.of(ExtraFluids.RICH_URANIUM_FUEL.value())), List.of(
                ExtraLang.RECIPE_VIEWER_INFO_RICH_URANIUM_FUEL.translate()), MekanismExtras.rl("info/rich_uranium_fuel")));
        if (MekanismExtras.hooks.mekanismGenerators.isLoaded()) {
            registry.addRecipe(infoRecipe(List.of(EmiStack.of(GenExtraFluids.POLONIUM_CONTAINING_SOLUTION.value())), List.of(
                    GenExtraLang.RECIPE_VIEWER_INFO_POLONIUM_CONTAINING_SOLUTION.translate()), MekanismExtras.rl("info/polonium_containing_solution")));
        }
    }
    // The optional EMI API still names Identifier ResourceLocation on its legacy compile artifact.
    // Resolve the constructor by runtime types so this integration can follow the viewer's port.
    private static dev.emi.emi.api.recipe.EmiRecipe infoRecipe(List<EmiStack> stacks,
          List<net.minecraft.network.chat.Component> text, net.minecraft.resources.Identifier id) {
        try {
            return (dev.emi.emi.api.recipe.EmiRecipe) EmiInfoRecipe.class
                .getConstructor(List.class, List.class, id.getClass()).newInstance(stacks, text, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("EMI must support this Minecraft version", exception);
        }
    }
}
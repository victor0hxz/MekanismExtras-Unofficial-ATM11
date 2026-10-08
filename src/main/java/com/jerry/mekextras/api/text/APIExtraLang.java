package com.jerry.mekextras.api.text;

import com.jerry.mekextras.MekanismExtras;

import org.jspecify.annotations.NullMarked;
import mekanism.api.text.ILangEntry;

import net.minecraft.util.Util;
import net.minecraft.resources.Identifier;

@NullMarked
public enum APIExtraLang implements ILangEntry {

    // Upgrades
    UPGRADE_STACK("upgrade", "stack"),
    UPGRADE_STACK_DESCRIPTION("upgrade", "stack.description"),
    UPGRADE_IONIC_MEMBRANE("upgrade", "ionic_membrane"),
    UPGRADE_IONIC_MEMBRANE_DESCRIPTION("upgrade", "ionic_membrane.description"),
    UPGRADE_CREATIVE("upgrade", "creative"),
    UPGRADE_CREATIVE_DESCRIPTION("upgrade", "creative.description");

    private final String key;

    APIExtraLang(String type, String path) {
        this(Util.makeDescriptionId(type, Identifier.fromNamespaceAndPath(MekanismExtras.MOD_ID, path)));
    }

    APIExtraLang(String key) {
        this.key = key;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}

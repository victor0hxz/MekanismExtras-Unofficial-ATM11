package com.jerry.mekextras.common.capabilities.chemical.variable;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public class ExtraVariableCapacityChemicalTank extends BasicChemicalTank {
    public ExtraVariableCapacityChemicalTank(long capacity, BiPredicate<ChemicalResource, AutomationType> canExtract,
            BiPredicate<ChemicalResource, AutomationType> canInsert, Predicate<ChemicalResource> validator,
            @Nullable ChemicalAttributeValidator attributeValidator, @Nullable IContentsListener listener) {
        super(capacity, canExtract, canInsert, validator, null, null, attributeValidator, listener);
    }
}
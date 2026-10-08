package com.jerry.mekextras.client.model;

import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.api.tier.IAdvancedTier;
import com.jerry.mekextras.common.tier.TierColor;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ExtraTierTintSource() implements ItemTintSource {
    public static final MapCodec<ExtraTierTintSource> MAP_CODEC = MapCodec.unit(new ExtraTierTintSource());
    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        if (stack.getItem() instanceof BlockItem item) {
            IAdvancedTier tier = ExtraAttribute.getAdvancedTier(item.getBlock(), IAdvancedTier.class);
            if (tier != null) return TierColor.getPackedColor(tier);
        }
        return -1;
    }
    @Override
    public MapCodec<ExtraTierTintSource> type() { return MAP_CODEC; }
}

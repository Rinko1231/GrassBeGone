package com.rinko1231.grassbegone.mixin;

import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WeightedStateProvider.class)
public interface WeightedStateProviderAccessor {
    @Accessor("weightedList")
    SimpleWeightedRandomList<BlockState> getWeightedList();
}
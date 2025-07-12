package com.rinko1231.grassbegone.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(NoiseThresholdProvider.class)
public interface NoiseThresholdProviderAccessor {

    @Accessor("threshold")
    float getThreshold();

    @Accessor("highChance")
    float getHighChance();

    @Accessor("defaultState")
    BlockState getDefaultState();

    @Accessor("lowStates")
    List<BlockState> getLowStates();

    @Accessor("highStates")
    List<BlockState> getHighStates();
}
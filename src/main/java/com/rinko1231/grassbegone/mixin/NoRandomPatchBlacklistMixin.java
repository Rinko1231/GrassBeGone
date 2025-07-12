package com.rinko1231.grassbegone.mixin;

import com.rinko1231.grassbegone.BlacklistCache;
import com.rinko1231.grassbegone.config.ConfigHandler;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.RandomPatchFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(RandomPatchFeature.class)
public abstract class NoRandomPatchBlacklistMixin {


    @Inject(
            method = "place",
            at = @At("HEAD"),
            cancellable = true
    )
    private void patchBlacklist(
            FeaturePlaceContext<RandomPatchConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RandomPatchConfiguration config = context.config();
        PlacedFeature placedFeature = config.feature().value();

        Optional<ConfiguredFeature<?, ?>> configured = placedFeature.feature().value().getFeatures().findAny();
        if (configured.isEmpty()) return;

        ConfiguredFeature<?, ?> feature = configured.get();

        if (!(feature.config() instanceof SimpleBlockConfiguration simpleConfig)) return;

        BlockStateProvider provider = simpleConfig.toPlace();
        Set<ResourceLocation> blacklist = BlacklistCache.getBlacklist();
        boolean replaceWithAir = BlacklistCache.getReplaceWithAir();

        // SimpleStateProvider
        if (provider instanceof SimpleStateProvider simpleState) {
            BlockState state = simpleState.getState(context.random(), context.origin());
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (id == null) return;

            if (blacklist.contains(id)) {
                if (replaceWithAir && Blocks.AIR.defaultBlockState().canSurvive(context.level(), context.origin())) {
                    cir.setReturnValue(true); //允许放置空气
                } else {
                    cir.setReturnValue(false); //阻止放置
                }
            }
        }

        //WeightedStateProvider
        else if (provider instanceof WeightedStateProvider weightedProvider) {
            SimpleWeightedRandomList<BlockState> list = ((WeightedStateProviderAccessor) weightedProvider).getWeightedList();
            List<WeightedEntry.Wrapper<BlockState>> entries = list.unwrap();

            boolean containsBlacklisted = false;
            SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();

            for (WeightedEntry.Wrapper<BlockState> entry : entries) {
                BlockState state = entry.getData();
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (id == null) {
                    builder.add(state, entry.getWeight().asInt());
                    continue;
                }

                if (blacklist.contains(id)) {
                    containsBlacklisted = true;
                    if (replaceWithAir) {
                        builder.add(Blocks.AIR.defaultBlockState(), entry.getWeight().asInt());
                    }
                } else {
                    builder.add(state, entry.getWeight().asInt());
                }
            }

            if (containsBlacklisted) {
                if (replaceWithAir) {
                    WeightedStateProvider newProvider = new WeightedStateProvider(builder.build());
                    SimpleBlockConfiguration newConfig = new SimpleBlockConfiguration(newProvider);
                    ConfiguredFeature<SimpleBlockConfiguration, ?> newConfigured =
                            new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, newConfig);

                    PlacedFeature newFeature = new PlacedFeature(Holder.direct(newConfigured), List.of());

                    boolean result = newFeature.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
                    cir.setReturnValue(result);
                } else {
                    cir.setReturnValue(false);
                }
            }
        }

        // NoiseThresholdProvider（如花）
        else if (provider instanceof NoiseThresholdProvider noiseProvider) {
            boolean containsBlacklisted = false;

            List<BlockState> high = new ArrayList<>();
            List<BlockState> low = new ArrayList<>();

            BlockState defaultState = ((NoiseThresholdProviderAccessor) noiseProvider).getDefaultState();
            ResourceLocation idDefault = ForgeRegistries.BLOCKS.getKey(defaultState.getBlock());
            if (idDefault == null) return;

            boolean defaultBlacklisted = blacklist.contains(idDefault);
            containsBlacklisted |= defaultBlacklisted;

            BlockState replacedDefault = defaultBlacklisted && replaceWithAir
                    ? Blocks.AIR.defaultBlockState()
                    : defaultState;

            for (BlockState state : ((NoiseThresholdProviderAccessor) noiseProvider).getHighStates()) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (id == null) {
                    high.add(state);
                    continue;
                }
                containsBlacklisted |= blacklist.contains(id);
                high.add(blacklist.contains(id) && replaceWithAir ? Blocks.AIR.defaultBlockState() : state);
            }

            for (BlockState state : ((NoiseThresholdProviderAccessor) noiseProvider).getLowStates()) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (id == null) {
                    low.add(state);
                    continue;
                }
                containsBlacklisted |= blacklist.contains(id);
                low.add(blacklist.contains(id) && replaceWithAir ? Blocks.AIR.defaultBlockState() : state);
            }

            if (containsBlacklisted) {
                if (replaceWithAir) {
                    NoiseThresholdProvider newProvider = new NoiseThresholdProvider(
                            ((NoiseBasedStateProviderAccessor) noiseProvider).getSeed(),
                            ((NoiseBasedStateProviderAccessor) noiseProvider).getParameters(),
                            ((NoiseBasedStateProviderAccessor) noiseProvider).getScale(),
                            ((NoiseThresholdProviderAccessor) noiseProvider).getThreshold(),
                            ((NoiseThresholdProviderAccessor) noiseProvider).getHighChance(),
                            replacedDefault,
                            low,
                            high
                    );

                    SimpleBlockConfiguration newConfig = new SimpleBlockConfiguration(newProvider);
                    ConfiguredFeature<SimpleBlockConfiguration, ?> newConfigured =
                            new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, newConfig);
                    PlacedFeature newFeature = new PlacedFeature(Holder.direct(newConfigured), List.of());

                    boolean result = newFeature.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
                    cir.setReturnValue(result);
                } else {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}



/*
@Mixin(RandomPatchFeature.class)
public abstract class NoRandomPatchBlacklistMixin {

    @Inject(
            method = "place",
            at = @At("HEAD"),
            cancellable = true
    )
    private void patchBlacklist(
            FeaturePlaceContext<RandomPatchConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RandomPatchConfiguration config = context.config();
        PlacedFeature placedFeature = config.feature().value();

        Optional<ConfiguredFeature<?, ?>> configured = placedFeature.feature().value().getFeatures().findAny();
        if (configured.isEmpty()) return;

        ConfiguredFeature<?, ?> feature = configured.get();

        if (!(feature.config() instanceof SimpleBlockConfiguration simpleConfig)) return;

        BlockStateProvider provider = simpleConfig.toPlace();
        Set<ResourceLocation> blacklist = ConfigHandler.patchBlockBlacklist.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());

        boolean replaceWithAir = ConfigHandler.onlyReplaceWithAir.get();

        // ✅ SimpleStateProvider
        if (provider instanceof SimpleStateProvider simpleState) {
            BlockState state = simpleState.getState(context.random(), context.origin());
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (blacklist.contains(id)) {
                cir.setReturnValue(replaceWithAir && Blocks.AIR.defaultBlockState().canSurvive(context.level(), context.origin()));
            }
        }

        // ✅ WeightedStateProvider
        else if (provider instanceof WeightedStateProvider weightedProvider) {
            SimpleWeightedRandomList<BlockState> list = ((WeightedStateProviderAccessor) weightedProvider).getWeightedList();
            List<WeightedEntry.Wrapper<BlockState>> entries = list.unwrap();

            boolean containsBlacklisted = false;
            SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();

            for (WeightedEntry.Wrapper<BlockState> entry : entries) {
                BlockState state = entry.getData();
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());

                if (blacklist.contains(id)) {
                    containsBlacklisted = true;
                    if (replaceWithAir) {
                        builder.add(Blocks.AIR.defaultBlockState(), entry.getWeight().asInt());
                    }
                } else {
                    builder.add(state, entry.getWeight().asInt());
                }
            }

            if (containsBlacklisted) {
                if (replaceWithAir) {
                    WeightedStateProvider newProvider = new WeightedStateProvider(builder.build());
                    SimpleBlockConfiguration newConfig = new SimpleBlockConfiguration(newProvider);
                    ConfiguredFeature<SimpleBlockConfiguration, ?> newConfigured =
                            new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, newConfig);

                    PlacedFeature newFeature = new PlacedFeature(Holder.direct(newConfigured), List.of());

                    boolean result = newFeature.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
                    cir.setReturnValue(result);
                } else {
                    cir.setReturnValue(false);
                }
            }
        }

        // ✅ NoiseThresholdProvider（处理如花的 plain）
        else if (provider instanceof NoiseThresholdProvider noiseProvider) {
            boolean containsBlacklisted = false;

            List<BlockState> high = new ArrayList<>();
            List<BlockState> low = new ArrayList<>();

            // 处理 defaultState
            BlockState defaultState = ((NoiseThresholdProviderAccessor) noiseProvider).getDefaultState();
            ResourceLocation idDefault = ForgeRegistries.BLOCKS.getKey(defaultState.getBlock());
            boolean defaultBlacklisted = blacklist.contains(idDefault);
            containsBlacklisted |= defaultBlacklisted;

            BlockState replacedDefault = defaultBlacklisted && replaceWithAir
                    ? Blocks.AIR.defaultBlockState()
                    : defaultState;

            // 处理 highStates
            for (BlockState state : ((NoiseThresholdProviderAccessor) noiseProvider).getHighStates()) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                containsBlacklisted |= blacklist.contains(id);
                high.add(blacklist.contains(id) && replaceWithAir ? Blocks.AIR.defaultBlockState() : state);
            }

            // 处理 lowStates
            for (BlockState state : ((NoiseThresholdProviderAccessor) noiseProvider).getLowStates()) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                containsBlacklisted |= blacklist.contains(id);
                low.add(blacklist.contains(id) && replaceWithAir ? Blocks.AIR.defaultBlockState() : state);
            }

            if (containsBlacklisted) {
                if (replaceWithAir) {
                    NoiseThresholdProvider newProvider = new NoiseThresholdProvider(
                            ((NoiseBasedStateProviderAccessor) noiseProvider).getSeed(),
                            ((NoiseBasedStateProviderAccessor) noiseProvider).getParameters(),
                            ((NoiseBasedStateProviderAccessor) noiseProvider).getScale(),
                            ((NoiseThresholdProviderAccessor) noiseProvider).getThreshold(),
                            ((NoiseThresholdProviderAccessor) noiseProvider).getHighChance(),
                            replacedDefault,
                            low,   // 替换后的 lowStates
                            high   // 替换后的 highStates
                    );

                    SimpleBlockConfiguration newConfig = new SimpleBlockConfiguration(newProvider);
                    ConfiguredFeature<SimpleBlockConfiguration, ?> newConfigured =
                            new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, newConfig);
                    PlacedFeature newFeature = new PlacedFeature(Holder.direct(newConfigured), List.of());

                    boolean result = newFeature.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
                    cir.setReturnValue(result);
                } else {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
*/

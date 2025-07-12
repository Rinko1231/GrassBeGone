package com.rinko1231.grassbegone.mixin;

import com.rinko1231.grassbegone.BlacklistCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {

    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState state, int flags, int recursionLimit, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return;

        Set<ResourceLocation> blacklist = BlacklistCache.getBlacklist();
        if (blacklist.contains(id)) {

            cir.setReturnValue(false);
        }
    }
}
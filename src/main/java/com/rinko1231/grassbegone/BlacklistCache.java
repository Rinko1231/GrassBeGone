package com.rinko1231.grassbegone;

import com.rinko1231.grassbegone.config.ConfigHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class BlacklistCache {
    private static volatile Set<ResourceLocation> cachedBlacklist = null;
    private static volatile Boolean cachedReplaceWithAir = null;

    public static synchronized void refreshCache() {
        cachedBlacklist = ConfigHandler.patchBlockBlacklist.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
        cachedReplaceWithAir = ConfigHandler.onlyReplaceWithAir.get();
    }

    public static Set<ResourceLocation> getBlacklist() {
        if (cachedBlacklist == null) refreshCache();
        return cachedBlacklist;
    }

    public static boolean getReplaceWithAir() {
        if (cachedReplaceWithAir == null) refreshCache();
        return cachedReplaceWithAir;
    }
}

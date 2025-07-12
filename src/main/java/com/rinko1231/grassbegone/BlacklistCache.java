package com.rinko1231.grassbegone;

import com.rinko1231.grassbegone.config.ConfigHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.stream.Collectors;

public class BlacklistCache {
    private static volatile Set<ResourceLocation> cachedBlacklist = null;

    public static synchronized void refreshCache() {
        cachedBlacklist = ConfigHandler.patchBlockBlacklist.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
    }

    public static Set<ResourceLocation> getBlacklist() {
        if (cachedBlacklist == null) refreshCache();
        return cachedBlacklist;
    }

}

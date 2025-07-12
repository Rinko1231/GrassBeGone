package com.rinko1231.grassbegone.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public class ConfigHandler {
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> patchBlockBlacklist;
    public static ForgeConfigSpec configSpec;
    public static ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec.BooleanValue onlyReplaceWithAir;


    static {
        BUILDER.push("Grass Be Gone Config");
        patchBlockBlacklist = BUILDER
                .comment("Grass and flower feature that will be removed.")
                .comment("You can still get grass and flower through bone meal.")
                .defineList("Grass/Flower Blacklist", List.of("minecraft:grass"),
                        element -> element instanceof String);
        onlyReplaceWithAir = BUILDER
                .comment("Just replace blacklisted blocks with air instead of removing the feature completely")
                .define("onlyReplaceWithAir", true);
        configSpec = BUILDER.build();
    }
    public static void setup()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, configSpec, "GrassBeGone.toml");
    }

}
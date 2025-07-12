package com.rinko1231.grassbegone.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public class ConfigHandler {
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> patchBlockBlacklist;
    public static ForgeConfigSpec configSpec;
    public static ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    static {
        BUILDER.push("Grass Be Gone Config");
        patchBlockBlacklist = BUILDER
                .comment("Blocks that will be removed during world feature generation.")
                .comment("You can still get grass and flower through bone meal.")
                .defineList("Block Blacklist", List.of("minecraft:grass"),
                        element -> element instanceof String);

        configSpec = BUILDER.build();
    }
    public static void setup()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, configSpec, "GrassBeGone.toml");
    }

}
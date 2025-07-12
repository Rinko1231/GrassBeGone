package com.rinko1231.grassbegone;

import com.rinko1231.grassbegone.config.ConfigHandler;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;


@Mod(GrassBeGone.MODID)
public class GrassBeGone
{
    public static final String MODID = "grassbegone";


    public GrassBeGone()
    {
        ConfigHandler.setup();

        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    public void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == ConfigHandler.configSpec) {
            BlacklistCache.refreshCache();
        }
    }
}

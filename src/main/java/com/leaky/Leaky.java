package com.leaky;

import com.leaky.config.CommonConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(com.leaky.Leaky.MODID)
public class Leaky
{
    public static final String                              MODID              = "leaky";
    public static final Logger                              LOGGER             = LogManager.getLogger();
    public static       Random                              rand               = new Random();

    public Leaky(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.addListener(this::commandRegister);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        if (CommonConfiguration.config != null)
        {
            LOGGER.info(MODID + " mod initialized");
        }
    }

    @SubscribeEvent
    public void commandRegister(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(new Command().build());
    }
}

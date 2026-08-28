package com.leaky;

import com.leaky.config.CommonConfiguration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
public class Leaky implements ModInitializer
{
    public static final String MODID  = "leaky";
    public static final Logger LOGGER = LogManager.getLogger();
    public static       Random rand   = new Random();

    public Leaky()
    {

    }

    @Override
    public void onInitialize()
    {
        if (CommonConfiguration.config != null)
        {
            LOGGER.info(MODID + " mod initialized");
        }

        CommandRegistrationCallback.EVENT.register((c, o, b) -> c.register(new Command().build()));
    }
}

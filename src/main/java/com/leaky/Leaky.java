package com.leaky;

import com.cupboard.config.CupboardConfig;
import com.leaky.config.CommonConfiguration;
import com.leaky.storage.ItemCluster;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(com.leaky.Leaky.MODID)
public class Leaky
{
    public static final String                              MODID  = "leaky";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static       Random                              rand   = new Random();

    public Leaky()
    {
        FORGE.bus().get().addListener(this::commandRegister);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
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

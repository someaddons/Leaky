package com.leaky;

import com.cupboard.config.CupboardConfig;
import com.leaky.config.CommonConfiguration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(com.leaky.Leaky.MODID)
public class Leaky
{
    public static final String                              MODID  = "leaky";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(MODID, new CommonConfiguration());
    public static       Random                              rand   = new Random();

    private static Map<BlockPos, Long> reportedLocations = new HashMap<>();

    public Leaky()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        config.load();
        LOGGER.info(MODID + " mod initialized");
    }

    public static void detectedItemLeak(final ItemEntity entity, final List<ItemEntity> items)
    {
        for (final Map.Entry<BlockPos, Long> entry : reportedLocations.entrySet())
        {
            if (entry.getKey().distSqr(entity.blockPosition()) < 10 * 10 && (entity.level().getGameTime() - entry.getValue()) < config.getCommonConfig().reportInterval * 20)
            {
                return;
            }
        }

        reportedLocations.put(entity.blockPosition(), entity.level().getGameTime());

        MutableComponent component = Component.literal("Detected farm leak: " + items.size() + " stacked items at:")
          .append(Component.literal("[" + entity.blockPosition().toShortString() + "]")
            .withStyle(ChatFormatting.YELLOW).withStyle(style ->
            {
                return style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                  "/tp " + entity.getBlockX() + " " + entity.getBlockY() + " " + entity.getBlockZ()));
            }))
          .append(Component.literal(" in " + entity.level().dimension().location().toString()));

        if (items.size() > config.getCommonConfig().autoremovethreshold)
        {
            component.append(Component.literal(". Removed leaking items automatically"));
            items.forEach(Entity::discard);
        }

        if (config.getCommonConfig().chatnotification.equalsIgnoreCase("PLAYER"))
        {
            double dist = Double.MAX_VALUE;
            Player closest = null;
            for (final Player player : entity.level().players())
            {
                if (player.position().distanceTo(entity.position()) < dist)
                {
                    dist = player.position().distanceTo(entity.position());
                    closest = player;
                }
            }

            if (closest != null)
            {
                closest.sendSystemMessage(component);
            }
        }
        else if (config.getCommonConfig().chatnotification.equalsIgnoreCase("EVERYONE"))
        {
            for (final Player player : entity.level().getServer().getPlayerList().getPlayers())
            {
                player.sendSystemMessage(component);
            }
        }
        else
        {
            component.append(Component.literal(" Chatnotification mode:NONE(" + config.getCommonConfig().chatnotification + ")"));
        }

        Leaky.LOGGER.warn(component.getString());
    }
}

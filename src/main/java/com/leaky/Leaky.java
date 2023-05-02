package com.leaky;

import com.leaky.config.Configuration;
import net.fabricmc.api.ModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
public class Leaky implements ModInitializer
{
    public static final String        MOD_ID = "leaky";
    public static final Logger        LOGGER = LogManager.getLogger();
    private static      Configuration config = null;
    public static       Random        rand   = new Random();

    private static Map<BlockPos, Long> reportedLocations = new HashMap<>();

    public Leaky()
    {

    }

    public static Configuration getConfig()
    {
        if (config == null)
        {
            config = new Configuration();
            config.load();
        }

        return config;
    }

    public static ResourceLocation id(String name)
    {
        return new ResourceLocation(MOD_ID, name);
    }

    @Override
    public void onInitialize()
    {
        LOGGER.info(MOD_ID + " mod initialized");
    }

    public static void detectedItemLeak(final ItemEntity entity, final List<ItemEntity> items)
    {
        for (final Map.Entry<BlockPos, Long> entry : reportedLocations.entrySet())
        {
            if (entry.getKey().distSqr(entity.blockPosition()) < 10 * 10 && (entity.level.getGameTime() - entry.getValue()) < config.getCommonConfig().reportInterval * 20)
            {
                return;
            }
        }

        reportedLocations.put(entity.blockPosition(), entity.level.getGameTime());

        MutableComponent component = Component.literal("Detected farm leak: " + items.size() + " stacked items at:")
          .append(Component.literal("[" + entity.blockPosition().toShortString() + "]")
            .withStyle(ChatFormatting.YELLOW).withStyle(style ->
            {
                return style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                  "/tp " + entity.getBlockX() + " " + entity.getBlockY() + " " + entity.getBlockZ()));
            }))
          .append(Component.literal(" in " + entity.level.dimension().location().toString()));

        if (items.size() > config.getCommonConfig().autoremovethreshold)
        {
            component.append(Component.literal(". Removed leaking items automatically"));
            items.forEach(Entity::discard);
        }

        if (config.getCommonConfig().chatnotification.equalsIgnoreCase("PLAYER"))
        {
            double dist = Double.MAX_VALUE;
            Player closest = null;
            for (final Player player : entity.level.players())
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
            for (final Player player : entity.level.getServer().getPlayerList().getPlayers())
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

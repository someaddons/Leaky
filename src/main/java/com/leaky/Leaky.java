package com.leaky;

import com.cupboard.config.CupboardConfig;
import com.leaky.config.CommonConfiguration;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
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
    public static final String                              MOD_ID = "leaky";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static final int CONTAIN_RADIUS_SQR = 4 * 4;
    public static final int REPORT_RADIUS_SQR = 10 * 10;
    public static      CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(MOD_ID, new CommonConfiguration());
    public static       Random                              rand   = new Random();

    private static Object2LongOpenHashMap<BlockPos> reportedLocations = new Object2LongOpenHashMap<>();

    public Leaky()
    {

    }

    @Override
    public void onInitialize()
    {
        LOGGER.info(MOD_ID + " mod initialized");
    }

    public static void detectedItemLeak(final ItemEntity entity, final List<ItemEntity> items, final int range)
    {
        int size = items.size();
        for (final ItemEntity item : items)
        {
            if (item instanceof INearbyItemAwareEntity nearbyItemAware)
            {
                nearbyItemAware.setNearbyItems(size);
            }
        }

        if (range > 2 && size < config.getCommonConfig().autoremovethreshold * 3)
        {
            size /= 2;
        }

        if (size < Leaky.config.getCommonConfig().reportThreshold)
        {
            return;
        }

        boolean contained = false;

        // Large leaks bypass the cooldown, to allow deletion before server crashes
        if (size < config.getCommonConfig().autoremovethreshold * 3)
        {
            long now = entity.level().getGameTime();
            for (final Map.Entry<BlockPos, Long> entry : reportedLocations.entrySet())
            {
                final double dist = entry.getKey().distSqr(entity.blockPosition());

                if (dist < CONTAIN_RADIUS_SQR)
                {
                    contained = true;
                }

                if (dist < REPORT_RADIUS_SQR && (now - entry.getValue()) < config.getCommonConfig().reportInterval * 20)
                {
                    return;
                }
            }
        }

        reportedLocations.put(entity.blockPosition(), entity.level().getGameTime());

        MutableComponent component = Component.translatable("leaky.detect", items.size(), Component.translatable(entity.level().dimension().location().toLanguageKey()))
            .append(Component.literal("[" + entity.blockPosition().toShortString() + "]")
                .withStyle(ChatFormatting.YELLOW).withStyle(style ->
                {
                    return style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/tp " + entity.getBlockX() + " " + entity.getBlockY() + " " + entity.getBlockZ()));
                }));

        if (size > config.getCommonConfig().autoremovethreshold && (contained || size >= config.getCommonConfig().autoremovethreshold * 3))
        {
            component.append(Component.translatable("leaky.removedItems"));
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
        else if (config.getCommonConfig().chatnotification.equalsIgnoreCase("OP"))
        {
            double dist = Double.MAX_VALUE;
            Player closest = null;
            for (final Player player : entity.level().players())
            {
                if (player.level().getServer().getPlayerList().isOp(player.getGameProfile()) && player.position().distanceTo(entity.position()) < dist)
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
        else
        {
            component.append(Component.literal(" Chatnotification mode:NONE(" + config.getCommonConfig().chatnotification + ")"));
        }

        Leaky.LOGGER.warn(component.getString());
    }
}

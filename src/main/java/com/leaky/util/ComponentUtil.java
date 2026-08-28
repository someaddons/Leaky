package com.leaky.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ComponentUtil
{
    /**
     * Clickable location component, for teleport
     *
     * @return Component with clickable teleport position
     */
    public static Component createLocationComponent(final BlockPos position, final ResourceKey<Level> dimension)
    {
        return Component.translatable("leaky.action.location", position.toShortString())
            .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in "+dimension.location()+" run tp " + position.getX() + " " + position.getY() + " " + position.getZ())));
    }

    /**
     * Clickable inspect component, for details
     *
     * @return
     */
    public static Component createInspectComponent(final int clusterID, final ResourceKey<Level> dimension)
    {
        return Component.translatable("leaky.action.inspect")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in "+dimension.location()+" run leaky inspect " + clusterID)));
    }

    /**
     * Create a clickable restore button
     *
     * @param clusterID
     * @param dimension
     * @return
     */
    public static Component createRestoreComponent(final int clusterID, final ResourceKey<Level> dimension)
    {
        return Component.translatable("leaky.action.restore")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/leaky restore " + clusterID + " "+dimension.location())));
    }

    /**
     * Formats seconds to a nicely readable hh/mm/ss string
     * @param seconds
     * @return
     */
    public static Component formatAge(final long seconds)
    {
        if (seconds < 60)
        {
            return Component.translatable("leaky.time.seconds", seconds);
        }

        if (seconds < 3600)
        {
            return Component.translatable("leaky.time.minutes_seconds", seconds / 60, seconds % 60);
        }

        return Component.translatable("leaky.time.hours_minutes", seconds / 3600, (seconds % 3600) / 60);
    }
}

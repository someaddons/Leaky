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
    public static Component createLocationComponent(final BlockPos position)
    {
        return Component.literal("[" + position.toShortString() + "]")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + position.getX() + " " + position.getY() + " " + position.getZ())));
    }

    /**
     * Clickable inspect component, for details
     *
     * @return
     */
    public static Component createInspectComponent(final int clusterID)
    {
        return Component.literal("[Inspect]")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/leaky inspect " + clusterID)));
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
        return Component.literal("[RESTORE]")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/leaky restore " + clusterID + " "+dimension.location())));
    }

    /**
     * Formats seconds to a nicely readable hh/mm/ss string
     * @param seconds
     * @return
     */
    public static String formatAge(final long seconds)
    {
        if (seconds < 60)
        {
            return seconds + "s";
        }

        if (seconds < 3600)
        {
            return seconds / 60 + "m " + seconds % 60 + "s";
        }

        return seconds / 3600 + "h " + (seconds % 3600) / 60 + "m";
    }
}

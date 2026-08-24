package com.leaky.storage;

import net.minecraft.ChatFormatting;

public enum ClusterState
{
    CREATED,
    OBSERVING,
    SUSPICIOUS,
    LEAK,
    PENDING_DELETION;

    ClusterState()
    {

    }

    public static ChatFormatting getStateColor(final ClusterState state)
    {
        return switch (state)
        {
            case CREATED -> ChatFormatting.DARK_GRAY;
            case OBSERVING -> ChatFormatting.GRAY;
            case SUSPICIOUS -> ChatFormatting.YELLOW;
            case LEAK -> ChatFormatting.GOLD;
            case PENDING_DELETION -> ChatFormatting.RED;
        };
    }
}

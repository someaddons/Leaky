package com.leaky.storage;

import net.minecraft.ChatFormatting;

public enum ClusterState
{
    CREATED("leaky.cluster.state.created"),
    OBSERVING("leaky.cluster.state.observing"),
    SUSPICIOUS("leaky.cluster.state.suspicious"),
    LEAK("leaky.cluster.state.leak"),
    PENDING_DELETION("leaky.cluster.state.pending_deletion");

    private final String translationKey;

    ClusterState(final String translationKey)
    {
        this.translationKey = translationKey;
    }

    public String getTranslationKey()
    {
        return translationKey;
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
